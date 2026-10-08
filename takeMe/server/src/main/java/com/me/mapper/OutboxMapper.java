package com.me.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.me.entity.OutboxMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OutboxMapper extends BaseMapper<OutboxMessage> {
    //查询到期待发送消息编号
    @Select("SELECT id FROM mq_outbox WHERE status = 0 AND next_attempt_time <= NOW() ORDER BY create_time, id LIMIT 20")
    List<String> findDueIds();

    // 多实例发送任务跳过其他实例已经持有的消息行。
    @Select("SELECT * FROM mq_outbox WHERE id = #{id} AND status = 0 AND next_attempt_time <= NOW() FOR UPDATE SKIP LOCKED")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    OutboxMessage selectDueForUpdate(@Param("id") String id);
}
