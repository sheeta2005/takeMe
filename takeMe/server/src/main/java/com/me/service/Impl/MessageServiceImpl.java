package com.me.service.Impl;

import com.me.context.BaseContext;
import com.me.dto.PageQueryDTO;
import com.me.entity.Message;
import com.me.mapper.MessageMapper;
import com.me.service.MessageService;
import com.me.vo.MessageVO;
import com.me.vo.PageResultVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;

    public MessageServiceImpl(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public PageResultVO<MessageVO> list(Long receiverId, Integer receiverType, Integer type, Integer isRead, PageQueryDTO pageQueryDTO) {
        // 1. 构建MP分页对象（仅内部使用，不返回）
        Page<Message> page = new Page<>(pageQueryDTO.getPageNum(), pageQueryDTO.getPageSize());

        // 2. 构建查询条件
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, receiverId)
                .eq(Message::getReceiverType, receiverType);

        if (type != null) wrapper.eq(Message::getType, type);
        if (isRead != null) wrapper.eq(Message::getIsRead, isRead);

        wrapper.orderByDesc(Message::getCreateTime);

        // 3. MP分页查询
        messageMapper.selectPage(page, wrapper);

        // 4. 封装为【你的自定义分页类】PageResultVO（彻底解决类型报错！）
        List<MessageVO> records = page.getRecords().stream()
                .map(this::convertToVO)
                .toList();

        PageResultVO<MessageVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(page.getTotal());
        pageResult.setRecords(records);

        return pageResult;
    }

    @Override
    public MessageVO getById(Long id) {
        Message message = messageMapper.selectById(id);
        if (message == null) throw new RuntimeException("消息不存在");

        // 越权校验
        Long currentUserId = BaseContext.getLoginId();
        Integer currentUserType = BaseContext.getLoginType();
        if (!message.getReceiverId().equals(currentUserId) || !message.getReceiverType().equals(currentUserType)) {
            throw new RuntimeException("无权限访问");
        }

        return convertToVO(message);
    }

    @Override
    public void markRead(Long id) {
        Message message = messageMapper.selectById(id);
        if (message == null) throw new RuntimeException("消息不存在");

        // 越权校验
        Long currentUserId = BaseContext.getLoginId();
        Integer currentUserType = BaseContext.getLoginType();
        if (!message.getReceiverId().equals(currentUserId) || !message.getReceiverType().equals(currentUserType)) {
            throw new RuntimeException("无权限操作");
        }

        message.setIsRead(1);
        messageMapper.updateById(message);
    }

    // 实体转VO
    private MessageVO convertToVO(Message message) {
        MessageVO vo = new MessageVO();
        BeanUtils.copyProperties(message, vo);
        return vo;
    }
}