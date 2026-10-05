package com.me.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.me.dto.MessageDTO;
import com.me.dto.PageResultDTO;
import com.me.entity.Message;
import com.me.vo.MessageVO;
import java.util.List;

public interface MessageService extends IService<Message> {
    IPage<MessageVO> list(Long receiverId, Integer receiverType, Integer type, Integer isRead, PageResultDTO pageResultDTO);

    MessageVO convertToVO(Message message);

    boolean markAsRead(Long messageId, Integer receiverType, Long receiverId);

    boolean markAllAsRead(Integer receiverType, Long receiverId);

    int getUnreadCount(Integer receiverType, Long receiverId);

    void sendMessage(Message message);

    // 消费事件通知，数据库提交成功后监听器才能结束处理。
    void sendEventMessage(Message message, String eventId);

    IPage<Message> getAdminMessagePage(Integer receiverType, Integer type, PageResultDTO pageResultDTO);

    IPage<Message> getSentMessagePage(Integer receiverType, Integer type, PageResultDTO pageResultDTO);

    void sendBatchMessage(List<MessageDTO> messages);

    boolean deleteMessage(Long messageId);

    java.util.Map<String, Object> getMessageStatistics();

}
