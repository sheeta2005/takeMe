package com.me.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.context.BaseContext;
import com.me.dto.FailedMessageSummary;
import com.me.dto.FailedMessageCloseDTO;
import com.me.result.Result;
import com.me.service.FailedMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/mq-failures")
@RequiredArgsConstructor
public class AdminFailedMessageController {
    private final FailedMessageService failedMessageService;

    @GetMapping
    public Result<Page<FailedMessageSummary>> page(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "20") long pageSize,
                                             @RequestParam(required = false) Integer status) {
        return Result.success(failedMessageService.page(pageNum, pageSize, status));
    }

    @GetMapping("/recent-counts")
    public Result<java.util.List<Map<String, Object>>> recentCounts() {
        return Result.success(failedMessageService.recentFailures());
    }

    @PostMapping("/{id}/replay")
    public Result<Void> replay(@PathVariable String id) {
        if (!failedMessageService.replay(id, BaseContext.getLoginId())) {
            return Result.error("重放未完成，请检查失败原因或来源队列");
        }
        return Result.success();
    }

    // 结案复用管理员鉴权，保留操作者及原因供保留期内核查。
    @PostMapping("/{id}/close")
    public Result<Void> close(@PathVariable String id, @RequestBody FailedMessageCloseDTO request) {
        if (!failedMessageService.close(id, BaseContext.getLoginId(), request.getReason())) {
            return Result.error("记录不存在或已经结案");
        }
        return Result.success();
    }
}
