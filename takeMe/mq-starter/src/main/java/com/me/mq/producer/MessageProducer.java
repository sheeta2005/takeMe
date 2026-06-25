package com.me.mq.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${middleware.rabbitmq.enabled:true}")
    private boolean rabbitmqEnabled;

    public void sendMessage(String exchange, String routingKey, Object message) {
        if (!rabbitmqEnabled) {
            log.debug("RabbitMQ 已禁用，跳过消息投递 exchange={}, routingKey={}", exchange, routingKey);
            return;
        }
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }

    public void sendOrderMessage(Object message) {
        if (!rabbitmqEnabled) {
            log.debug("RabbitMQ 已禁用，跳过订单消息投递");
            return;
        }
        rabbitTemplate.convertAndSend("", "order.queue", message);
    }
}
