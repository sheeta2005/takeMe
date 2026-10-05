package com.me.mq.consumer;

import com.me.dto.OrderStatusChangeMessage;
import com.me.mq.config.RabbitMQConfig;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
// 仅在旧审计消息迁移期间启用，正常业务不再维护日志专用 MQ 分支。
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression(
        "${middleware.enabled:true} && ${middleware.rabbitmq.enabled:true} && ${middleware.rabbitmq.legacy-audit.enabled:false}")
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

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_ADMIN_QUEUE, containerFactory = "reliableRabbitListenerContainerFactory")
    public void handleAdminAudit(OrderStatusChangeMessage message, Message msg, Channel channel) {
        try {
            if (message == null) {
                log.warn("收到空消息，跳过处理");
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

        } catch (Exception e) {
            log.error("管理员审计日志处理失败: orderId={}", message != null ? message.getOrderId() : "unknown", e);
            throw new IllegalStateException("管理员审计处理失败", e);
        }
    }
}

