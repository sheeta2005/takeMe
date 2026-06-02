package com.me.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.entity.Approval;
import com.me.result.Result;
import com.me.service.ApprovalService;
import com.me.vo.PageResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/approval")
@RequiredArgsConstructor
public class AdminApprovalController {

    private final ApprovalService approvalService;

    @GetMapping("/page")
    public Result<PageResultVO<Approval>> getApprovalPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        Page<Approval> mpPage = approvalService.getApprovalPage(
            page, pageSize, type, status, keyword, startDate, endDate
        );
        
        PageResultVO<Approval> result = new PageResultVO<>(mpPage.getTotal(), mpPage.getRecords());
        return Result.success(result);
    }

    @GetMapping("/detail/{id}")
    public Result<Approval> getApprovalDetail(@PathVariable Long id) {
        Approval approval = approvalService.getApprovalDetail(id);
        if (approval == null) {
            return Result.error("申请不存在");
        }
        return Result.success(approval);
    }

    @PostMapping("/approve/{id}")
    public Result<Void> approveApplication(
            @PathVariable Long id,
            @RequestBody(required = false) Approval updateData
    ) {
        String remark = updateData != null ? updateData.getRemark() : null;
        boolean success = approvalService.approveApplication(id, remark);
        
        if (!success) {
            return Result.error("操作失败，该申请可能已被处理");
        }
        return Result.success();
    }

    @PostMapping("/reject/{id}")
    public Result<Void> rejectApplication(
            @PathVariable Long id,
            @RequestBody Approval updateData
    ) {
        if (updateData == null || updateData.getRemark() == null || updateData.getRemark().trim().isEmpty()) {
            return Result.error("驳回时必须填写审批意见");
        }
        
        boolean success = approvalService.rejectApplication(id, updateData.getRemark());
        
        if (!success) {
            return Result.error("操作失败，该申请可能已被处理");
        }
        return Result.success();
    }
}
