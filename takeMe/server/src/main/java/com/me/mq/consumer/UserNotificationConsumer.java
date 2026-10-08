package com.me.mq.consumer;

import com.me.dto.OrderStatusChangeMessage;
import com.me.mq.config.RabbitMQConfig;
import com.me.service.MessageService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("${middleware.enabled:true} && ${middleware.rabbitmq.enabled:true}")
@RequiredArgsConstructor
public class UserNotificationConsumer {

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

    //处理用户订单变更通知
    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_USER_QUEUE, containerFactory = "reliableRabbitListenerContainerFactory")
    public void handleUserNotification(OrderStatusChangeMessage message, org.springframework.amqp.core.Message msg, Channel channel) {
        try {
            if (message == null) {
                log.warn("收到空消息，跳过处理");
                return;
            }

            Long userId = message.getUserId();
            if (userId == null) {
                log.warn("用户ID为空，跳过通知: orderId={}", message.getOrderId());
                return;
            }

            String title = buildTitle(message);
            String content = buildContent(message);

            com.me.entity.Message notification = new com.me.entity.Message();
            notification.setReceiverId(userId);
            notification.setReceiverType(2);
            notification.setType(1);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setIsRead(0);
            notification.setRelatedOrderId(message.getOrderId());
            notification.setCreateTime(message.getChangeTime());

            messageService.sendEventMessage(notification, msg == null ? null : msg.getMessageProperties().getMessageId());

            com.me.dto.OrderStatusChangeWsMessage wsMessage = com.me.dto.OrderStatusChangeWsMessage.builder()
                .orderId(message.getOrderId())
                .orderNo(message.getOrderNo())
                .oldStatus(message.getOldStatus())
                .newStatus(message.getNewStatus())
                .userId(message.getUserId())
                .volunteerId(message.getVolunteerId())
                .message(content)
                .changeTime(message.getChangeTime())
                .build();
            
            com.me.websocket.OrderWebSocketEndpoint.sendMessageToUser(userId.toString(), wsMessage);

            log.info("用户通知发送成功: userId={}, orderId={}, status={}→{}", 
                userId, message.getOrderId(), message.getOldStatus(), message.getNewStatus());
        } catch (Exception e) {
            log.error("用户通知处理失败: orderId={}", message != null ? message.getOrderId() : "unknown", e);
            throw new IllegalStateException("用户通知处理失败", e);
        }
    }


    //生成用户通知标题
    private String buildTitle(OrderStatusChangeMessage message) {
        // 服务项发生动作但父订单聚合状态未变时，仍明确展示本次动作。
        if (message.getRemark() != null) return message.getRemark();
        Integer newStatus = message.getNewStatus();
        switch (newStatus) {
            case 1:
                return "服务已接单";
            case 2:
                return "服务进行中";
            case 3:
                return "服务已完成，待确认";
            case 4:
                return "订单已完成";
            case 5:
                return "订单已取消";
            default:
                return "订单状态更新";
        }
    }

    //生成用户通知内容
    private String buildContent(OrderStatusChangeMessage message) {
        String oldStatusText = STATUS_TEXT_MAP.getOrDefault(message.getOldStatus(), "未知");
        String newStatusText = STATUS_TEXT_MAP.getOrDefault(message.getNewStatus(), "未知");

        StringBuilder content = new StringBuilder();
        content.append("您的订单 ").append(message.getOrderNo()).append(" 状态已更新：");
        content.append(oldStatusText).append(" → ").append(newStatusText);

        if (message.getRemark() != null && !message.getRemark().isEmpty()) {
            content.append("（").append(message.getRemark()).append("）");
        }

        return content.toString();
    }
}

