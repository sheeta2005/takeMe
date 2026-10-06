package com.me.integration;

import com.me.dto.ApprovalResultMessage;
import com.me.mq.config.RabbitMQConfig;
import com.me.mq.producer.MessageProducer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

// 所有交换机、队列和消费都限定在随机虚拟主机，避免触碰现有业务消息。
@EnabledIfSystemProperty(named = "takeme.rabbit.tests", matches = "true")
class RabbitReliabilityIntegrationTest {
    private static final String USER = System.getProperty("takeme.test.rabbit.user", "admin");
    private static final String PASSWORD = System.getProperty("takeme.test.rabbit.password", "");
    private static final String VHOST = "takeme_fix_test_" + UUID.randomUUID().toString().replace("-", "");
    private static CachingConnectionFactory connection;
    private static RabbitTemplate template;
    private static RabbitAdmin admin;
    private static MessageProducer producer;
    private static SimpleRabbitListenerContainerFactory factory;
    private static boolean created;

    @BeforeAll
    static void setup() throws Exception {
        management("PUT", "/vhosts/" + VHOST, "{}");
        created = true;
        management("PUT", "/permissions/" + VHOST + "/" + USER,
                "{\"configure\":\".*\",\"write\":\".*\",\"read\":\".*\"}");
        connection = new CachingConnectionFactory("localhost", 5672);
        connection.setUsername(USER);
        connection.setPassword(PASSWORD);
        connection.setVirtualHost(VHOST);
        RabbitMQConfig config = new RabbitMQConfig();
        template = config.rabbitTemplate(connection);
        admin = config.rabbitAdmin(template);
        for (Declarable declarable : config.consumerFailedTopology().getDeclarables()) {
            if (declarable instanceof Exchange exchange) admin.declareExchange(exchange);
            if (declarable instanceof Queue queue) admin.declareQueue(queue);
            if (declarable instanceof Binding binding) admin.declareBinding(binding);
        }
        DirectExchange approvalExchange = config.approvalResultDirectExchange();
        Queue approvalQueue = config.volunteerApprovalQueue();
        admin.declareExchange(approvalExchange);
        admin.declareQueue(approvalQueue);
        admin.declareBinding(config.volunteerApprovalBinding(approvalQueue, approvalExchange));
        producer = new MessageProducer(template);
        ReflectionTestUtils.setField(producer, "rabbitmqEnabled", true);
        RabbitProperties properties = new RabbitProperties();
        properties.getListener().getSimple().setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory = config.reliableRabbitListenerContainerFactory(
                new SimpleRabbitListenerContainerFactoryConfigurer(properties), connection, template);
    }

    @AfterAll
    static void cleanup() throws Exception {
        if (connection != null) connection.destroy();
        assertTrue(VHOST.startsWith("takeme_fix_test_"));
        if (created) management("DELETE", "/vhosts/" + VHOST, null);
    }

    @BeforeEach
    void clearTestFailureQueue() {
        admin.purgeQueue(RabbitMQConfig.CONSUMER_FAILED_QUEUE);
    }

    @Test
    void approvalResultActuallyReachesFixedDirectBinding() {
        ApprovalResultMessage payload = new ApprovalResultMessage();
        payload.setApprovalId(1L);
        payload.setApplicantId(1L);
        producer.sendConfirmed(RabbitMQConfig.APPROVAL_RESULT_DIRECT_EXCHANGE,
                RabbitMQConfig.APPROVAL_RESULT_ROUTING_KEY, payload, "approval-event");
        Message received = template.receive("approval.result.volunteer.queue", 5000);
        assertNotNull(received);
        assertEquals("approval-event", received.getMessageProperties().getMessageId());
        assertEquals(MessageDeliveryMode.PERSISTENT, received.getMessageProperties().getReceivedDeliveryMode());
        ApprovalResultMessage decoded = (ApprovalResultMessage) template.getMessageConverter().fromMessage(received);
        assertEquals(1L, decoded.getApprovalId());
        assertEquals(1L, decoded.getApplicantId());
    }

    @Test
    void brokerAckDoesNotHideAnUnroutableMessage() {
        assertThrows(AmqpException.class, () -> producer.sendConfirmed(
                RabbitMQConfig.APPROVAL_RESULT_DIRECT_EXCHANGE, "unbound.route", "测试", "returned-event"));
    }

    @Test
    void poisonMessageRetriesThreeTimesThenMovesToFailureQueue() {
        String queue = privateQueue();
        AtomicInteger attempts = new AtomicInteger();
        SimpleMessageListenerContainer container = listener(queue, message -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("模拟不可恢复业务异常");
        });
        try {
            container.start();
            producer.sendConfirmed("", queue, "失败消息", "poison-event");
            Message failed = template.receive(RabbitMQConfig.CONSUMER_FAILED_QUEUE, 15000);
            assertNotNull(failed);
            assertEquals(3, attempts.get());
            assertEquals("poison-event", failed.getMessageProperties().getMessageId());
            assertTrue(failed.getMessageProperties().getHeaders().containsKey("x-exception-stacktrace"));
        } finally {
            container.stop();
        }
        assertNull(template.receive(queue, 500));
    }

    @Test
    void transientFailureSucceedsOnThirdAttemptWithoutManualAck() throws Exception {
        String queue = privateQueue();
        AtomicInteger attempts = new AtomicInteger();
        CountDownLatch handled = new CountDownLatch(1);
        SimpleMessageListenerContainer container = listener(queue, message -> {
            if (attempts.incrementAndGet() < 3) throw new IllegalStateException("模拟瞬时故障");
            handled.countDown();
        });
        try {
            container.start();
            producer.sendConfirmed("", queue, "重试成功", "retry-event");
            assertTrue(handled.await(15, TimeUnit.SECONDS));
        } finally {
            container.stop();
        }
        assertEquals(3, attempts.get());
        assertNull(template.receive(queue, 500));
        assertNull(template.receive(RabbitMQConfig.CONSUMER_FAILED_QUEUE, 500));
    }

    @Test
    void failedRecoveryKeepsOriginalMessageUntilFailureQueueReturns() throws Exception {
        String queue = privateQueue();
        AtomicInteger attempts = new AtomicInteger();
        CountDownLatch retrying = new CountDownLatch(1);
        admin.deleteQueue(RabbitMQConfig.CONSUMER_FAILED_QUEUE);
        SimpleMessageListenerContainer container = listener(queue, message -> {
            if (attempts.incrementAndGet() > 3) retrying.countDown();
            throw new IllegalStateException("模拟业务失败且错误队列不可用");
        });
        try {
            container.start();
            producer.sendConfirmed("", queue, "不能丢失", "recovery-event");
            assertTrue(retrying.await(15, TimeUnit.SECONDS), "恢复投递失败后必须保留原消息");
            Queue failedQueue = QueueBuilder.durable(RabbitMQConfig.CONSUMER_FAILED_QUEUE).build();
            admin.declareQueue(failedQueue);
            admin.declareBinding(BindingBuilder.bind(failedQueue)
                    .to(new DirectExchange(RabbitMQConfig.CONSUMER_FAILED_EXCHANGE))
                    .with(RabbitMQConfig.CONSUMER_FAILED_ROUTING_KEY));
            assertNotNull(template.receive(RabbitMQConfig.CONSUMER_FAILED_QUEUE, 15000));
        } finally {
            container.stop();
        }
        assertNull(template.receive(queue, 500));
    }

    private static String privateQueue() {
        String queue = "test." + UUID.randomUUID();
        admin.declareQueue(QueueBuilder.durable(queue).build());
        return queue;
    }

    private static SimpleMessageListenerContainer listener(String queue, MessageListener listener) {
        SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
        endpoint.setId(queue);
        endpoint.setQueueNames(queue);
        endpoint.setMessageListener(listener);
        return factory.createListenerContainer(endpoint);
    }

    private static void management(String method, String path, String body) throws Exception {
        String basic = Base64.getEncoder().encodeToString((USER + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
        HttpURLConnection request = (HttpURLConnection) URI.create(
                "http://localhost:15672/api" + path).toURL().openConnection();
        try {
            request.setRequestMethod(method);
            request.setConnectTimeout(5000);
            request.setReadTimeout(5000);
            request.setRequestProperty("Authorization", "Basic " + basic);
            request.setRequestProperty("Content-Type", "application/json");
            if (body != null) {
                request.setDoOutput(true);
                try (var output = request.getOutputStream()) {
                    output.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            int code = request.getResponseCode();
            assertTrue(code >= 200 && code < 300, "RabbitMQ 测试虚拟主机操作失败: " + code);
        } finally {
            request.disconnect();
        }
    }
}
