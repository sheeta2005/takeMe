package com.me.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.me.dto.PageResultDTO;
import com.me.entity.Message;
import com.me.vo.MessageVO;

public interface MessageService extends IService<Message> {
    IPage<MessageVO> list(Long receiverId, Integer receiverType, Integer type, Integer isRead, PageResultDTO pageResultDTO);

    MessageVO convertToVO(Message message);

    boolean markAsRead(Long messageId, Long receiverId);

    boolean markAllAsRead(Integer receiverType, Long receiverId);

    int getUnreadCount(Integer receiverType, Long receiverId);

    void sendMessage(Message message);

    IPage<Message> getAdminMessagePage(Integer receiverType, Integer type, PageResultDTO pageResultDTO);
}