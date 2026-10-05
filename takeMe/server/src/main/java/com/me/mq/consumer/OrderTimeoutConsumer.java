package com.me.mq.consumer;

import com.me.dto.OrderTimeoutMessage;
import com.me.mq.config.RabbitMQConfig;
import com.me.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
// 保留消费者消化历史 TTL 消息，新订单不再生产此类事件。
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("${middleware.enabled:true} && ${middleware.rabbitmq.enabled:true}")
@RequiredArgsConstructor
public class OrderTimeoutConsumer {

    private final OrderService orderService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CANCEL_DLX_QUEUE, containerFactory = "reliableRabbitListenerContainerFactory")
    public void handleOrderTimeout(OrderTimeoutMessage message, Message msg, Channel channel) {
        try {
            if (message != null && message.getOrderId() != null) {
                // 消息可能早于预约时间到达，服务层逐项复核。
                orderService.expirePendingItems(message.getOrderId());
            }
        } catch (Exception e) {
            log.error("订单超时处理失败: orderId={}", message != null ? message.getOrderId() : null, e);
            // 传播异常，由容器执行有限重试，成功或错误消息落队后再自动确认。
            throw new IllegalStateException("订单超时处理失败", e);
        }
    }
}
