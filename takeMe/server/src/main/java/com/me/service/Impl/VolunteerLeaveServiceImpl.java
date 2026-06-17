package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.dto.ApprovalSubmitMessage;
import com.me.entity.Approval;
import com.me.entity.Message;
import com.me.entity.Volunteer;
import com.me.entity.VolunteerLeave;
import com.me.mapper.ApprovalMapper;
import com.me.mapper.VolunteerLeaveMapper;
import com.me.mapper.VolunteerMapper;
import com.me.mq.config.RabbitMQConfig;
import com.me.mq.producer.MessageProducer;
import com.me.service.MessageService;
import com.me.service.VolunteerLeaveService;
import com.me.vo.VolunteerLeaveVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VolunteerLeaveServiceImpl implements VolunteerLeaveService {

    private final VolunteerLeaveMapper volunteerLeaveMapper;
    private final ApprovalMapper approvalMapper;
    private final VolunteerMapper volunteerMapper;
    private final MessageService messageService;
    private final MessageProducer messageProducer;

    @Override
    public List<VolunteerLeaveVO> getListByVolunteerId(Long volunteerId) {
        LambdaQueryWrapper<VolunteerLeave> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VolunteerLeave::getVolunteerId, volunteerId);
        wrapper.orderByDesc(VolunteerLeave::getCreateTime);

        List<VolunteerLeave> leaves = volunteerLeaveMapper.selectList(wrapper);
        return leaves.stream().map(leave -> {
            VolunteerLeaveVO vo = new VolunteerLeaveVO();
            BeanUtils.copyProperties(leave, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void submit(VolunteerLeave leave) {
        leave.setStatus((byte) 0);
        leave.setCreateTime(LocalDateTime.now());
        volunteerLeaveMapper.insert(leave);

        Volunteer volunteer = volunteerMapper.selectById(leave.getVolunteerId());
        String applicantName = volunteer != null ? volunteer.getRealName() : "志愿者ID:" + leave.getVolunteerId();

        Approval approval = new Approval();
        approval.setType("leave");
        approval.setApplicantId(leave.getVolunteerId());
        approval.setApplicantName(applicantName);
        approval.setContent("请假类型：" + (leave.getType() == 0 ? "事假" : "病假") +
                "，时间：" + leave.getStartTime() + " 至 " + leave.getEndTime() +
                "，原因：" + leave.getReason());
        approval.setStatus("pending");
        approval.setCreateTime(LocalDateTime.now());
        approvalMapper.insert(approval);

        sendMessage(leave.getVolunteerId(), 1, 1, "请假申请已提交",
                applicantName + "的请假申请已提交，等待管理员审批", null);

        ApprovalSubmitMessage submitMessage = ApprovalSubmitMessage.builder()
            .approvalId(approval.getId())
            .type("leave")
            .applicantId(leave.getVolunteerId())
            .applicantName(applicantName)
            .content(approval.getContent())
            .submitTime(LocalDateTime.now())
            .build();

        try {
            messageProducer.sendMessage(
                RabbitMQConfig.APPROVAL_SUBMIT_FANOUT_EXCHANGE,
                "",
                submitMessage
            );
            log.info("请假申请提交消息发送成功: approvalId={}, applicantId={}", 
                approval.getId(), leave.getVolunteerId());
        } catch (Exception e) {
            log.error("请假申请提交消息发送失败: approvalId={}", approval.getId(), e);
        }
    }

    private void sendMessage(Long receiverId, Integer receiverType, Integer type, 
                             String title, String content, Long relatedOrderId) {
        Message msg = new Message();
        msg.setReceiverId(receiverId);
        msg.setReceiverType(receiverType);
        msg.setType(type);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setIsRead(0);
        msg.setRelatedOrderId(relatedOrderId);
        msg.setCreateTime(LocalDateTime.now());
        messageService.sendMessage(msg);
    }
}
