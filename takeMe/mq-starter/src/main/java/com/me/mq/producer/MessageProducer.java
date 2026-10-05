package com.me.mq.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.AmqpException;
import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("#{${middleware.enabled:true} && ${middleware.rabbitmq.enabled:true}}")
    private boolean rabbitmqEnabled;

    public void sendMessage(String exchange, String routingKey, Object message) {
        if (!rabbitmqEnabled) {
            log.debug("RabbitMQ 已禁用，跳过消息投递 exchange={}, routingKey={}", exchange, routingKey);
            return;
        }
        sendConfirmed(exchange, routingKey, message, UUID.randomUUID().toString());
    }

    // 本地消息表调用此方法，确认超时、NACK 和退回均作为发送失败。
    public void sendConfirmed(String exchange, String routingKey, Object message, String eventId) {
        if (!rabbitmqEnabled) throw new IllegalStateException("RabbitMQ 已禁用");
        CorrelationData correlation = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend(exchange, routingKey, message, msg -> {
            msg.getMessageProperties().setMessageId(eventId);
            msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return msg;
        }, correlation);
        try {
            CorrelationData.Confirm confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
            if (!confirm.isAck() || correlation.getReturned() != null) {
                throw new AmqpException("消息未进入目标队列: " + confirm.getReason());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AmqpException("等待消息确认被中断", e);
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
            throw new AmqpException("等待消息确认失败", e);
        }
    }

}
