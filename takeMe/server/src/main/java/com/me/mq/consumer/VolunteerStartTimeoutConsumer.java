package com.me.mq.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.dto.VolunteerStartTimeoutMessage;
import com.me.entity.Order;
import com.me.entity.OrderItem;
import com.me.mapper.OrderItemMapper;
import com.me.mapper.OrderMapper;
import com.me.mq.config.RabbitMQConfig;
import com.me.service.MessageService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class VolunteerStartTimeoutConsumer {

    private final OrderItemMapper orderItemMapper;
    private final OrderMapper orderMapper;
    private final MessageService messageService;

    @RabbitListener(queues = RabbitMQConfig.VOLUNTEER_START_TIMEOUT_QUEUE)
    public void handleVolunteerStartTimeout(VolunteerStartTimeoutMessage message,
                                            org.springframework.amqp.core.Message msg,
                                            Channel channel) {
        try {
            if (message == null) {
                log.warn("收到空消息，跳过处理");
                if (msg != null && channel != null) {
                    channel.basicAck(msg.getMessageProperties().getDeliveryTag(), false);
                }
                return;
            }

            Long orderItemId = message.getOrderItemId();
            Long volunteerId = message.getVolunteerId();

            // 查询OrderItem当前状态
            OrderItem item = orderItemMapper.selectById(orderItemId);
            if (item == null) {
                log.warn("OrderItem不存在: orderItemId={}", orderItemId);
                if (msg != null && channel != null) {
                    channel.basicAck(msg.getMessageProperties().getDeliveryTag(), false);
                }
                return;
            }

            // 如果状态仍是1（已接单但未启动），执行自动放弃
            if (item.getItemStatus() == 1 && item.getVolunteerId() != null
                    && item.getVolunteerId().equals(volunteerId)) {

                log.info("志愿者启动服务超时，自动放弃: orderItemId={}, volunteerId={}, orderId={}",
                        orderItemId, volunteerId, message.getOrderId());

                // 执行与volunteerAbandonOrder相同的逻辑
                item.setVolunteerId(null);
                item.setItemStatus(0);
                orderItemMapper.updateById(item);

                // 更新订单的志愿者ID列表
                updateOrderVolunteerIds(message.getOrderId());

                // 更新订单状态
                updateOrderStatus(message.getOrderId());

                // 发送通知给用户
                Order order = orderMapper.selectById(message.getOrderId());
                if (order != null) {
                    sendMessage(order.getUserId(), 2, 0,
                            "志愿者超时未启动服务",
                            "您订单的志愿者接单后超过预约时间10分钟仍未启动服务，系统已自动取消该志愿者的接取，将重新安排接单",
                            orderItemId);

                    // 发送通知给志愿者
                    sendMessage(volunteerId, 1, 0,
                            "服务超时自动取消",
                            "您接取的服务（订单号：" + message.getOrderNo() + "）已超过预约时间10分钟未启动，系统已自动取消您的接取",
                            orderItemId);
                }

                log.info("志愿者启动服务超时处理完成: orderItemId={}", orderItemId);
            } else {
                log.info("OrderItem状态已变更，无需处理: orderItemId={}, currentStatus={}",
                        orderItemId, item.getItemStatus());
            }

            if (msg != null && channel != null) {
                channel.basicAck(msg.getMessageProperties().getDeliveryTag(), false);
            }
        } catch (Exception e) {
            log.error("志愿者启动超时处理失败: orderItemId={}",
                    message != null ? message.getOrderItemId() : "unknown", e);
            try {
                if (msg != null && channel != null) {
                    channel.basicNack(msg.getMessageProperties().getDeliveryTag(), false, true);
                }
            } catch (IOException ioException) {
                log.error("NACK失败", ioException);
            }
        }
    }

    private void updateOrderVolunteerIds(Long orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        wrapper.isNotNull(OrderItem::getVolunteerId);
        wrapper.ne(OrderItem::getItemStatus, 0);
        java.util.List<OrderItem> items = orderItemMapper.selectList(wrapper);

        java.util.Set<Long> volunteerIdSet = items.stream()
                .map(OrderItem::getVolunteerId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());

        Order order = orderMapper.selectById(orderId);
        if (order != null) {
            if (volunteerIdSet.isEmpty()) {
                order.setVolunteerIds(null);
            } else {
                order.setVolunteerIds(volunteerIdSet.stream()
                        .map(String::valueOf)
                        .collect(java.util.stream.Collectors.joining(",")));
            }
            orderMapper.updateById(order);
        }
    }

    private Integer updateOrderStatus(Long orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        java.util.List<OrderItem> items = orderItemMapper.selectList(wrapper);

        if (items == null || items.isEmpty()) {
            return null;
        }

        boolean allCompleted = items.stream().allMatch(item -> item.getItemStatus() == 4);
        boolean hasInProgress = items.stream().anyMatch(item -> item.getItemStatus() == 2);
        boolean hasConfirmed = items.stream().anyMatch(item -> item.getItemStatus() == 3);
        boolean hasAccepted = items.stream().anyMatch(item -> item.getItemStatus() == 1);

        Integer newStatus = null;
        if (allCompleted) {
            newStatus = 4;
        } else if (hasInProgress) {
            newStatus = 2;
        } else if (hasConfirmed) {
            newStatus = 3;
        } else if (hasAccepted) {
            newStatus = 1;
        } else {
            newStatus = 0;
        }

        Order order = orderMapper.selectById(orderId);
        if (order != null && !newStatus.equals(order.getStatus())) {
            order.setStatus(newStatus);
            orderMapper.updateById(order);
        }

        return newStatus;
    }

    private void sendMessage(Long receiverId, Integer receiverType, Integer type,
                             String title, String content, Long relatedOrderId) {
        com.me.entity.Message message = new com.me.entity.Message();
        message.setReceiverId(receiverId);
        message.setReceiverType(receiverType);
        message.setType(type);
        message.setTitle(title);
        message.setContent(content);
        message.setIsRead(0);
        message.setRelatedOrderId(relatedOrderId);
        message.setCreateTime(LocalDateTime.now());
        messageService.sendMessage(message);
    }
}
