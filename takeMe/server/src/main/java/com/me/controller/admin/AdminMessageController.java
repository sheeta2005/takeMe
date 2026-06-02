package com.me.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.entity.Message;
import com.me.result.Result;
import com.me.service.MessageService;
import com.me.vo.PageResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/message")
@RequiredArgsConstructor
public class AdminMessageController {

    private final MessageService messageService;

    @GetMapping("/page")
    public Result<PageResultVO<Message>> getMessagePage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer receiverType,
            @RequestParam(required = false) Integer type
    ) {
        Page<Message> mpPage = messageService.getAdminMessagePage(page, pageSize, receiverType, type);
        PageResultVO<Message> result = new PageResultVO<>(mpPage.getTotal(), mpPage.getRecords());
        return Result.success(result);
    }

    @GetMapping("/sent")
    public Result<PageResultVO<Message>> getSentMessagePage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer receiverType,
            @RequestParam(required = false) Integer type
    ) {
        Page<Message> mpPage = messageService.getAdminMessagePage(page, pageSize, receiverType, type);
        PageResultVO<Message> result = new PageResultVO<>(mpPage.getTotal(), mpPage.getRecords());
        return Result.success(result);
    }

    @PostMapping("/send")
    public Result<Void> sendMessage(@RequestBody Message message) {
        boolean success = messageService.sendMessage(message);
        if (!success) {
            return Result.error("发送失败");
        }
        return Result.success();
    }

    @PostMapping("/sendBatch")
    public Result<Void> sendBatchMessage(@RequestBody Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        Map<String, Object> messageMap = (Map<String, Object>) data.get("message");
        @SuppressWarnings("unchecked")
        List<Long> receiverIds = (List<Long>) data.get("receiverIds");
        
        if (receiverIds == null || receiverIds.isEmpty()) {
            return Result.error("接收者列表不能为空");
        }
        
        Message message = new Message();
        message.setReceiverType((Integer) messageMap.get("receiverType"));
        message.setType((Integer) messageMap.get("type"));
        message.setTitle((String) messageMap.get("title"));
        message.setContent((String) messageMap.get("content"));
        message.setRelatedOrderId(messageMap.get("relatedOrderId") != null ? ((Number) messageMap.get("relatedOrderId")).longValue() : null);
        message.setRelatedUserId(messageMap.get("relatedUserId") != null ? ((Number) messageMap.get("relatedUserId")).longValue() : null);
        message.setRelatedVolunteerId(messageMap.get("relatedVolunteerId") != null ? ((Number) messageMap.get("relatedVolunteerId")).longValue() : null);
        message.setRelatedUrl((String) messageMap.get("relatedUrl"));
        
        int count = messageService.sendBatchMessage(message, receiverIds);
        
        if (count == 0) {
            return Result.error("发送失败");
        }
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteMessage(@PathVariable Long id) {
        boolean success = messageService.deleteMessage(id);
        if (!success) {
            return Result.error("消息不存在");
        }
        return Result.success();
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getMessageStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        LambdaQueryWrapper<Message> allWrapper = new LambdaQueryWrapper<>();
        Long totalCount = messageService.countMessages(allWrapper);
        stats.put("totalCount", totalCount);
        
        LambdaQueryWrapper<Message> unreadWrapper = new LambdaQueryWrapper<>();
        unreadWrapper.eq(Message::getIsRead, 0);
        Long unreadCount = messageService.countMessages(unreadWrapper);
        stats.put("unreadCount", unreadCount);
        
        return Result.success(stats);
    }
}
