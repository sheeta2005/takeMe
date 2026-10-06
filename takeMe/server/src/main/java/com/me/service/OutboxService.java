package com.me.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.me.entity.OutboxMessage;
import com.me.mapper.OutboxMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {
    private final OutboxMapper outboxMapper;
    private final ObjectMapper objectMapper;

    // 必须加入业务事务，登记失败时业务数据也回滚。
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(String exchange, String routingKey, Object payload) {
        OutboxMessage message = new OutboxMessage();
        message.setId(UUID.randomUUID().toString());
        message.setExchangeName(exchange);
        message.setRoutingKey(routingKey);
        message.setPayloadType(payload.getClass().getName());
        try {
            message.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("消息序列化失败", e);
        }
        LocalDateTime now = LocalDateTime.now();
        message.setStatus(0);
        message.setAttempts(0);
        message.setNextAttemptTime(now);
        message.setCreateTime(now);
        message.setUpdateTime(now);
        if (outboxMapper.insert(message) != 1) {
            throw new IllegalStateException("待发送消息登记失败");
        }
    }
}
