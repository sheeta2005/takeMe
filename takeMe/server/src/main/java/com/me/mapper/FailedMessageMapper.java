package com.me.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.me.entity.FailedMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.me.dto.FailedMessageSummary;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@Mapper
public interface FailedMessageMapper extends BaseMapper<FailedMessage> {
    // 同一事件在不同消费分支独立归档；重投后再次失败时重新开放人工处理。
    int archive(FailedMessage message);

    // 显式投影分页摘要，原始消息体只在重放事务中读取。
    List<FailedMessageSummary> selectPage(@Param("offset") long offset,
                                           @Param("limit") long limit,
                                           @Param("status") Integer status);

    // 统计与分页使用同一筛选条件。
    long count(@Param("status") Integer status);

    // 重放和结案共用行锁，避免两个管理员同时投递或覆盖处置状态。
    FailedMessage selectForReplay(@Param("id") String id);

    // 重放失败也保存尝试记录，成功状态只表示 Broker 确认收到。
    int recordReplay(FailedMessage message);

    // 只有人工核查后的结案记录可以进入保留期清理。
    int close(FailedMessage message);

    // 条件随删除语句一起执行；再次归档恢复为待核查后，不再符合清理条件。
    int deleteClosedBefore(@Param("cutoff") LocalDateTime cutoff, @Param("limit") int limit);

    // 已重投记录从最后一次重投计时，其余记录从最近一次失败计时。
    List<Map<String, Object>> outstandingSummary(@Param("cutoff") LocalDateTime cutoff);

    // 每分钟按来源和失败类型聚合，避免异常风暴逐条刷屏。
    List<Map<String, Object>> recentFailures();
}
