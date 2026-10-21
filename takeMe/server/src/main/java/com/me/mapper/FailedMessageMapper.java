package com.me.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.me.entity.FailedMessage;
import org.apache.ibatis.annotations.*;
import com.me.dto.FailedMessageSummary;

import java.util.List;
import java.util.Map;

@Mapper
public interface FailedMessageMapper extends BaseMapper<FailedMessage> {
    // 同一事件在不同消费分支独立归档；重投后再次失败时重新开放人工处理。
    @Insert("""
            INSERT INTO mq_failed_message (id, message_id, source_queue, exchange_name, routing_key,
                body, headers_json, content_type, content_encoding, correlation_id, reason, last_error,
                occurrences, status, replay_count, create_time, last_failure_time)
            VALUES (#{id}, #{messageId}, #{sourceQueue}, #{exchangeName}, #{routingKey},
                #{body}, #{headersJson}, #{contentType}, #{contentEncoding}, #{correlationId}, #{reason},
                #{lastError}, 1, 0, 0, #{createTime}, #{lastFailureTime})
            ON DUPLICATE KEY UPDATE occurrences = occurrences + 1, status = 0,
                last_error = VALUES(last_error), reason = VALUES(reason), last_failure_time = VALUES(last_failure_time)
            """)
    int archive(FailedMessage message);

    // 显式投影分页摘要，原始消息体只在重放事务中读取。
    @Select("""
            SELECT id, message_id AS messageId, source_queue AS sourceQueue,
                exchange_name AS exchangeName, routing_key AS routingKey, reason, last_error AS lastError,
                occurrences, status, replay_count AS replayCount, last_replay_error AS lastReplayError,
                replayed_by AS replayedBy, create_time AS createTime,
                last_failure_time AS lastFailureTime, last_replay_time AS lastReplayTime
            FROM mq_failed_message
            WHERE (#{status} IS NULL OR status = #{status})
            ORDER BY last_failure_time DESC LIMIT #{offset}, #{limit}
            """)
    List<FailedMessageSummary> selectPage(@Param("offset") long offset,
                                           @Param("limit") long limit,
                                           @Param("status") Integer status);

    // 统计与分页使用同一筛选条件。
    @Select("SELECT COUNT(*) FROM mq_failed_message WHERE (#{status} IS NULL OR status = #{status})")
    long count(@Param("status") Integer status);

    // 行锁覆盖重放和结果保存，防止两个管理员同时重复投递同一条记录。
    @Select("SELECT * FROM mq_failed_message WHERE id = #{id} FOR UPDATE")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    FailedMessage selectForReplay(@Param("id") String id);

    // 重放失败也保存尝试记录，成功状态只表示 Broker 确认收到。
    @Update("""
            UPDATE mq_failed_message SET status = #{status}, replay_count = replay_count + 1,
                last_replay_error = #{lastReplayError}, replayed_by = #{replayedBy},
                last_replay_time = #{lastReplayTime} WHERE id = #{id}
            """)
    int recordReplay(FailedMessage message);

    // 每分钟按来源和失败类型聚合，避免异常风暴逐条刷屏。
    @Select("""
            SELECT source_queue AS sourceQueue, reason, COUNT(*) AS messageCount
            FROM mq_failed_message WHERE last_failure_time >= DATE_SUB(NOW(), INTERVAL 1 MINUTE)
            GROUP BY source_queue, reason
            """)
    List<Map<String, Object>> recentFailures();
}
