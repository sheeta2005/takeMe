package com.me.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.me.dto.PageResultDTO;
import com.me.entity.Message;
import com.me.result.Result;
import com.me.service.MessageService;
import com.me.vo.PageResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/message")
@RequiredArgsConstructor
public class AdminMessageController {

    private final MessageService messageService;

    @GetMapping("/page")
    public Result<PageResultVO<Message>> getMessagePage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer receiverType,
            @RequestParam(required = false) Integer type
    ) {
        PageResultDTO pageResultDTO = new PageResultDTO();
        pageResultDTO.setPageNum(pageNum);
        pageResultDTO.setPageSize(pageSize);
        
        IPage<Message> iPage = messageService.getAdminMessagePage(receiverType, type, pageResultDTO);
        PageResultVO<Message> result = PageResultVO.from(iPage);
        return Result.success(result);
    }

    @PostMapping("/read/{id}")
    public Result<Void> markAsRead(@PathVariable Long id) {
        boolean success = messageService.markAsRead(id, 0L);
        if (!success) {
            return Result.error("标记已读失败");
        }
        return Result.success();
    }

    @PostMapping("/read-all")
    public Result<Void> markAllAsRead(@RequestParam Integer receiverType) {
        boolean success = messageService.markAllAsRead(receiverType, 0L);
        if (!success) {
            return Result.error("全部标记已读失败");
        }
        return Result.success();
    }
}
