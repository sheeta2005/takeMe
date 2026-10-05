package com.me.mapper;

import com.me.entity.Message;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
    // 重投不覆盖原消息内容及已读状态，唯一约束负责并发去重。
    @Insert("""
            INSERT INTO message (event_id, receiver_id, receiver_type, type, title, content,
                is_read, related_order_id, related_user_id, related_volunteer_id, related_url, create_time)
            VALUES (#{eventId}, #{receiverId}, #{receiverType}, #{type}, #{title}, #{content},
                #{isRead}, #{relatedOrderId}, #{relatedUserId}, #{relatedVolunteerId}, #{relatedUrl}, #{createTime})
            ON DUPLICATE KEY UPDATE id = id
            """)
    int insertEventNotification(Message message);
}
