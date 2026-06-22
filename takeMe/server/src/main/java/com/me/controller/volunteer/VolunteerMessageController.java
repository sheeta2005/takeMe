package com.me.controller.volunteer;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.me.context.BaseContext;
import com.me.dto.PageResultDTO;
import com.me.result.Result;
import com.me.service.MessageService;
import com.me.vo.MessageVO;
import com.me.vo.PageResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "志愿者-信息管理")
@RestController
@RequestMapping("/api/volunteer/message")
@RequiredArgsConstructor
public class VolunteerMessageController {

    private final MessageService messageService;

    @Operation(summary = "查列表")
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

    @Operation(summary = "已读")
    @PostMapping("/read/{id}")
    public Result<Void> markAsRead(@PathVariable Long id) {
        Long receiverId = BaseContext.getLoginId();
        boolean success = messageService.markAsRead(id, receiverId);
        if (!success) {
            return Result.error("标记已读失败");
        }
        return Result.success();
    }

    @Operation(summary = "全部已读")
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

    @Operation(summary = "查未读数量")
    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount() {
        Long receiverId = BaseContext.getLoginId();
        Integer receiverType = BaseContext.getLoginType();
        int count = messageService.getUnreadCount(receiverType, receiverId);
        return Result.success(count);
    }
}
