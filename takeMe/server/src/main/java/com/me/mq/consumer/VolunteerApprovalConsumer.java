package com.me.mq.consumer;

import com.me.dto.ApprovalResultMessage;
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
public class VolunteerApprovalConsumer {

    private final MessageService messageService;

    private static final Map<String, String> TYPE_TEXT_MAP = new HashMap<>();
    static {
        TYPE_TEXT_MAP.put("leave", "请假");
        TYPE_TEXT_MAP.put("service_days_change", "工作日期变更");
        TYPE_TEXT_MAP.put("service_change", "服务变更");
        TYPE_TEXT_MAP.put("points_appeal", "积分申诉");
        TYPE_TEXT_MAP.put("register", "注册");
    }

    @RabbitListener(queues = "#{@volunteerApprovalQueue}")
    public void handleVolunteerApprovalResult(ApprovalResultMessage message, org.springframework.amqp.core.Message msg, Channel channel) {
        try {
            if (message == null) {
                log.warn("收到空消息，跳过处理");
                if (msg != null && channel != null) {
                    channel.basicAck(msg.getMessageProperties().getDeliveryTag(), false);
                }
                return;
            }

            Long volunteerId = message.getApplicantId();
            if (volunteerId == null) {
                log.warn("志愿者ID为空，跳过通知: approvalId={}", message.getApprovalId());
                if (msg != null && channel != null) {
                    channel.basicAck(msg.getMessageProperties().getDeliveryTag(), false);
                }
                return;
            }

            String typeText = TYPE_TEXT_MAP.getOrDefault(message.getType(), "业务");
            String resultText = "approved".equals(message.getResult()) ? "已通过" : "已拒绝";
            String title = typeText + "申请" + resultText;

            StringBuilder content = new StringBuilder();
            content.append("您的").append(typeText).append("申请").append(resultText);
            if (message.getRemark() != null && !message.getRemark().isEmpty()) {
                content.append("，审批意见：").append(message.getRemark());
            }

            com.me.entity.Message notification = new com.me.entity.Message();
            notification.setReceiverId(volunteerId);
            notification.setReceiverType(1);
            notification.setType(1);
            notification.setTitle(title);
            notification.setContent(content.toString());
            notification.setIsRead(0);
            notification.setRelatedOrderId(message.getApprovalId());
            notification.setCreateTime(message.getApproveTime());

            messageService.sendMessage(notification);

            if (msg != null && channel != null) {
                channel.basicAck(msg.getMessageProperties().getDeliveryTag(), false);
            }
            log.info("志愿者审批结果通知发送成功: volunteerId={}, approvalId={}, result={}",
                    volunteerId, message.getApprovalId(), message.getResult());
        } catch (Exception e) {
            log.error("志愿者审批结果通知处理失败: approvalId={}", message != null ? message.getApprovalId() : "unknown", e);
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

