package com.me.mq.consumer;

import com.me.dto.OrderStatusChangeMessage;
import com.me.mq.config.RabbitMQConfig;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class AdminAuditConsumer {

    private static final Map<Integer, String> STATUS_TEXT_MAP = new HashMap<>();
    static {
        STATUS_TEXT_MAP.put(0, "待接单");
        STATUS_TEXT_MAP.put(1, "已接单");
        STATUS_TEXT_MAP.put(2, "服务中");
        STATUS_TEXT_MAP.put(3, "待确认");
        STATUS_TEXT_MAP.put(4, "已完成");
        STATUS_TEXT_MAP.put(5, "已取消");
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_ADMIN_QUEUE)
    public void handleAdminAudit(OrderStatusChangeMessage message, Message msg, Channel channel) {
        try {
            if (message == null) {
                log.warn("收到空消息，跳过处理");
                if (msg != null && channel != null) {
                    channel.basicAck(msg.getMessageProperties().getDeliveryTag(), false);
                }
                return;
            }

            String oldStatusText = STATUS_TEXT_MAP.getOrDefault(message.getOldStatus(), "未知");
            String newStatusText = STATUS_TEXT_MAP.getOrDefault(message.getNewStatus(), "未知");

            log.info("[订单审计] orderId={}, orderNo={}, 状态变更: {}→{}, 用户ID={}, 志愿者ID={}, 备注={}, 时间={}",
                    message.getOrderId(),
                    message.getOrderNo(),
                    oldStatusText,
                    newStatusText,
                    message.getUserId(),
                    message.getVolunteerId(),
                    message.getRemark(),
                    message.getChangeTime()
            );

            if (msg != null && channel != null) {
                channel.basicAck(msg.getMessageProperties().getDeliveryTag(), false);
            }
        } catch (Exception e) {
            log.error("管理员审计日志处理失败: orderId={}", message != null ? message.getOrderId() : "unknown", e);
            try {
                if (msg != null && channel != null) {
                    channel.basicNack(msg.getMessageProperties().getDeliveryTag(), false, true);
                }
            } catch (IOException ioException) {
                log.error("NACK失败", ioException);
            }
        }
    }
}

