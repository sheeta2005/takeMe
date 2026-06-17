package com.me.mq.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.dto.OrderTimeoutMessage;
import com.me.entity.Order;
import com.me.entity.OrderItem;
import com.me.mapper.OrderItemMapper;
import com.me.mapper.OrderMapper;
import com.me.mq.config.RabbitMQConfig;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutConsumer {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final StringRedisTemplate redisTemplate;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CANCEL_DLX_QUEUE)
    public void handleOrderTimeout(OrderTimeoutMessage message, Message msg, Channel channel) {
        if (message == null) {
            log.warn("收到空消息，跳过处理");
            if (msg != null && channel != null) {
                try {
                    channel.basicAck(msg.getMessageProperties().getDeliveryTag(), false);
                } catch (IOException e) {
                    log.error("ACK失败", e);
                }
            }
            return;
        }

        String lockKey = "mq:idempotent:order:timeout:" + message.getOrderId();

        Boolean isLocked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 300, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(isLocked)) {
            log.warn("订单超时消息重复消费，已跳过: orderId={}", message.getOrderId());
            try {
                if (msg != null && channel != null) {
                    channel.basicAck(msg.getMessageProperties().getDeliveryTag(), false);
                }
            } catch (IOException e) {
                log.error("ACK失败", e);
            }
            return;
        }

        try {
            cancelOrderIfNeeded(message);
            if (msg != null && channel != null) {
                channel.basicAck(msg.getMessageProperties().getDeliveryTag(), false);
            }
            log.info("订单超时处理完成: orderId={}", message.getOrderId());
        } catch (Exception e) {
            log.error("订单超时处理失败: orderId={}", message.getOrderId(), e);
            try {
                if (msg != null && channel != null) {
                    channel.basicNack(msg.getMessageProperties().getDeliveryTag(), false, true);
                }
            } catch (IOException ioException) {
                log.error("NACK失败", ioException);
            }
        }
    }


    private void cancelOrderIfNeeded(OrderTimeoutMessage timeoutMessage) {
        Long orderId = timeoutMessage.getOrderId();

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            log.warn("订单不存在，orderId={}", orderId);
            return;
        }

        if (order.getStatus() != 0) {
            log.info("订单状态已变更，无需取消: orderId={}, status={}", orderId, order.getStatus());
            return;
        }

        order.setStatus(5);
        orderMapper.updateById(order);

        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, orderId);
        itemWrapper.eq(OrderItem::getItemStatus, 0);
        OrderItem item = orderItemMapper.selectOne(itemWrapper);

        if (item != null) {
            item.setItemStatus(5);
            orderItemMapper.updateById(item);
        }

        log.info("订单超时自动取消成功: orderId={}, orderNo={}", orderId, timeoutMessage.getOrderNo());
    }
}

