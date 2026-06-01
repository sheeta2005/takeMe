package com.me.service;

import com.me.dto.PageQueryDTO;
import com.me.vo.MessageVO;
import com.me.vo.PageResultVO;

public interface MessageService {

    /**
     * 分页查询用户消息列表
     */
    PageResultVO<MessageVO> list(Long receiverId, Integer receiverType, Integer type, Integer isRead, PageQueryDTO pageQueryDTO);

    /**
     * 获取消息详情
     */
    MessageVO getById(Long id);

    /**
     * 标记消息为已读
     */
    void markRead(Long id);
}