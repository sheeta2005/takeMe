package com.me.service.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.me.dto.ApprovalResultMessage;
import com.me.dto.ApprovalSubmitMessage;
import com.me.dto.OrderStatusChangeMessage;
import com.me.dto.OrderTimeoutMessage;
import com.me.dto.VolunteerStartTimeoutMessage;
import com.me.entity.OutboxMessage;
import com.me.mapper.OutboxMapper;
import com.me.mq.producer.MessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
public class OutboxDispatchTask {
    private final OutboxMapper outboxMapper;
    private final ObjectMapper objectMapper;
    private final MessageProducer messageProducer;
    private final TransactionTemplate transaction;

    @Value("#{${middleware.enabled:true} && ${middleware.rabbitmq.enabled:true}}")
    private boolean enabled;

    private static final Map<String, Class<?>> PAYLOAD_TYPES = Map.of(
            ApprovalResultMessage.class.getName(), ApprovalResultMessage.class,
            ApprovalSubmitMessage.class.getName(), ApprovalSubmitMessage.class,
            OrderStatusChangeMessage.class.getName(), OrderStatusChangeMessage.class,
            OrderTimeoutMessage.class.getName(), OrderTimeoutMessage.class,
            VolunteerStartTimeoutMessage.class.getName(), VolunteerStartTimeoutMessage.class,
            com.me.entity.Message.class.getName(), com.me.entity.Message.class);


    //内部调用事物不生效，用该工具替代。
    public OutboxDispatchTask(OutboxMapper mapper, ObjectMapper objectMapper,
                              MessageProducer producer, PlatformTransactionManager manager) {
        this.outboxMapper = mapper;
        this.objectMapper = objectMapper;
        this.messageProducer = producer;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Scheduled(fixedDelay = 1000)
    public void dispatch() {
        if (!enabled) return;
        for (String id : outboxMapper.findDueIds()) {
            try {
                dispatchOne(id);
            } catch (Exception e) {
                // 数据库故障或发送后进程退出会留下待发送行，下一轮可恢复。
                log.error("消息发送任务失败: eventId={}", id, e);
            }
        }
    }

    public void dispatchOne(String id) {
        transaction.executeWithoutResult(status -> {
            OutboxMessage message = outboxMapper.selectDueForUpdate(id);
            if (message == null) return;
            message.setAttempts(message.getAttempts() + 1);
            try {
                Class<?> type = PAYLOAD_TYPES.get(message.getPayloadType());
                if (type == null) throw new IllegalArgumentException("不支持的消息类型");
                Object payload = objectMapper.readValue(message.getPayload(), type);
                // 重发保留事件 ID；只有确认成功且未被退回才标记为已发送。
                messageProducer.sendConfirmed(message.getExchangeName(), message.getRoutingKey(), payload, id);
                message.setStatus(1);
                message.setLastError(null);
            } catch (Exception e) {
                long delay = Math.min(300, 1L << Math.min(message.getAttempts(), 9));
                message.setNextAttemptTime(LocalDateTime.now().plusSeconds(delay));
                String error = e.toString();
                message.setLastError(error.substring(0, Math.min(error.length(), 2000)));
                log.warn("消息待重试: eventId={}, attempt={}", id, message.getAttempts(), e);
            }
            message.setUpdateTime(LocalDateTime.now());
            // 成功必须明确清空上一次错误，不能受非空字段更新策略影响。
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<OutboxMessage> update =
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
            update.eq(OutboxMessage::getId, id)
                    .set(OutboxMessage::getStatus, message.getStatus())
                    .set(OutboxMessage::getAttempts, message.getAttempts())
                    .set(OutboxMessage::getNextAttemptTime, message.getNextAttemptTime())
                    .set(OutboxMessage::getLastError, message.getLastError())
                    .set(OutboxMessage::getUpdateTime, message.getUpdateTime());
            if (outboxMapper.update(null, update) != 1) {
                throw new IllegalStateException("消息发送结果保存失败");
            }
        });
    }
}
