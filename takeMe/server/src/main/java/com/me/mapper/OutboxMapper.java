package com.me.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.me.entity.OutboxMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@Mapper
public interface OutboxMapper extends BaseMapper<OutboxMessage> {
    //查询到期待发送消息编号
    List<String> findDueIds();

    // 多实例发送任务跳过其他实例已经持有的消息行。
    OutboxMessage selectDueForUpdate(@Param("id") String id);

    // 只清理确认后已满保留期的记录，待发送消息始终由投递任务承接。
    int deleteConfirmedBefore(@Param("cutoff") LocalDateTime cutoff, @Param("limit") int limit);

    // 当前积压与超时积压一起统计，避免只观察最近一分钟的新增失败。
    Map<String, Object> pendingSummary(@Param("cutoff") LocalDateTime cutoff);

    // 发送成功明确清空历史错误，失败保留重试信息。
    int recordDispatch(OutboxMessage message);
}
