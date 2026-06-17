package com.me.mq.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.dto.ApprovalSubmitMessage;
import com.me.entity.Admin;
import com.me.mapper.AdminMapper;
import com.me.mq.config.RabbitMQConfig;
import com.me.service.MessageService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminApprovalConsumer {

    private final MessageService messageService;
    private final AdminMapper adminMapper;

    private static final Map<String, String> TYPE_TEXT_MAP = new HashMap<>();
    static {
        TYPE_TEXT_MAP.put("leave", "请假");
        TYPE_TEXT_MAP.put("service_days_change", "工作日期变更");
        TYPE_TEXT_MAP.put("service_change", "服务变更");
        TYPE_TEXT_MAP.put("points_appeal", "积分申诉");
        TYPE_TEXT_MAP.put("register", "注册");
    }

    @RabbitListener(queues = RabbitMQConfig.APPROVAL_ADMIN_QUEUE)
    public void handleAdminApprovalNotification(ApprovalSubmitMessage message, org.springframework.amqp.core.Message msg, Channel channel) {
        try {
            if (message == null) {
                log.warn("收到空消息，跳过处理");
                if (msg != null && channel != null) {
                    channel.basicAck(msg.getMessageProperties().getDeliveryTag(), false);
                }
                return;
            }

            LambdaQueryWrapper<Admin> adminWrapper = new LambdaQueryWrapper<>();
            List<Admin> admins = adminMapper.selectList(adminWrapper);

            if (admins.isEmpty()) {
                log.warn("未找到任何管理员，跳过通知发送");
                if (msg != null && channel != null) {
                    channel.basicAck(msg.getMessageProperties().getDeliveryTag(), false);
                }
                return;
            }

            String typeText = TYPE_TEXT_MAP.getOrDefault(message.getType(), "业务");
            String title = "新的" + typeText + "申请待审批";
            String content = String.format("申请人：%s（ID：%d）提交了%s申请，请及时处理。",
                    message.getApplicantName(),
                    message.getApplicantId(),
                    typeText
            );

            for (Admin admin : admins) {
                com.me.entity.Message notification = new com.me.entity.Message();
                notification.setReceiverId(admin.getId());
                notification.setReceiverType(0);
                notification.setType(1);
                notification.setTitle(title);
                notification.setContent(content);
                notification.setIsRead(0);
                notification.setRelatedOrderId(message.getApprovalId());
                notification.setCreateTime(message.getSubmitTime());

                messageService.sendMessage(notification);
            }

            if (msg != null && channel != null) {
                channel.basicAck(msg.getMessageProperties().getDeliveryTag(), false);
            }
            log.info("管理员审批通知发送成功: approvalId={}, type={}, adminCount={}",
                    message.getApprovalId(), message.getType(), admins.size());
        } catch (Exception e) {
            log.error("管理员审批通知处理失败: approvalId={}", message != null ? message.getApprovalId() : "unknown", e);
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

