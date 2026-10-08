package com.me.mq.consumer;

import com.me.dto.VolunteerStartTimeoutMessage;
import com.me.mq.config.RabbitMQConfig;
import com.me.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
// 保留消费者消化历史 TTL 消息，新接单不再生产此类事件。
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("${middleware.enabled:true} && ${middleware.rabbitmq.enabled:true}")
@RequiredArgsConstructor
public class VolunteerStartTimeoutConsumer {

    private final OrderService orderService;

    //处理志愿者超时未开始服务
    @RabbitListener(queues = RabbitMQConfig.VOLUNTEER_START_TIMEOUT_QUEUE, containerFactory = "reliableRabbitListenerContainerFactory")
    public void handleVolunteerStartTimeout(VolunteerStartTimeoutMessage message,
                                            org.springframework.amqp.core.Message msg,
                                            Channel channel) {
        try {
            if (message != null && message.getOrderItemId() != null) {
                // 复核当前接单者和预约时间，重复或过早消息不改变状态。
                orderService.expireAcceptedItem(message.getOrderItemId(), message.getVolunteerId());
            }
        } catch (Exception e) {
            log.error("志愿者启动超时处理失败: orderItemId={}",
                    message != null ? message.getOrderItemId() : null, e);
            throw new IllegalStateException("志愿者启动超时处理失败", e);
        }
    }
}
