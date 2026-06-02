package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.me.entity.Approval;
import com.me.mapper.ApprovalMapper;
import com.me.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl extends ServiceImpl<ApprovalMapper, Approval> implements ApprovalService {

    @Override
    public Page<Approval> getApprovalPage(
            Integer page,
            Integer pageSize,
            String type,
            String status,
            String keyword,
            String startDate,
            String endDate
    ) {
        Page<Approval> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Approval> wrapper = new LambdaQueryWrapper<>();

        if (type != null && !type.trim().isEmpty()) {
            wrapper.eq(Approval::getType, type);
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(Approval::getStatus, status);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(Approval::getApplicantName, keyword)
                    .or()
                    .like(Approval::getApplicantId, keyword));
        }
        if (startDate != null && !startDate.trim().isEmpty()) {
            wrapper.ge(Approval::getCreateTime, startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            wrapper.le(Approval::getCreateTime, endDate + " 23:59:59");
        }

        wrapper.orderByDesc(Approval::getCreateTime);
        return this.page(pageParam, wrapper);
    }

    @Override
    public Approval getApprovalDetail(Long id) {
        return this.getById(id);
    }

    @Override
    public boolean approveApplication(Long id, String remark) {
        Approval approval = this.getById(id);
        if (approval == null) {
            return false;
        }

        if (!"pending".equals(approval.getStatus())) {
            return false;
        }

        approval.setStatus("approved");
        approval.setRemark(remark);
        approval.setUpdateTime(LocalDateTime.now());
        return this.updateById(approval);
    }

    @Override
    public boolean rejectApplication(Long id, String remark) {
        Approval approval = this.getById(id);
        if (approval == null) {
            return false;
        }

        if (!"pending".equals(approval.getStatus())) {
            return false;
        }

        approval.setStatus("rejected");
        approval.setRemark(remark);
        approval.setUpdateTime(LocalDateTime.now());
        return this.updateById(approval);
    }
}
