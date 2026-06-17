package com.me.mq.consumer;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;

import java.io.IOException;

@Slf4j
public abstract class BaseConsumer {

    protected void handleMessage(String message, Message msg, Channel channel) {
        try {
            log.info("收到消息: {}", message);

            processMessage(message);

            long deliveryTag = msg.getMessageProperties().getDeliveryTag();
            channel.basicAck(deliveryTag, false);

            log.info("消息处理完成");
        } catch (Exception e) {
            log.error("消息处理失败", e);
            try {
                long deliveryTag = msg.getMessageProperties().getDeliveryTag();
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ioException) {
                log.error("消息拒绝失败", ioException);
            }
        }
    }

    protected abstract void processMessage(String message);
}
