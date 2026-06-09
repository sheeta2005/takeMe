package com.me.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.me.dto.PageResultDTO;
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
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        PageResultDTO pageResultDTO = new PageResultDTO();
        pageResultDTO.setPageNum(pageNum);
        pageResultDTO.setPageSize(pageSize);
        
        IPage<Approval> iPage = approvalService.getApprovalPage(
            type, status, keyword, startDate, endDate, pageResultDTO
        );
        
        PageResultVO<Approval> result = PageResultVO.from(iPage);
        return Result.success(result);
    }

    @GetMapping("/detail/{id}")
    public Result<Approval> getApprovalDetail(@PathVariable Long id) {
        Approval approval = approvalService.getApprovalDetail(id);
        if (approval == null) {
            return Result.error("审批记录不存在");
        }
        return Result.success(approval);
    }

    @PostMapping("/approve/{id}")
    public Result<Void> approveApplication(@PathVariable Long id, @RequestParam(required = false) String remark) {
        boolean success = approvalService.approveApplication(id, remark);
        if (!success) {
            return Result.error("审批失败");
        }
        return Result.success();
    }

    @PostMapping("/reject/{id}")
    public Result<Void> rejectApplication(@PathVariable Long id, @RequestParam String remark) {
        boolean success = approvalService.rejectApplication(id, remark);
        if (!success) {
            return Result.error("审批失败");
        }
        return Result.success();
    }
}
