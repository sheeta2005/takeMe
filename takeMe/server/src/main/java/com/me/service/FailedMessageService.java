package com.me.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.dto.FailedMessageSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.me.entity.FailedMessage;
import com.me.mapper.FailedMessageMapper;
import com.me.mq.config.RabbitMQConfig;
import com.me.mq.producer.MessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FailedMessageService {
    private static final List<String> REPLAYABLE_QUEUES = List.of(
            RabbitMQConfig.NOTIFICATION_USER_QUEUE,
            RabbitMQConfig.NOTIFICATION_VOLUNTEER_QUEUE,
            RabbitMQConfig.NOTIFICATION_ADMIN_QUEUE,
            RabbitMQConfig.APPROVAL_ADMIN_QUEUE,
            "approval.result.volunteer.queue",
            RabbitMQConfig.BROADCAST_QUEUE,
            RabbitMQConfig.ORDER_DELAY_QUEUE,
            RabbitMQConfig.ORDER_CANCEL_DLX_QUEUE,
            "volunteer.start.timeout.delay.queue",
            RabbitMQConfig.VOLUNTEER_START_TIMEOUT_QUEUE);
    private static final TypeReference<LinkedHashMap<String, Object>> HEADERS_TYPE = new TypeReference<>() {};

    private final FailedMessageMapper failedMessageMapper;
    private final ObjectMapper objectMapper;
    private final MessageProducer messageProducer;

    // 原始字节归档，不依赖业务 DTO；数据库事务提交成功后监听容器才确认消息。
    @Transactional(rollbackFor = Exception.class)
    public void archive(Message message) {
        MessageProperties properties = message.getMessageProperties();
        Map<String, Object> headers = properties.getHeaders();
        String sourceQueue = sourceQueue(headers);
        String reason = headers.containsKey("x-exception-message") ? "consumer-failure" : deathReason(headers);
        String messageId = properties.getMessageId();
        String identity = messageId == null || messageId.isBlank()
                ? HexFormat.of().formatHex(sha256(message.getBody())) : messageId;
        LocalDateTime now = LocalDateTime.now();

        FailedMessage failed = new FailedMessage();
        failed.setId(HexFormat.of().formatHex(sha256((sourceQueue + "\u0000" + identity).getBytes(StandardCharsets.UTF_8))));
        failed.setMessageId(identity);
        failed.setSourceQueue(sourceQueue);
        failed.setExchangeName(headerOrDefault(headers, "x-original-exchange", properties.getReceivedExchange()));
        failed.setRoutingKey(headerOrDefault(headers, "x-original-routingKey", properties.getReceivedRoutingKey()));
        failed.setBody(message.getBody());
        try {
            failed.setHeadersJson(objectMapper.writeValueAsString(jsonSafeHeaders(headers)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("失败消息头序列化失败", e);
        }
        failed.setContentType(properties.getContentType());
        failed.setContentEncoding(properties.getContentEncoding());
        failed.setCorrelationId(properties.getCorrelationId());
        failed.setReason(reason);
        Object error = headers.get("x-exception-message");
        failed.setLastError(error == null ? null : String.valueOf(error));
        failed.setCreateTime(now);
        failed.setLastFailureTime(now);
        // MySQL 的 UPSERT 在更新已有记录时返回 2，同样属于归档成功。
        if (failedMessageMapper.archive(failed) < 1) {
            throw new IllegalStateException("失败消息归档写入失败");
        }
        log.debug("消息进入失败归档: id={}, sourceQueue={}, reason={}", failed.getId(), sourceQueue, reason);
    }

    // 列表只读取摘要，避免将全部原始消息体和异常堆栈传回管理端。
    public Page<FailedMessageSummary> page(long pageNum, long pageSize, Integer status) {
        if (pageNum < 1 || pageNum > Integer.MAX_VALUE || pageSize < 1 || pageSize > 100
                || (status != null && status != 0 && status != 1)) {
            throw new IllegalArgumentException("分页参数无效");
        }
        long total = failedMessageMapper.count(status);
        Page<FailedMessageSummary> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(failedMessageMapper.selectPage(page.offset(), pageSize, status));
        return page;
    }

    // 行锁串行化人工重放；原事件 ID 使业务落库在确认超时后的重试中仍可去重。
    @Transactional(rollbackFor = Exception.class)
    public boolean replay(String id, Long adminId) {
        FailedMessage failed = failedMessageMapper.selectForReplay(id);
        if (failed == null || failed.getStatus() != 0) return false;
        if (!REPLAYABLE_QUEUES.contains(failed.getSourceQueue())) {
            failed.setLastReplayError("来源队列未知或不允许重放");
            failed.setLastReplayTime(LocalDateTime.now());
            failed.setReplayedBy(adminId);
            recordReplay(failed);
            return false;
        }

        failed.setReplayedBy(adminId);
        failed.setLastReplayTime(LocalDateTime.now());
        try {
            LinkedHashMap<String, Object> headers = objectMapper.readValue(failed.getHeadersJson(), HEADERS_TYPE);
            headers.keySet().removeIf(key -> key.startsWith("x-exception-") || key.startsWith("x-original-")
                    || key.equals(RabbitMQConfig.FAILED_SOURCE_QUEUE_HEADER) || key.equals("x-death")
                    || key.startsWith("x-first-death-") || key.startsWith("x-last-death-"));
            MessageProperties properties = new MessageProperties();
            properties.getHeaders().putAll(headers);
            properties.setMessageId(failed.getMessageId());
            properties.setContentType(failed.getContentType());
            properties.setContentEncoding(failed.getContentEncoding());
            properties.setCorrelationId(failed.getCorrelationId());
            messageProducer.replayConfirmed(failed.getSourceQueue(), new Message(failed.getBody(), properties));
            failed.setStatus(1);
            failed.setLastReplayError(null);
        } catch (Exception e) {
            failed.setLastReplayError(shortError(e));
            log.warn("失败消息重放失败: id={}, sourceQueue={}", id, failed.getSourceQueue(), e);
        }
        recordReplay(failed);
        return failed.getStatus() == 1;
    }

    // 按来源和失败原因聚合，供告警任务与管理端查询。
    public List<Map<String, Object>> recentFailures() {
        return failedMessageMapper.recentFailures();
    }

    // 应用失败使用显式来源，Broker 溢出使用最近一次 x-death 中记录的来源。
    private static String sourceQueue(Map<String, Object> headers) {
        Object source = headers.get(RabbitMQConfig.FAILED_SOURCE_QUEUE_HEADER);
        if (source instanceof String queue && !queue.isBlank()) return queue;
        Object deaths = headers.get("x-death");
        if (deaths instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> death && death.get("queue") != null) return death.get("queue").toString();
            }
        }
        return "";
    }

    // 状态记录和 Broker 发布不构成分布式事务；记录失败时抛出，不能向管理端假报成功。
    private void recordReplay(FailedMessage failed) {
        if (failedMessageMapper.recordReplay(failed) != 1) {
            throw new IllegalStateException("重放结果保存失败");
        }
    }

    // 保存实际的死信原因，区分容量溢出、到期和拒绝。
    private static String deathReason(Map<String, Object> headers) {
        if (headers.get("x-death") instanceof List<?> deaths && !deaths.isEmpty()
                && deaths.getFirst() instanceof Map<?, ?> death && death.get("reason") != null) {
            return death.get("reason").toString();
        }
        return "unknown";
    }

    // 优先记录 recoverer 保存的原路由，而非失败归档交换机本身。
    private static String headerOrDefault(Map<String, Object> headers, String key, String fallback) {
        Object value = headers.get(key);
        return value == null ? fallback == null ? "" : fallback : value.toString();
    }

    // RabbitMQ 的 LongString 等协议类型先转换为可归档的 JSON 值。
    private static Map<String, Object> jsonSafeHeaders(Map<String, Object> headers) {
        Map<String, Object> safe = new LinkedHashMap<>();
        headers.forEach((key, value) -> safe.put(key, jsonSafeValue(value)));
        return safe;
    }

    // 保留业务使用的字符串类型头；死信嵌套元数据同样可保存。
    private static Object jsonSafeValue(Object value) {
        if (value instanceof com.rabbitmq.client.LongString longString) return longString.toString();
        if (value instanceof byte[] bytes) return java.util.Base64.getEncoder().encodeToString(bytes);
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> safe = new LinkedHashMap<>();
            map.forEach((key, item) -> safe.put(String.valueOf(key), jsonSafeValue(item)));
            return safe;
        }
        if (value instanceof List<?> list) {
            List<Object> safe = new ArrayList<>(list.size());
            list.forEach(item -> safe.add(jsonSafeValue(item)));
            return safe;
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof String || value == null) return value;
        return String.valueOf(value);
    }

    // 无事件 ID 的历史消息使用稳定内容摘要；不同来源队列分别归档。
    private static byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // 失败原因在表字段范围内截断，保留完整原消息不受影响。
    private static String shortError(Exception e) {
        String error = e.toString();
        return error.substring(0, Math.min(error.length(), 2000));
    }
}
