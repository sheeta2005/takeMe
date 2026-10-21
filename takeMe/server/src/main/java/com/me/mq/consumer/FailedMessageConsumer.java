package com.me.mq.consumer;

import com.me.mq.config.RabbitMQConfig;
import com.me.service.FailedMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnExpression("${middleware.enabled:true} && ${middleware.rabbitmq.enabled:true}")
public class FailedMessageConsumer {
    private final FailedMessageService failedMessageService;

    // 不反序列化业务 DTO，格式错误的毒消息也能原样归档；事务提交后容器才 ACK。
    @RabbitListener(queues = RabbitMQConfig.CONSUMER_FAILED_QUEUE,
            containerFactory = "failedMessageListenerContainerFactory")
    public void archive(Message message) {
        failedMessageService.archive(message);
    }
}
