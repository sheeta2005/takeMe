package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.me.dto.ApprovalResultMessage;
import com.me.dto.PageResultDTO;
import com.me.entity.Approval;
import com.me.entity.VolunteerLeave;
import com.me.entity.Volunteer;
import com.me.mapper.ApprovalMapper;
import com.me.mapper.VolunteerLeaveMapper;
import com.me.mq.config.RabbitMQConfig;
import com.me.mapper.VolunteerMapper;
import com.me.service.OutboxService;
import com.me.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl extends ServiceImpl<ApprovalMapper, Approval> implements ApprovalService {

    private final VolunteerLeaveMapper volunteerLeaveMapper;
    private final VolunteerMapper volunteerMapper;
    private final OutboxService outboxService;

    //分页查询审批记录
    @Override
    public IPage<Approval> getApprovalPage(
            String type,
            String status,
            String keyword,
            String startDate,
            String endDate,
            PageResultDTO pageResultDTO
    ) {
        Page<Approval> page = new Page<>(pageResultDTO.getPageNum(), pageResultDTO.getPageSize());

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
        return this.page(page, wrapper);
    }

    //查询审批详情
    @Override
    public Approval getApprovalDetail(Long id) {
        return this.getById(id);
    }

    //通过审批申请
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approveApplication(Long id, String remark) {
        return decide(id, "approved", remark);
    }

    //拒绝审批申请
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rejectApplication(Long id, String remark) {
        return decide(id, "rejected", remark);
    }

    // 同一审批只允许从待审状态迁移一次，业务更新与消息登记一并回滚。
    private boolean decide(Long id, String result, String remark) {
        Approval approval = baseMapper.selectForUpdate(id);
        if (approval == null || !"pending".equals(approval.getStatus())) return false;
        boolean approved = "approved".equals(result);
        switch (approval.getType()) {
            case "leave" -> {
                if (approval.getBusinessId() == null) {
                    throw new IllegalStateException("历史请假审批尚未关联具体请假记录");
                }
                VolunteerLeave leave = volunteerLeaveMapper.selectForUpdate(approval.getBusinessId());
                if (leave == null || !approval.getApplicantId().equals(leave.getVolunteerId())
                        || leave.getStatus() == null || leave.getStatus() != 0) {
                    throw new IllegalStateException("请假记录不存在或已审批");
                }
                LambdaUpdateWrapper<VolunteerLeave> update = new LambdaUpdateWrapper<>();
                update.eq(VolunteerLeave::getId, leave.getId()).eq(VolunteerLeave::getStatus, 0)
                        .set(VolunteerLeave::getStatus, approved ? 1 : 2);
                if (volunteerLeaveMapper.update(null, update) != 1) {
                    throw new IllegalStateException("请假状态更新失败");
                }
            }
            case "register", "service_days_change" -> {
                if (approved) {
                    Volunteer volunteer = volunteerMapper.selectForUpdate(approval.getApplicantId());
                    if (volunteer == null) throw new IllegalStateException("志愿者不存在");
                    LambdaUpdateWrapper<Volunteer> update = new LambdaUpdateWrapper<>();
                    update.eq(Volunteer::getId, volunteer.getId());
                    if ("register".equals(approval.getType())) {
                        update.set(Volunteer::getStatus, 1);
                    } else {
                        if (approval.getContent() == null || !approval.getContent().matches("[0-6](,[0-6]){0,2}")) {
                            throw new IllegalArgumentException("工作日期格式无效");
                        }
                        update.set(Volunteer::getServiceDays, approval.getContent());
                    }
                    if (volunteerMapper.update(null, update) != 1) {
                        throw new IllegalStateException("审批业务更新失败");
                    }
                }
            }
            default -> throw new IllegalArgumentException("尚未支持该审批类型");
        }
        LambdaUpdateWrapper<Approval> update = new LambdaUpdateWrapper<>();
        update.eq(Approval::getId, id).eq(Approval::getStatus, "pending")
                .set(Approval::getStatus, result).set(Approval::getRemark, remark)
                .set(Approval::getUpdateTime, LocalDateTime.now());
        if (baseMapper.update(null, update) != 1) throw new IllegalStateException("审批状态更新失败");
        sendApprovalResultMessage(approval, result, remark);
        return true;
    }

    //登记审批结果通知
    private void sendApprovalResultMessage(Approval approval, String result, String remark) {
        ApprovalResultMessage resultMessage = ApprovalResultMessage.builder()
            .approvalId(approval.getId())
            .type(approval.getType())
            .applicantId(approval.getApplicantId())
            .applicantName(approval.getApplicantName())
            .result(result)
            .remark(remark)
            .approveTime(LocalDateTime.now())
            .build();

        outboxService.enqueue(
                RabbitMQConfig.APPROVAL_RESULT_DIRECT_EXCHANGE,
                RabbitMQConfig.APPROVAL_RESULT_ROUTING_KEY,
                resultMessage
        );
        log.info("审批结果消息已登记: approvalId={}, applicantId={}, result={}",
                approval.getId(), approval.getApplicantId(), result);
    }
}
