package com.me.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.me.dto.MessageDTO;
import com.me.dto.PageResultDTO;
import com.me.entity.Message;
import com.me.vo.MessageVO;
import java.util.List;

public interface MessageService extends IService<Message> {
    //分页查询接收消息
    IPage<MessageVO> list(Long receiverId, Integer receiverType, Integer type, Integer isRead, PageResultDTO pageResultDTO);

    //转换消息展示信息
    MessageVO convertToVO(Message message);

    //标记消息已读
    boolean markAsRead(Long messageId, Integer receiverType, Long receiverId);

    //标记全部消息已读
    boolean markAllAsRead(Integer receiverType, Long receiverId);

    //统计未读消息
    int getUnreadCount(Integer receiverType, Long receiverId);

    //发送单条或群发消息
    void sendMessage(Message message);

    // 消费事件通知，数据库提交成功后监听器才能结束处理。
    void sendEventMessage(Message message, String eventId);

    //分页查询管理端消息
    IPage<Message> getAdminMessagePage(Integer receiverType, Integer type, PageResultDTO pageResultDTO);

    //分页查询发送记录
    IPage<Message> getSentMessagePage(Integer receiverType, Integer type, PageResultDTO pageResultDTO);

    //批量发送消息
    void sendBatchMessage(List<MessageDTO> messages);

    //删除消息
    boolean deleteMessage(Long messageId);

    //统计消息数量与类型
    java.util.Map<String, Object> getMessageStatistics();

}
