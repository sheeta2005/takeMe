package com.me.controller.volunteer;

import com.me.context.BaseContext;
import com.me.dto.PageQueryDTO;
import com.me.result.Result;
import com.me.service.MessageService;
import com.me.vo.MessageVO;
import com.me.vo.PageResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/volunteer/message")
@RequiredArgsConstructor
public class VolunteerMessageController {

    private final MessageService messageService;

    @GetMapping("/list")
    public Result<PageResultVO<MessageVO>> list(
            PageQueryDTO pageQueryDTO,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer isRead) {

        Long receiverId = BaseContext.getLoginId();
        Integer receiverType = BaseContext.getLoginType();

        PageResultVO<MessageVO> result = messageService.list(receiverId, receiverType, type, isRead, pageQueryDTO);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<MessageVO> getById(@PathVariable Long id) {
        MessageVO message = messageService.getById(id);
        return Result.success(message);
    }

    @PostMapping("/read/{id}")
    public Result<Void> markRead(@PathVariable Long id) {
        messageService.markRead(id);
        return Result.success();
    }
}
