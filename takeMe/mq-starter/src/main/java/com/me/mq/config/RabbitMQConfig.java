package com.me.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "middleware.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQConfig {

    @Value("${mq.order.timeout-minutes:30}")
    private Integer orderTimeoutMinutes;

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

    // 志愿者启动服务超时队列配置
    public static final String VOLUNTEER_START_TIMEOUT_EXCHANGE = "volunteer.start.timeout.exchange";
    public static final String VOLUNTEER_START_TIMEOUT_QUEUE = "volunteer.start.timeout.queue";
    public static final String VOLUNTEER_START_TIMEOUT_ROUTING_KEY = "volunteer.start.timeout";

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        
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
                .error("消息被退回: exchange={}, routingKey={}, message={}", 
                    returned.getExchange(), returned.getRoutingKey(), returned.getMessage());
        });
        
        return template;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(RabbitTemplate rabbitTemplate) {
        return new RabbitAdmin(rabbitTemplate);
    }


    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange orderDeadLetterExchange() {
        return new DirectExchange(ORDER_DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public FanoutExchange orderStatusFanoutExchange() {
        return new FanoutExchange(ORDER_STATUS_FANOUT_EXCHANGE, true, false);
    }

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

    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", ORDER_DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", ORDER_CANCEL_ROUTING_KEY);
        args.put("x-message-ttl", orderTimeoutMinutes * 60 * 1000);
        
        return QueueBuilder.durable(ORDER_DELAY_QUEUE)
            .withArguments(args)
            .build();
    }

    @Bean
    public Queue orderCancelDlxQueue() {
        return QueueBuilder.durable(ORDER_CANCEL_DLX_QUEUE).build();
    }

    @Bean
    public Queue notificationUserQueue() {
        return QueueBuilder.durable(NOTIFICATION_USER_QUEUE).build();
    }

    @Bean
    public Queue notificationVolunteerQueue() {
        return QueueBuilder.durable(NOTIFICATION_VOLUNTEER_QUEUE).build();
    }

    @Bean
    public Queue notificationAdminQueue() {
        return QueueBuilder.durable(NOTIFICATION_ADMIN_QUEUE).build();
    }

    @Bean
    public Queue approvalAdminQueue() {
        return QueueBuilder.durable(APPROVAL_ADMIN_QUEUE).build();
    }

    @Bean
    public Queue volunteerApprovalQueue() {
        return QueueBuilder.durable("approval.result.volunteer.queue").build();
    }

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

    @Bean
    public Binding orderCancelDlxBinding(Queue orderCancelDlxQueue, DirectExchange orderDeadLetterExchange) {
        return BindingBuilder.bind(orderCancelDlxQueue)
            .to(orderDeadLetterExchange)
            .with(ORDER_CANCEL_ROUTING_KEY);
    }

    @Bean
    public Binding notificationUserBinding(Queue notificationUserQueue, FanoutExchange orderStatusFanoutExchange) {
        return BindingBuilder.bind(notificationUserQueue).to(orderStatusFanoutExchange);
    }

    @Bean
    public Binding notificationVolunteerBinding(Queue notificationVolunteerQueue, FanoutExchange orderStatusFanoutExchange) {
        return BindingBuilder.bind(notificationVolunteerQueue).to(orderStatusFanoutExchange);
    }

    @Bean
    public Binding notificationAdminBinding(Queue notificationAdminQueue, FanoutExchange orderStatusFanoutExchange) {
        return BindingBuilder.bind(notificationAdminQueue).to(orderStatusFanoutExchange);
    }

    @Bean
    public Binding approvalAdminBinding(Queue approvalAdminQueue, FanoutExchange approvalSubmitFanoutExchange) {
        return BindingBuilder.bind(approvalAdminQueue).to(approvalSubmitFanoutExchange);
    }

    @Bean
    public Binding volunteerApprovalBinding(Queue volunteerApprovalQueue, DirectExchange approvalResultDirectExchange) {
        return BindingBuilder.bind(volunteerApprovalQueue)
            .to(approvalResultDirectExchange)
            .with(APPROVAL_RESULT_ROUTING_KEY_PREFIX + "*");
    }

    @Bean
    public Binding volunteerStartTimeoutDelayBinding(
            @Qualifier("volunteerStartTimeoutQueue") Queue volunteerStartTimeoutDelayQueue, 
            DirectExchange volunteerStartTimeoutExchange) {
        return BindingBuilder.bind(volunteerStartTimeoutDelayQueue)
            .to(volunteerStartTimeoutExchange)
            .with("volunteer.start.timeout.delay");
    }

    @Bean
    public Binding volunteerStartTimeoutDlxBinding(
            @Qualifier("volunteerStartTimeoutDlxQueue") Queue volunteerStartTimeoutDlxQueue, 
            DirectExchange volunteerStartTimeoutExchange) {
        return BindingBuilder.bind(volunteerStartTimeoutDlxQueue)
            .to(volunteerStartTimeoutExchange)
            .with(VOLUNTEER_START_TIMEOUT_ROUTING_KEY);
    }
}
