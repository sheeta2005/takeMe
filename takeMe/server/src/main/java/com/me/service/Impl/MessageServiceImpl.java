package com.me.service.Impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.me.dto.MessageDTO;
import com.me.dto.PageResultDTO;
import com.me.entity.Message;
import com.me.mapper.MessageMapper;
import com.me.service.MessageService;
import com.me.service.OutboxService;
import com.me.mq.config.RabbitMQConfig;
import com.me.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private final MessageMapper messageMapper;
    private final OutboxService outboxService;

    //分页查询接收消息
    @Override
    public IPage<MessageVO> list(Long receiverId, Integer receiverType, Integer type, Integer isRead, PageResultDTO pageResultDTO) {
        Page<Message> page = new Page<>(pageResultDTO.getPageNum(), pageResultDTO.getPageSize());

        Page<Message> resultPage = messageMapper.selectReceiverPage(page, receiverId, receiverType, type, isRead);
        
        List<MessageVO> voRecords = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        Page<MessageVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        voPage.setRecords(voRecords);
        return voPage;
    }

    //转换消息展示信息
    @Override
    public MessageVO convertToVO(Message message) {
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setTitle(message.getTitle());
        vo.setContent(message.getContent());
        vo.setType(message.getType());
        vo.setIsRead(message.getIsRead());
        vo.setCreateTime(message.getCreateTime());
        vo.setRelatedOrderId(message.getRelatedOrderId());
        return vo;
    }

    //标记消息已读
    @Override
    public boolean markAsRead(Long messageId, Integer receiverType, Long receiverId) {
        // 不同角色的数字 ID 可能相同，必须同时校验角色与账号。
        return messageMapper.markRead(messageId, receiverType, receiverId) > 0;
    }

    //标记全部消息已读
    @Override
    public boolean markAllAsRead(Integer receiverType, Long receiverId) {
        return messageMapper.markAllUnread(receiverType, receiverId) > 0;
    }

    //统计未读消息
    @Override
    public int getUnreadCount(Integer receiverType, Long receiverId) {
        return Math.toIntExact(messageMapper.countUnreadForReceiver(receiverType, receiverId));
    }

    //发送单条或群发消息
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendMessage(Message message) {
        if (message.getCreateTime() == null) {
            message.setCreateTime(LocalDateTime.now());
        }
        if (message.getIsRead() == null) {
            message.setIsRead(0);
        }
        
        if (message.getReceiverId() == null && message.getReceiverType() != null) {
            if (message.getReceiverType() != 1 && message.getReceiverType() != 2) {
                throw new IllegalArgumentException("群发接收者类型无效");
            }
            // 群发和订单通知复用可靠消息链路，事务提交后再分批写入。
            outboxService.enqueue(RabbitMQConfig.BROADCAST_EXCHANGE, "broadcast", message);
        } else {
            this.save(message);
        }
    }

    //按事件标识保存通知
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendEventMessage(Message message, String eventId) {
        if (eventId == null || eventId.isBlank()) {
            // 兼容已有队列中不含事件 ID 的消息，使用稳定的业务内容生成标识。
            String identity = message.getRelatedOrderId() + "|" + message.getTitle() + "|"
                    + message.getContent() + "|" + message.getCreateTime();
            eventId = "legacy:" + UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8));
        }
        message.setEventId(eventId);
        if (message.getCreateTime() == null) message.setCreateTime(LocalDateTime.now());
        if (message.getIsRead() == null) message.setIsRead(0);
        messageMapper.insertEventNotification(message);
    }

    //分页查询管理端消息
    @Override
    public IPage<Message> getAdminMessagePage(Integer receiverType, Integer type, PageResultDTO pageResultDTO) {
        Page<Message> page = new Page<>(pageResultDTO.getPageNum(), pageResultDTO.getPageSize());

        return messageMapper.selectAdminPage(page, receiverType, type);
    }

    //分页查询发送记录
    @Override
    public IPage<Message> getSentMessagePage(Integer receiverType, Integer type, PageResultDTO pageResultDTO) {
        Page<Message> page = new Page<>(pageResultDTO.getPageNum(), pageResultDTO.getPageSize());

        // 管理员发送的消息：receiverType=0 表示接收者是管理员
        return messageMapper.selectAdminPage(page, receiverType, type);
    }

    //批量发送消息
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendBatchMessage(List<MessageDTO> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        
        List<Message> entityList = messages.stream().map(dto -> {
            Message message = new Message();
            message.setReceiverId(dto.getReceiverId());
            message.setReceiverType(dto.getReceiverType());
            message.setType(dto.getType());
            message.setTitle(dto.getTitle());
            message.setContent(dto.getContent());
            message.setRelatedOrderId(dto.getRelatedOrderId());
            message.setRelatedUserId(dto.getRelatedUserId());
            message.setRelatedVolunteerId(dto.getRelatedVolunteerId());
            message.setRelatedUrl(dto.getRelatedUrl());
            message.setIsRead(0);
            message.setCreateTime(LocalDateTime.now());
            return message;
        }).collect(Collectors.toList());
        
        entityList.forEach(this::sendMessage);
    }

    //删除消息
    @Override
    public boolean deleteMessage(Long messageId) {
        return this.removeById(messageId);
    }

    //统计消息数量与类型
    @Override
    public Map<String, Object> getMessageStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总消息数
        stats.put("totalCount", messageMapper.countByReadAndType(null, null));
        
        // 未读消息数
        stats.put("unreadCount", messageMapper.countByReadAndType(0, null));
        
        // 已读消息数
        stats.put("readCount", messageMapper.countByReadAndType(1, null));
        
        // 按类型统计
        for (int i = 0; i <= 2; i++) {
            stats.put("type" + i + "Count", messageMapper.countByReadAndType(null, i));
        }
        
        return stats;
    }

}
