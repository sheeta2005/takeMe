package com.me.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.me.entity.OutboxMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@Mapper
public interface OutboxMapper extends BaseMapper<OutboxMessage> {
    //查询到期待发送消息编号
    @Select("SELECT id FROM mq_outbox WHERE status = 0 AND next_attempt_time <= NOW() ORDER BY create_time, id LIMIT 20")
    List<String> findDueIds();

    // 多实例发送任务跳过其他实例已经持有的消息行。
    @Select("SELECT * FROM mq_outbox WHERE id = #{id} AND status = 0 AND next_attempt_time <= NOW() FOR UPDATE SKIP LOCKED")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    OutboxMessage selectDueForUpdate(@Param("id") String id);

    // 只清理确认后已满保留期的记录，待发送消息始终由投递任务承接。
    @Delete("DELETE FROM mq_outbox WHERE status = 1 AND update_time < #{cutoff} ORDER BY update_time, id LIMIT #{limit}")
    int deleteConfirmedBefore(@Param("cutoff") LocalDateTime cutoff, @Param("limit") int limit);

    // 当前积压与超时积压一起统计，避免只观察最近一分钟的新增失败。
    @Select("SELECT COUNT(*) AS messageCount, COUNT(CASE WHEN create_time < #{cutoff} THEN 1 END) AS overdueCount " +
            "FROM mq_outbox WHERE status = 0")
    Map<String, Object> pendingSummary(@Param("cutoff") LocalDateTime cutoff);
}
