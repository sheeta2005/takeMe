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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
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

    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        
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
}
