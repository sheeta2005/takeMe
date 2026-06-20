package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.me.dto.MessageDTO;
import com.me.dto.PageResultDTO;
import com.me.entity.Message;
import com.me.mapper.MessageMapper;
import com.me.redis.utils.RedisUtil;
import com.me.service.MessageService;
import com.me.service.UserService;
import com.me.service.VolunteerService;
import com.me.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private final MessageMapper messageMapper;
    private final UserService userService;
    private final VolunteerService volunteerService;
    private final RedisUtil redisUtil;

    private static final String TASK_PROGRESS_PREFIX = "message:task:";
    private static final int BATCH_SIZE = 500;

    @Override
    public IPage<MessageVO> list(Long receiverId, Integer receiverType, Integer type, Integer isRead, PageResultDTO pageResultDTO) {
        Page<Message> page = new Page<>(pageResultDTO.getPageNum(), pageResultDTO.getPageSize());

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, receiverId)
                .eq(Message::getReceiverType, receiverType);

        if (type != null) wrapper.eq(Message::getType, type);
        if (isRead != null) wrapper.eq(Message::getIsRead, isRead);
        wrapper.orderByDesc(Message::getCreateTime);

        Page<Message> resultPage = messageMapper.selectPage(page, wrapper);
        
        List<MessageVO> voRecords = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        Page<MessageVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        voPage.setRecords(voRecords);
        return voPage;
    }

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

    @Override
    public boolean markAsRead(Long messageId, Long receiverId) {
        Message message = this.getById(messageId);
        if (message == null || !message.getReceiverId().equals(receiverId)) {
            return false;
        }
        message.setIsRead(1);
        return this.updateById(message);
    }

    @Override
    public boolean markAllAsRead(Integer receiverType, Long receiverId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverType, receiverType)
               .eq(Message::getReceiverId, receiverId)
               .eq(Message::getIsRead, 0);
        
        Message updateMessage = new Message();
        updateMessage.setIsRead(1);
        
        return this.update(updateMessage, wrapper);
    }

    @Override
    public int getUnreadCount(Integer receiverType, Long receiverId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverType, receiverType)
               .eq(Message::getReceiverId, receiverId)
               .eq(Message::getIsRead, 0);
        
        return Math.toIntExact(this.count(wrapper));
    }

    @Override
    public void sendMessage(Message message) {
        if (message.getCreateTime() == null) {
            message.setCreateTime(LocalDateTime.now());
        }
        if (message.getIsRead() == null) {
            message.setIsRead(0);
        }
        
        if (message.getReceiverId() == null && message.getReceiverType() != null) {
            sendToAllUsersAsync(message.getReceiverType(), message);
        } else {
            this.save(message);
        }
    }

    @Override
    public IPage<Message> getAdminMessagePage(Integer receiverType, Integer type, PageResultDTO pageResultDTO) {
        Page<Message> page = new Page<>(pageResultDTO.getPageNum(), pageResultDTO.getPageSize());

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        if (receiverType != null) {
            wrapper.eq(Message::getReceiverType, receiverType);
        }
        if (type != null) {
            wrapper.eq(Message::getType, type);
        }
        wrapper.orderByDesc(Message::getCreateTime);

        return messageMapper.selectPage(page, wrapper);
    }

    @Override
    public IPage<Message> getSentMessagePage(Integer receiverType, Integer type, PageResultDTO pageResultDTO) {
        Page<Message> page = new Page<>(pageResultDTO.getPageNum(), pageResultDTO.getPageSize());

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        // 管理员发送的消息：receiverType=0 表示接收者是管理员
        if (receiverType != null) {
            wrapper.eq(Message::getReceiverType, receiverType);
        }
        if (type != null) {
            wrapper.eq(Message::getType, type);
        }
        wrapper.orderByDesc(Message::getCreateTime);

        return messageMapper.selectPage(page, wrapper);
    }

    @Override
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
        
        this.saveBatch(entityList);
    }

    @Override
    public boolean deleteMessage(Long messageId) {
        return this.removeById(messageId);
    }

    @Override
    public Map<String, Object> getMessageStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总消息数
        stats.put("totalCount", this.count());
        
        // 未读消息数
        LambdaQueryWrapper<Message> unreadWrapper = new LambdaQueryWrapper<>();
        unreadWrapper.eq(Message::getIsRead, 0);
        stats.put("unreadCount", this.count(unreadWrapper));
        
        // 已读消息数
        LambdaQueryWrapper<Message> readWrapper = new LambdaQueryWrapper<>();
        readWrapper.eq(Message::getIsRead, 1);
        stats.put("readCount", this.count(readWrapper));
        
        // 按类型统计
        for (int i = 0; i <= 2; i++) {
            LambdaQueryWrapper<Message> typeWrapper = new LambdaQueryWrapper<>();
            typeWrapper.eq(Message::getType, i);
            stats.put("type" + i + "Count", this.count(typeWrapper));
        }
        
        return stats;
    }

    @Override
    @Async("messageTaskExecutor")
    public void sendToAllUsersAsync(Integer receiverType, Message template) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        String taskKey = TASK_PROGRESS_PREFIX + taskId;
        
        try {
            log.info("开始异步群发消息任务: {}, receiverType: {}", taskId, receiverType);
            
            int pageNum = 1;
            int totalProcessed = 0;
            int totalPages = 0;
            
            while (true) {
                List<Long> userIds;
                if (receiverType == 1) {
                    userIds = volunteerService.getAllVolunteerIds(pageNum, BATCH_SIZE);
                } else if (receiverType == 2) {
                    userIds = userService.getAllUserIds(pageNum, BATCH_SIZE);
                } else {
                    log.warn("不支持的receiverType: {}", receiverType);
                    break;
                }
                
                if (userIds == null || userIds.isEmpty()) {
                    break;
                }
                
                totalPages = pageNum;
                
                List<Message> messages = userIds.stream().map(userId -> {
                    Message msg = new Message();
                    msg.setReceiverId(userId);
                    msg.setReceiverType(receiverType);
                    msg.setType(template.getType());
                    msg.setTitle(template.getTitle());
                    msg.setContent(template.getContent());
                    msg.setRelatedOrderId(template.getRelatedOrderId());
                    msg.setRelatedUserId(template.getRelatedUserId());
                    msg.setRelatedVolunteerId(template.getRelatedVolunteerId());
                    msg.setRelatedUrl(template.getRelatedUrl());
                    msg.setIsRead(0);
                    msg.setCreateTime(LocalDateTime.now());
                    return msg;
                }).collect(Collectors.toList());
                
                this.saveBatch(messages, BATCH_SIZE);
                
                totalProcessed += messages.size();
                
                redisUtil.hSet(taskKey, "processed", totalProcessed);
                
                log.info("任务 {} 进度: 第{}批, 本批{}条, 累计{}条", 
                        taskId, pageNum, messages.size(), totalProcessed);
                
                pageNum++;
                
                if (userIds.size() < BATCH_SIZE) {
                    break;
                }
            }
            
            redisUtil.hSet(taskKey, "total", totalProcessed);
            redisUtil.hSet(taskKey, "status", "SUCCESS");
            redisUtil.expire(taskKey, 24, TimeUnit.HOURS);
            
            log.info("消息群发任务完成: {}, 总计{}条", taskId, totalProcessed);
            
        } catch (Exception e) {
            log.error("消息群发任务失败: {}", taskId, e);
            redisUtil.hSet(taskKey, "status", "FAILED");
            redisUtil.hSet(taskKey, "error", e.getMessage());
            redisUtil.expire(taskKey, 24, TimeUnit.HOURS);
        }
    }
}