package com.me.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.me.dto.MessageDTO;
import com.me.dto.PageResultDTO;
import com.me.entity.Message;
import com.me.result.Result;
import com.me.service.MessageService;
import com.me.vo.PageResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "管理员-消息管理", description = "消息查询、发送、删除等操作")
@RestController
@RequestMapping("/api/admin/message")
@RequiredArgsConstructor
public class AdminMessageController {

    private final MessageService messageService;

    @Operation(summary = "分页查询消息", description = "查询所有消息列表，支持按接收者类型和消息类型筛选")
    @GetMapping("/page")
    public Result<PageResultVO<Message>> getMessagePage(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "接收者类型（0-管理员 1-志愿者 2-用户）") @RequestParam(required = false) Integer receiverType,
            @Parameter(description = "消息类型（0-系统通知 1-任务通知 2-温馨提醒）") @RequestParam(required = false) Integer type
    ) {
        PageResultDTO pageResultDTO = new PageResultDTO();
        pageResultDTO.setPageNum(pageNum);
        pageResultDTO.setPageSize(pageSize);
        
        IPage<Message> iPage = messageService.getAdminMessagePage(receiverType, type, pageResultDTO);
        PageResultVO<Message> result = PageResultVO.from(iPage);
        return Result.success(result);
    }

    @Operation(summary = "查询发送的消息", description = "查询管理员发送的消息列表")
    @GetMapping("/sent")
    public Result<PageResultVO<Message>> getSentMessagePage(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "接收者类型（0-管理员 1-志愿者 2-用户）") @RequestParam(required = false) Integer receiverType,
            @Parameter(description = "消息类型（0-系统通知 1-任务通知 2-温馨提醒）") @RequestParam(required = false) Integer type
    ) {
        PageResultDTO pageResultDTO = new PageResultDTO();
        pageResultDTO.setPageNum(pageNum);
        pageResultDTO.setPageSize(pageSize);
        
        IPage<Message> iPage = messageService.getSentMessagePage(receiverType, type, pageResultDTO);
        PageResultVO<Message> result = PageResultVO.from(iPage);
        return Result.success(result);
    }

    @Operation(summary = "发送单条消息", description = "向指定用户/志愿者发送消息")
    @PostMapping("/send")
    public Result<Void> sendMessage(@RequestBody MessageDTO messageDTO) {
        Message message = new Message();
        message.setReceiverId(messageDTO.getReceiverId());
        message.setReceiverType(messageDTO.getReceiverType());
        message.setType(messageDTO.getType());
        message.setTitle(messageDTO.getTitle());
        message.setContent(messageDTO.getContent());
        message.setRelatedOrderId(messageDTO.getRelatedOrderId());
        message.setRelatedUserId(messageDTO.getRelatedUserId());
        message.setRelatedVolunteerId(messageDTO.getRelatedVolunteerId());
        message.setRelatedUrl(messageDTO.getRelatedUrl());
        
        messageService.sendMessage(message);
        return Result.success();
    }

    @Operation(summary = "批量发送消息", description = "向多个用户/志愿者批量发送消息")
    @PostMapping("/sendBatch")
    public Result<Void> sendBatchMessage(@RequestBody List<MessageDTO> messages) {
        messageService.sendBatchMessage(messages);
        return Result.success();
    }

    @Operation(summary = "删除消息", description = "删除指定的消息")
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteMessage(@Parameter(description = "消息ID", required = true) @PathVariable Long id) {
        boolean success = messageService.deleteMessage(id);
        if (!success) {
            return Result.error("删除失败");
        }
        return Result.success();
    }

    @Operation(summary = "标记消息为已读", description = "将指定消息标记为已读状态")
    @PostMapping("/read/{id}")
    public Result<Void> markAsRead(@Parameter(description = "消息ID", required = true) @PathVariable Long id) {
        boolean success = messageService.markAsRead(id, 0L);
        if (!success) {
            return Result.error("标记已读失败");
        }
        return Result.success();
    }

    @Operation(summary = "全部标记已读", description = "将指定类型的所有消息标记为已读")
    @PostMapping("/read-all")
    public Result<Void> markAllAsRead(@Parameter(description = "接收者类型", required = true) @RequestParam Integer receiverType) {
        boolean success = messageService.markAllAsRead(receiverType, 0L);
        if (!success) {
            return Result.error("全部标记已读失败");
        }
        return Result.success();
    }

    @Operation(summary = "消息统计", description = "获取消息总数、未读数、已读数等统计信息")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getMessageStatistics() {
        Map<String, Object> stats = messageService.getMessageStatistics();
        return Result.success(stats);
    }
}
