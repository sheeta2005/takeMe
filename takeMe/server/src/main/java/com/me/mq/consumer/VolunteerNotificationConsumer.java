package com.me.mq.consumer;

import com.me.dto.OrderStatusChangeMessage;
import com.me.mq.config.RabbitMQConfig;
import com.me.service.MessageService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class VolunteerNotificationConsumer {

    private final MessageService messageService;

    private static final Map<Integer, String> STATUS_TEXT_MAP = new HashMap<>();
    static {
        STATUS_TEXT_MAP.put(0, "待接单");
        STATUS_TEXT_MAP.put(1, "已接单");
        STATUS_TEXT_MAP.put(2, "服务中");
        STATUS_TEXT_MAP.put(3, "待确认");
        STATUS_TEXT_MAP.put(4, "已完成");
        STATUS_TEXT_MAP.put(5, "已取消");
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_VOLUNTEER_QUEUE)
    public void handleVolunteerNotification(OrderStatusChangeMessage message, org.springframework.amqp.core.Message msg, Channel channel) {
        try {
            if (message == null) {
                log.warn("收到空消息，跳过处理");
                if (msg != null && channel != null) {
                    channel.basicAck(msg.getMessageProperties().getDeliveryTag(), false);
                }
                return;
            }

            Long volunteerId = message.getVolunteerId();
            if (volunteerId == null) {
                if (msg != null && channel != null) {
                    channel.basicAck(msg.getMessageProperties().getDeliveryTag(), false);
                }
                return;
            }

            String title = buildTitle(message);
            String content = buildContent(message);

            com.me.entity.Message notification = new com.me.entity.Message();
            notification.setReceiverId(volunteerId);
            notification.setReceiverType(1);
            notification.setType(1);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setIsRead(0);
            notification.setRelatedOrderId(message.getOrderId());
            notification.setCreateTime(message.getChangeTime());

            messageService.sendMessage(notification);

            if (msg != null && channel != null) {
                channel.basicAck(msg.getMessageProperties().getDeliveryTag(), false);
            }
            log.info("志愿者通知发送成功: volunteerId={}, orderId={}, status={}→{}", 
                volunteerId, message.getOrderId(), message.getOldStatus(), message.getNewStatus());
        } catch (Exception e) {
            log.error("志愿者通知处理失败: orderId={}", message != null ? message.getOrderId() : "unknown", e);
            try {
                if (msg != null && channel != null) {
                    channel.basicNack(msg.getMessageProperties().getDeliveryTag(), false, true);
                }
            } catch (IOException ioException) {
                log.error("NACK失败", ioException);
            }
        }
    }


    private String buildTitle(OrderStatusChangeMessage message) {
        Integer newStatus = message.getNewStatus();
        switch (newStatus) {
            case 2: return "开始服务提醒";
            case 3: return "服务完成确认";
            case 4: return "订单最终完成";
            case 5: return "订单已取消";
            default: return "订单状态更新";
        }
    }

    private String buildContent(OrderStatusChangeMessage message) {
        String oldStatusText = STATUS_TEXT_MAP.getOrDefault(message.getOldStatus(), "未知");
        String newStatusText = STATUS_TEXT_MAP.getOrDefault(message.getNewStatus(), "未知");

        StringBuilder content = new StringBuilder();
        content.append("您负责的订单 ").append(message.getOrderNo()).append(" 状态已更新：");
        content.append(oldStatusText).append(" → ").append(newStatusText);

        if (message.getRemark() != null && !message.getRemark().isEmpty()) {
            content.append("（").append(message.getRemark()).append("）");
        }

        return content.toString();
    }
}

