package com.me.controller.user;

import com.me.context.BaseContext;
import com.me.dto.PageQueryDTO;
import com.me.result.Result;
import com.me.service.MessageService;
import com.me.vo.MessageVO;
import com.me.vo.PageResultVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/message")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * 分页获取当前用户的消息列表（支持类型、已读状态筛选）
     */
    @GetMapping("/list")
    public Result<PageResultVO<MessageVO>> list(
            PageQueryDTO pageQueryDTO,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer isRead) {

        // 获取当前登录用户信息
        Long receiverId = BaseContext.getLoginId();
        Integer receiverType = BaseContext.getLoginType();

        PageResultVO<MessageVO> result = messageService.list(receiverId, receiverType, type, isRead, pageQueryDTO);
        return Result.success(result);
    }

    /**
     * 获取单条消息详情
     */
    @GetMapping("/{id}")
    public Result<MessageVO> getById(@PathVariable Long id) {
        MessageVO message = messageService.getById(id);
        return Result.success(message);
    }

    /**
     * 标记消息为已读
     */
    @PostMapping("/read/{id}")
    public Result<Void> markRead(@PathVariable Long id) {
        messageService.markRead(id);
        return Result.success();
    }
}