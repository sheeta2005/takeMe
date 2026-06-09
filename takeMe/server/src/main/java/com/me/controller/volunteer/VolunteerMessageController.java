package com.me.controller.volunteer;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.me.context.BaseContext;
import com.me.dto.PageResultDTO;
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
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer isRead) {

        Long receiverId = BaseContext.getLoginId();
        Integer receiverType = BaseContext.getLoginType();

        PageResultDTO pageResultDTO = new PageResultDTO();
        pageResultDTO.setPageNum(pageNum);
        pageResultDTO.setPageSize(pageSize);

        IPage<MessageVO> iPage = messageService.list(receiverId, receiverType, type, isRead, pageResultDTO);
        PageResultVO<MessageVO> result = PageResultVO.from(iPage);
        return Result.success(result);
    }

    @PostMapping("/read/{id}")
    public Result<Void> markAsRead(@PathVariable Long id) {
        Long receiverId = BaseContext.getLoginId();
        boolean success = messageService.markAsRead(id, receiverId);
        if (!success) {
            return Result.error("标记已读失败");
        }
        return Result.success();
    }

    @PostMapping("/read-all")
    public Result<Void> markAllAsRead() {
        Long receiverId = BaseContext.getLoginId();
        Integer receiverType = BaseContext.getLoginType();
        boolean success = messageService.markAllAsRead(receiverType, receiverId);
        if (!success) {
            return Result.error("全部标记已读失败");
        }
        return Result.success();
    }

    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount() {
        Long receiverId = BaseContext.getLoginId();
        Integer receiverType = BaseContext.getLoginType();
        int count = messageService.getUnreadCount(receiverType, receiverId);
        return Result.success(count);
    }
}
