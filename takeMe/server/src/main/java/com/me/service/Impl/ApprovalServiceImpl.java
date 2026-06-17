package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.me.dto.ApprovalResultMessage;
import com.me.dto.PageResultDTO;
import com.me.entity.Approval;
import com.me.entity.Message;
import com.me.entity.VolunteerLeave;
import com.me.mapper.ApprovalMapper;
import com.me.mapper.VolunteerLeaveMapper;
import com.me.mq.config.RabbitMQConfig;
import com.me.mq.producer.MessageProducer;
import com.me.service.ApprovalService;
import com.me.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl extends ServiceImpl<ApprovalMapper, Approval> implements ApprovalService {

    private final MessageService messageService;
    private final VolunteerLeaveMapper volunteerLeaveMapper;
    private final MessageProducer messageProducer;

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

        String type = approval.getType();

        switch (type) {
            case "leave":
                LambdaQueryWrapper<VolunteerLeave> leaveWrapper = new LambdaQueryWrapper<>();
                leaveWrapper.eq(VolunteerLeave::getVolunteerId, approval.getApplicantId())
                        .eq(VolunteerLeave::getStatus, 0)
                        .orderByDesc(VolunteerLeave::getCreateTime)
                        .last("LIMIT 1");
                VolunteerLeave leave = volunteerLeaveMapper.selectOne(leaveWrapper);
                if (leave != null) {
                    leave.setStatus((byte) 1);
                    volunteerLeaveMapper.updateById(leave);
                }
                break;
            case "service_days_change":
                break;
            default:
                break;
        }

        approval.setStatus("approved");
        approval.setRemark(remark);
        approval.setUpdateTime(LocalDateTime.now());
        boolean success = this.updateById(approval);

        if (success) {
            String title = "申请已通过";
            String typeText = "leave".equals(approval.getType()) ? "请假" : "信息修改";
            sendMessage(approval.getApplicantId(), 1, 0,
                    title, String.format("您的%s申请已通过审批", typeText), approval.getId());

            sendApprovalResultMessage(approval, "approved", remark);
        }

        return success;
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
        boolean success = this.updateById(approval);

        if (success) {
            if ("leave".equals(approval.getType())) {
                LambdaQueryWrapper<VolunteerLeave> leaveWrapper = new LambdaQueryWrapper<>();
                leaveWrapper.eq(VolunteerLeave::getVolunteerId, approval.getApplicantId())
                        .eq(VolunteerLeave::getStatus, 0)
                        .orderByDesc(VolunteerLeave::getCreateTime)
                        .last("LIMIT 1");
                VolunteerLeave leave = volunteerLeaveMapper.selectOne(leaveWrapper);
                if (leave != null) {
                    leave.setStatus((byte) 2);
                    volunteerLeaveMapper.updateById(leave);
                }
            }

            String title = "申请被拒绝";
            String typeText = "leave".equals(approval.getType()) ? "请假" : "信息修改";
            sendMessage(approval.getApplicantId(), 1, 0,
                    title, String.format("您的%s申请被拒绝，原因：%s", typeText, remark), approval.getId());

            sendApprovalResultMessage(approval, "rejected", remark);
        }

        return success;
    }


    private void sendMessage(Long receiverId, Integer receiverType, Integer type,
                             String title, String content, Long relatedOrderId) {
        Message message = new Message();
        message.setReceiverId(receiverId);
        message.setReceiverType(receiverType);
        message.setType(type);
        message.setTitle(title);
        message.setContent(content);
        message.setIsRead(0);
        message.setRelatedOrderId(relatedOrderId);
        message.setCreateTime(LocalDateTime.now());
        messageService.save(message);
    }

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

        try {
            String routingKey = RabbitMQConfig.APPROVAL_RESULT_ROUTING_KEY_PREFIX + approval.getApplicantId();
            messageProducer.sendMessage(
                RabbitMQConfig.APPROVAL_RESULT_DIRECT_EXCHANGE,
                routingKey,
                resultMessage
            );
            log.info("审批结果消息发送成功: approvalId={}, applicantId={}, result={}", 
                approval.getId(), approval.getApplicantId(), result);
        } catch (Exception e) {
            log.error("审批结果消息发送失败: approvalId={}", approval.getId(), e);
        }
    }
}
