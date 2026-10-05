package com.me.mq.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecovererWithConfirms;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnExpression("${middleware.enabled:true} && ${middleware.rabbitmq.enabled:true}")
public class RabbitMQConfig {


    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_CREATE_ROUTING_KEY = "order.create";
    public static final String ORDER_CANCEL_ROUTING_KEY = "order.cancel";

    public static final String ORDER_DELAY_QUEUE = "order.create.delay.queue";
    public static final String ORDER_CANCEL_DLX_QUEUE = "order.cancel.dlx.queue";
    public static final String ORDER_DEAD_LETTER_EXCHANGE = "order.dlx.exchange";

    public static final String ORDER_STATUS_FANOUT_EXCHANGE = "order.status.fanout.exchange";
    public static final String NOTIFICATION_USER_QUEUE = "notification.user.queue";
    public static final String NOTIFICATION_VOLUNTEER_QUEUE = "notification.volunteer.queue";
    public static final String NOTIFICATION_ADMIN_QUEUE = "notification.admin.queue";

    public static final String APPROVAL_SUBMIT_FANOUT_EXCHANGE = "approval.submit.fanout.exchange";
    public static final String APPROVAL_ADMIN_QUEUE = "approval.admin.queue";

    public static final String APPROVAL_RESULT_DIRECT_EXCHANGE = "approval.result.direct.exchange";
    public static final String APPROVAL_RESULT_ROUTING_KEY_PREFIX = "approval.result.";
    public static final String APPROVAL_RESULT_ROUTING_KEY = "approval.result.volunteer";
    public static final String CONSUMER_FAILED_EXCHANGE = "takeme.consumer.failed.exchange";
    public static final String CONSUMER_FAILED_QUEUE = "takeme.consumer.failed.queue";
    public static final String CONSUMER_FAILED_ROUTING_KEY = "failed";
    public static final String BROADCAST_EXCHANGE = "notification.broadcast.exchange";
    public static final String BROADCAST_QUEUE = "notification.broadcast.queue";

    // 志愿者启动服务超时队列配置
    public static final String VOLUNTEER_START_TIMEOUT_EXCHANGE = "volunteer.start.timeout.exchange";
    public static final String VOLUNTEER_START_TIMEOUT_QUEUE = "volunteer.start.timeout.queue";
    public static final String VOLUNTEER_START_TIMEOUT_ROUTING_KEY = "volunteer.start.timeout";


    //序列化转换器配置
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    //自定义配置bean
    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connectionFactory.setPublisherReturns(true);
        template.setMandatory(true);

        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                org.slf4j.LoggerFactory.getLogger(RabbitMQConfig.class)
                        .debug("消息发送成功: {}", correlationData != null ? correlationData.getId() : "unknown");
            } else {
                org.slf4j.LoggerFactory.getLogger(RabbitMQConfig.class)
                        .error("消息发送失败: {}, 原因: {}",
                                correlationData != null ? correlationData.getId() : "unknown", cause);
            }
        });

        template.setReturnsCallback(returned -> {
            org.slf4j.LoggerFactory.getLogger(RabbitMQConfig.class)
                    .error("消息被退回: exchange={}, routingKey={}, eventId={}",
                            returned.getExchange(), returned.getRoutingKey(),
                            returned.getMessage().getMessageProperties().getMessageId());
        });

        return template;
    }

    // 消费最多执行三次；失败消息确认进入错误队列后，容器才自动 ACK 原消息。
    @Bean
    public SimpleRabbitListenerContainerFactory reliableRabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            CachingConnectionFactory connectionFactory, RabbitTemplate rabbitTemplate) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setMessageConverter(messageConverter());
        RepublishMessageRecovererWithConfirms recoverer = new RepublishMessageRecovererWithConfirms(
                rabbitTemplate, CONSUMER_FAILED_EXCHANGE, CONSUMER_FAILED_ROUTING_KEY,
                CachingConnectionFactory.ConfirmType.CORRELATED);
        recoverer.setConfirmTimeout(5000);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless().maxAttempts(3)
                .backOffOptions(1000, 2, 3000).recoverer(recoverer).build());
        // 错误队列不可用属于基础设施故障，保留原消息等待恢复。
        factory.setDefaultRequeueRejected(true);
        return factory;
    }

    @Bean
    public Declarables broadcastTopology() {
        DirectExchange exchange = new DirectExchange(BROADCAST_EXCHANGE, true, false);
        Queue queue = QueueBuilder.durable(BROADCAST_QUEUE).build();
        return new Declarables(exchange, queue, BindingBuilder.bind(queue).to(exchange).with("broadcast"));
    }

    @Bean
    public Declarables consumerFailedTopology() {
        DirectExchange exchange = new DirectExchange(CONSUMER_FAILED_EXCHANGE, true, false);
        Queue queue = QueueBuilder.durable(CONSUMER_FAILED_QUEUE).build();
        return new Declarables(exchange, queue,
                BindingBuilder.bind(queue).to(exchange).with(CONSUMER_FAILED_ROUTING_KEY));
    }

    @Bean
    public RabbitAdmin rabbitAdmin(RabbitTemplate rabbitTemplate) {
        return new RabbitAdmin(rabbitTemplate);
    }


    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    //订单死信交换机
    @Bean
    public DirectExchange orderDeadLetterExchange() {
        return new DirectExchange(ORDER_DEAD_LETTER_EXCHANGE, true, false);
    }

    //订单状态群发交换机
    @Bean
    public FanoutExchange orderStatusFanoutExchange() {
        return new FanoutExchange(ORDER_STATUS_FANOUT_EXCHANGE, true, false);
    }

    //业务审批状态群发交换机

    @Bean
    public FanoutExchange approvalSubmitFanoutExchange() {
        return new FanoutExchange(APPROVAL_SUBMIT_FANOUT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange approvalResultDirectExchange() {
        return new DirectExchange(APPROVAL_RESULT_DIRECT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange volunteerStartTimeoutExchange() {
        return new DirectExchange(VOLUNTEER_START_TIMEOUT_EXCHANGE, true, false);
    }


    //订单延时缓冲队列
    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", ORDER_DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", ORDER_CANCEL_ROUTING_KEY);
        args.put("x-message-ttl", 30 * 60 * 1000);

        return QueueBuilder.durable(ORDER_DELAY_QUEUE)
                .withArguments(args)
                .build();
    }

    //取消订单队列
    @Bean
    public Queue orderCancelDlxQueue() {
        return QueueBuilder.durable(ORDER_CANCEL_DLX_QUEUE).build();
    }

    //用户消息队列
    @Bean
    public Queue notificationUserQueue() {
        return QueueBuilder.durable(NOTIFICATION_USER_QUEUE).build();
    }

    //志愿者消息队列
    @Bean
    public Queue notificationVolunteerQueue() {
        return QueueBuilder.durable(NOTIFICATION_VOLUNTEER_QUEUE).build();
    }

    //管理员消息队列
    @Bean
    public Queue notificationAdminQueue() {
        return QueueBuilder.durable(NOTIFICATION_ADMIN_QUEUE).build();
    }


    //业务审批管理员队列
    @Bean
    public Queue approvalAdminQueue() {
        return QueueBuilder.durable(APPROVAL_ADMIN_QUEUE).build();
    }


    //业务审批志愿者队列
    @Bean
    public Queue volunteerApprovalQueue() {
        return QueueBuilder.durable("approval.result.volunteer.queue").build();
    }


    //志愿者活动超时延时缓冲队列
    @Bean
    public Queue volunteerStartTimeoutQueue() {
        Map<String, Object> args = new HashMap<>();
        // 设置消息TTL为30分钟（1800000毫秒）
        args.put("x-message-ttl", 30 * 60 * 1000);
        // 设置死信交换机
        args.put("x-dead-letter-exchange", VOLUNTEER_START_TIMEOUT_EXCHANGE);
        args.put("x-dead-letter-routing-key", VOLUNTEER_START_TIMEOUT_ROUTING_KEY);

        return QueueBuilder.durable("volunteer.start.timeout.delay.queue")
                .withArguments(args)
                .build();
    }

    //志愿者活动超时业务消费队列
    @Bean
    public Queue volunteerStartTimeoutDlxQueue() {
        return QueueBuilder.durable(VOLUNTEER_START_TIMEOUT_QUEUE).build();
    }

    @Bean
    public Binding orderDelayBinding(Queue orderDelayQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderDelayQueue)
                .to(orderExchange)
                .with(ORDER_CREATE_ROUTING_KEY);
    }

    //订单取消死信队列绑定：将死信交换机与取消订单队列绑定，处理超时未支付的订单
    @Bean
    public Binding orderCancelDlxBinding(Queue orderCancelDlxQueue, DirectExchange orderDeadLetterExchange) {
        return BindingBuilder.bind(orderCancelDlxQueue)
                .to(orderDeadLetterExchange)
                .with(ORDER_CANCEL_ROUTING_KEY);
    }

    //用户通知队列绑定：将订单状态广播交换机与用户通知队列绑定，接收订单状态变更消息
    @Bean
    public Binding notificationUserBinding(Queue notificationUserQueue, FanoutExchange orderStatusFanoutExchange) {
        return BindingBuilder.bind(notificationUserQueue).to(orderStatusFanoutExchange);
    }

    //志愿者通知队列绑定：将订单状态广播交换机与志愿者通知队列绑定，接收订单状态变更消息
    @Bean
    public Binding notificationVolunteerBinding(Queue notificationVolunteerQueue, FanoutExchange orderStatusFanoutExchange) {
        return BindingBuilder.bind(notificationVolunteerQueue).to(orderStatusFanoutExchange);
    }

    //管理员通知队列绑定：将订单状态广播交换机与管理员通知队列绑定，接收订单状态变更消息
    @Bean
    @ConditionalOnProperty(name = "middleware.rabbitmq.legacy-audit.enabled", havingValue = "true")
    public Binding notificationAdminBinding(Queue notificationAdminQueue, FanoutExchange orderStatusFanoutExchange) {
        return BindingBuilder.bind(notificationAdminQueue).to(orderStatusFanoutExchange);
    }

    //审批管理员队列绑定：将审批提交广播交换机与管理员审批队列绑定，接收新审批请求
    @Bean
    public Binding approvalAdminBinding(Queue approvalAdminQueue, FanoutExchange approvalSubmitFanoutExchange) {
        return BindingBuilder.bind(approvalAdminQueue).to(approvalSubmitFanoutExchange);
    }

    // 审批结果使用固定 Direct 路由，接收者由消息内容区分。
    @Bean
    public Binding volunteerApprovalBinding(Queue volunteerApprovalQueue, DirectExchange approvalResultDirectExchange) {
        return BindingBuilder.bind(volunteerApprovalQueue)
                .to(approvalResultDirectExchange)
                .with(APPROVAL_RESULT_ROUTING_KEY);
    }

    //志愿者启动超时延时队列绑定：将超时交换机与延时队列绑定，用于志愿者接单后启动服务的超时检测
    @Bean
    public Binding volunteerStartTimeoutDelayBinding(
            @Qualifier("volunteerStartTimeoutQueue") Queue volunteerStartTimeoutDelayQueue,
            DirectExchange volunteerStartTimeoutExchange) {
        return BindingBuilder.bind(volunteerStartTimeoutDelayQueue)
                .to(volunteerStartTimeoutExchange)
                .with("volunteer.start.timeout.delay");
    }

    //志愿者启动超时死信队列绑定：将超时交换机与业务处理队列绑定，处理超时未启动服务的订单
    @Bean
    public Binding volunteerStartTimeoutDlxBinding(
            @Qualifier("volunteerStartTimeoutDlxQueue") Queue volunteerStartTimeoutDlxQueue,
            DirectExchange volunteerStartTimeoutExchange) {
        return BindingBuilder.bind(volunteerStartTimeoutDlxQueue)
                .to(volunteerStartTimeoutExchange)
                .with(VOLUNTEER_START_TIMEOUT_ROUTING_KEY);
    }
}
