package com.me.integration;

import com.me.dto.ApprovalResultMessage;
import com.me.mq.config.RabbitMQConfig;
import com.me.mq.producer.MessageProducer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
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
    private static final String PASSWORD = System.getProperty("takeme.test.rabbit.password",
            System.getenv().getOrDefault("TAKEME_TEST_RABBIT_PASSWORD", ""));
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
    void fullQueueNacksTheNewPublishAndKeepsExistingMessages() {
        String queue = "test." + UUID.randomUUID();
        admin.declareQueue(QueueBuilder.durable(queue).withArgument("x-max-length", 2)
                .withArgument("x-overflow", "reject-publish").build());
        producer.sendConfirmed("", queue, "第一条", "容量事件1");
        producer.sendConfirmed("", queue, "第二条", "容量事件2");

        assertThrows(AmqpException.class, () -> producer.sendConfirmed("", queue, "第三条", "容量事件3"));

        assertEquals("容量事件1", template.receive(queue, 5000).getMessageProperties().getMessageId());
        assertEquals("容量事件2", template.receive(queue, 5000).getMessageProperties().getMessageId());
        assertNull(template.receive(queue, 100));
        producer.sendConfirmed("", queue, "第三条", "容量事件3");
        assertEquals("容量事件3", template.receive(queue, 5000).getMessageProperties().getMessageId());
    }

    @Test
    void byteLimitRejectsNewBodyWithoutEvictingTheExistingBody() {
        String queue = "test." + UUID.randomUUID();
        admin.declareQueue(QueueBuilder.durable(queue).withArgument("x-max-length-bytes", 32)
                .withArgument("x-overflow", "reject-publish").build());
        MessageProperties properties = new MessageProperties();
        properties.setMessageId("字节容量事件");
        Message original = new Message(new byte[32], properties);
        producer.replayConfirmed(queue, original);

        assertThrows(AmqpException.class, () -> producer.replayConfirmed(queue, original));

        assertArrayEquals(original.getBody(), template.receive(queue, 5000).getBody());
        assertNull(template.receive(queue, 100));
    }

    @Test
    void partialFanoutRejectionRetainsTheEventIdAcrossRetry() {
        FanoutExchange exchange = new FanoutExchange("test." + UUID.randomUUID());
        Queue constrained = QueueBuilder.durable("test." + UUID.randomUUID()).withArgument("x-max-length", 1)
                .withArgument("x-overflow", "reject-publish").build();
        Queue available = QueueBuilder.durable(privateQueue()).build();
        admin.declareExchange(exchange);
        admin.declareQueue(constrained);
        admin.declareBinding(BindingBuilder.bind(constrained).to(exchange));
        admin.declareBinding(BindingBuilder.bind(available).to(exchange));
        producer.sendConfirmed("", constrained.getName(), "占用容量", "旧事件");

        assertThrows(AmqpException.class, () -> producer.sendConfirmed(exchange.getName(), "", "广播", "广播事件"));
        assertEquals("旧事件", template.receive(constrained.getName(), 5000).getMessageProperties().getMessageId());
        producer.sendConfirmed(exchange.getName(), "", "广播", "广播事件");

        assertEquals("广播事件", template.receive(constrained.getName(), 5000).getMessageProperties().getMessageId());
        assertEquals("广播事件", template.receive(available.getName(), 5000).getMessageProperties().getMessageId());
        assertEquals("广播事件", template.receive(available.getName(), 5000).getMessageProperties().getMessageId());
    }

    @Test
    void fullFailureQueueKeepsTheOriginalUntilArchivingCapacityReturns() throws Exception {
        management("PUT", "/policies/" + VHOST + "/test-failed-capacity",
                "{\"pattern\":\"^takeme[.]consumer[.]failed[.]queue$\",\"definition\":{\"max-length\":1," +
                        "\"overflow\":\"reject-publish\"},\"priority\":100,\"apply-to\":\"queues\"}");
        String queue = privateQueue();
        producer.sendConfirmed("", RabbitMQConfig.CONSUMER_FAILED_QUEUE, "已有归档消息", "占用失败队列");
        CountDownLatch retrying = new CountDownLatch(1);
        AtomicInteger attempts = new AtomicInteger();
        SimpleMessageListenerContainer container = listener(queue, message -> {
            if (attempts.incrementAndGet() > 3) retrying.countDown();
            throw new IllegalStateException("模拟失败队列已满");
        });
        try {
            container.start();
            producer.sendConfirmed("", queue, "必须保留原消息", "失败入口满载事件");
            assertTrue(retrying.await(15, TimeUnit.SECONDS));
            assertEquals("占用失败队列", template.receive(RabbitMQConfig.CONSUMER_FAILED_QUEUE, 5000)
                    .getMessageProperties().getMessageId());
            assertEquals("失败入口满载事件", template.receive(RabbitMQConfig.CONSUMER_FAILED_QUEUE, 15000)
                    .getMessageProperties().getMessageId());
        } finally {
            container.stop();
            management("DELETE", "/policies/" + VHOST + "/test-failed-capacity", null);
        }
        assertNull(template.receive(queue, 100));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void deploymentScriptChecksCapacityBeforeWritingAndDetectsPolicyDrift() throws Exception {
        RabbitMQConfig config = new RabbitMQConfig();
        for (Queue queue : new Queue[]{config.orderDelayQueue(), config.orderCancelDlxQueue(),
                config.notificationUserQueue(), config.notificationVolunteerQueue(), config.notificationAdminQueue(),
                config.approvalAdminQueue(), config.volunteerStartTimeoutQueue(), config.volunteerStartTimeoutDlxQueue()}) {
            admin.declareQueue(queue);
        }
        // 群发队列未声明时必须整体停止，不得只给一部分队列写策略。
        capacityScript(false, false);
        assertEquals("[]", management("GET", "/policies/" + VHOST, null));
        admin.declareQueue(QueueBuilder.durable(RabbitMQConfig.BROADCAST_QUEUE).build());
        for (int i = 0; i < 101; i++) producer.sendConfirmed("", RabbitMQConfig.BROADCAST_QUEUE, "积压", "积压" + i);
        awaitBroadcastStatistics(101);
        capacityScript(false, false);
        assertEquals("[]", management("GET", "/policies/" + VHOST, null));
        admin.purgeQueue(RabbitMQConfig.BROADCAST_QUEUE);
        awaitBroadcastStatistics(0);
        capacityScript(false, true);
        capacityScript(false, true);
        capacityScript(true, true);
        management("PUT", "/policies/" + VHOST + "/test-override",
                "{\"pattern\":\"^notification[.]user[.]queue$\",\"definition\":{\"max-length\":1," +
                        "\"overflow\":\"reject-publish\"},\"priority\":100,\"apply-to\":\"queues\"}");
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            var json = new com.fasterxml.jackson.databind.ObjectMapper();
            while (!"test-override".equals(json.readTree(management("GET", "/queues/" + VHOST + "/notification.user.queue", null))
                    .path("policy").asText())) {
                assertTrue(System.nanoTime() < deadline, "队列策略统计未刷新");
                Thread.sleep(200);
            }
            capacityScript(true, false);
        } finally {
            management("DELETE", "/policies/" + VHOST + "/test-override", null);
        }
    }

    // 管理统计异步刷新；等到实际数量可见再验证预检，避免把滞后统计当成零。
    private static void awaitBroadcastStatistics(int count) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        do {
            var queue = json.readTree(management("GET", "/queues/" + VHOST + "/" + RabbitMQConfig.BROADCAST_QUEUE, null));
            if (queue.hasNonNull("message_bytes_ready") && queue.path("messages_ready").asInt(-1) == count) return;
            Thread.sleep(200);
        } while (System.nanoTime() < deadline);
        fail("队列统计未在规定时间内更新");
    }

    private static void capacityScript(boolean verifyOnly, boolean expectSuccess) throws Exception {
        ProcessBuilder builder = new ProcessBuilder("pwsh", "-NoProfile", "-NonInteractive", "-Command",
                "& '../../tools/rabbitmq/apply-queue-policies.ps1' -Username $env:MQ_POLICY_TEST_USER " +
                        "-Password $env:MQ_POLICY_TEST_PASSWORD -VirtualHost $env:MQ_POLICY_TEST_VHOST" +
                        (verifyOnly ? " -VerifyOnly" : ""));
        builder.environment().put("MQ_POLICY_TEST_USER", USER);
        builder.environment().put("MQ_POLICY_TEST_PASSWORD", PASSWORD);
        builder.environment().put("MQ_POLICY_TEST_VHOST", VHOST);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "容量策略脚本执行超时");
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(expectSuccess, process.exitValue() == 0, output);
        } finally {
            process.destroyForcibly();
        }
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
            assertEquals(queue, failed.getMessageProperties().getHeaders()
                    .get(RabbitMQConfig.FAILED_SOURCE_QUEUE_HEADER));
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

    private static String management(String method, String path, String body) throws Exception {
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
            return new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            request.disconnect();
        }
    }
}
