package com.me.mapper;

import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.entity.Message;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
    // 重投不覆盖原消息内容及已读状态，唯一约束负责并发去重。
    int insertEventNotification(Message message);

    // 按接收角色及账号查询消息，防止角色间数字 ID 混用。
    default Page<Message> selectReceiverPage(Page<Message> page, Long receiverId, Integer receiverType, Integer type, Integer isRead) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, receiverId)
                .eq(Message::getReceiverType, receiverType);

        if (type != null) wrapper.eq(Message::getType, type);
        if (isRead != null) wrapper.eq(Message::getIsRead, isRead);
        wrapper.orderByDesc(Message::getCreateTime);

        return selectPage(page, wrapper);
    }

    // 标记单条已读时同时校验角色和账号归属。
    default int markRead(Long messageId, Integer receiverType, Long receiverId) {
        return update(null, new LambdaUpdateWrapper<Message>().eq(Message::getId, messageId)
                .eq(Message::getReceiverType, receiverType).eq(Message::getReceiverId, receiverId).set(Message::getIsRead, 1));
    }

    // 管理端消息按接收角色和类型分页。
    default Page<Message> selectAdminPage(Page<Message> page, Integer receiverType, Integer type) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        if (receiverType != null) {
            wrapper.eq(Message::getReceiverType, receiverType);
        }
        if (type != null) {
            wrapper.eq(Message::getType, type);
        }
        wrapper.orderByDesc(Message::getCreateTime);

        return selectPage(page, wrapper);
    }

    // 批量已读只更新当前账号仍未读的消息。
    int markAllUnread(@Param("receiverType") Integer receiverType, @Param("receiverId") Long receiverId);

    // 未读统计同时限定接收角色与账号。
    Long countUnreadForReceiver(@Param("receiverType") Integer receiverType, @Param("receiverId") Long receiverId);

    // 保留消息总量、已读状态和类型统计口径。
    Long countByReadAndType(@Param("isRead") Integer isRead, @Param("type") Integer type);
}
