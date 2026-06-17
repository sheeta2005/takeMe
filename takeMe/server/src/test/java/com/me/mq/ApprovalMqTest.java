package com.me.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.dto.ApprovalResultMessage;
import com.me.dto.ApprovalSubmitMessage;
import com.me.entity.Admin;
import com.me.entity.Message;
import com.me.mapper.AdminMapper;
import com.me.mapper.MessageMapper;
import com.me.mq.config.RabbitMQConfig;
import com.me.mq.consumer.AdminApprovalConsumer;
import com.me.mq.consumer.VolunteerApprovalConsumer;
import com.me.mq.producer.MessageProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ApprovalMqTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private AdminApprovalConsumer adminApprovalConsumer;

    @Autowired
    private VolunteerApprovalConsumer volunteerApprovalConsumer;

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private MessageMapper messageMapper;

    private Long testApprovalId = 100L;
    private Long testVolunteerId = 999L;
    private String testVolunteerName = "测试志愿者";

    @BeforeEach
    public void cleanup() {
        LambdaQueryWrapper<Message> msgWrapper = new LambdaQueryWrapper<>();
        msgWrapper.eq(Message::getRelatedOrderId, testApprovalId);
        messageMapper.delete(msgWrapper);

        System.out.println("✅ 测试前置清理完成");
    }

    @Test
    public void testApprovalExchangeExists() {
        assertNotNull(rabbitAdmin.getQueueInfo(RabbitMQConfig.APPROVAL_ADMIN_QUEUE),
                "管理员审批队列不存在");
        assertNotNull(rabbitAdmin.getQueueInfo("approval.result.volunteer.queue"),
                "志愿者审批结果队列不存在");

        System.out.println("✅ 审批相关Exchange和队列配置测试通过");
    }

    @Test
    public void testSendApprovalSubmitMessage() {
        ApprovalSubmitMessage message = ApprovalSubmitMessage.builder()
                .approvalId(testApprovalId)
                .type("leave")
                .applicantId(testVolunteerId)
                .applicantName(testVolunteerName)
                .content("请假类型：事假，时间：2026-06-20 至 2026-06-22，原因：家里有事")
                .submitTime(LocalDateTime.now())
                .build();

        messageProducer.sendMessage(
                RabbitMQConfig.APPROVAL_SUBMIT_FANOUT_EXCHANGE,
                "",
                message
        );

        System.out.println("✅ 审批提交消息发送测试通过");
    }

    @Test
    public void testSendApprovalResultMessage() {
        ApprovalResultMessage message = ApprovalResultMessage.builder()
                .approvalId(testApprovalId)
                .type("leave")
                .applicantId(testVolunteerId)
                .applicantName(testVolunteerName)
                .result("approved")
                .remark("同意请假")
                .approveTime(LocalDateTime.now())
                .build();

        String routingKey = RabbitMQConfig.APPROVAL_RESULT_ROUTING_KEY_PREFIX + testVolunteerId;
        messageProducer.sendMessage(
                RabbitMQConfig.APPROVAL_RESULT_DIRECT_EXCHANGE,
                routingKey,
                message
        );

        System.out.println("✅ 审批结果消息发送测试通过");
    }

    @Test
    @Transactional
    @Rollback
    public void testAdminApprovalConsumer() {
        List<Admin> admins = adminMapper.selectList(null);
        if (admins.isEmpty()) {
            Admin testAdmin = new Admin();
            testAdmin.setUsername("test_admin");
            testAdmin.setPassword("123456");
            testAdmin.setRealName("测试管理员");
            testAdmin.setCreateTime(LocalDateTime.now());
            testAdmin.setLastLoginTime(LocalDateTime.now());
            testAdmin.setId(12L);
            adminMapper.insert(testAdmin);
            admins = adminMapper.selectList(null);
        }

        ApprovalSubmitMessage message = ApprovalSubmitMessage.builder()
                .approvalId(testApprovalId)
                .type("leave")
                .applicantId(testVolunteerId)
                .applicantName(testVolunteerName)
                .content("请假类型：事假，时间：2026-06-20 至 2026-06-22，原因：家里有事")
                .submitTime(LocalDateTime.now())
                .build();

        org.springframework.amqp.core.Message testMsg = createTestMessage();
        adminApprovalConsumer.handleAdminApprovalNotification(message, testMsg, null);

        int adminCount = admins.size();
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverType, 0);
        wrapper.eq(Message::getRelatedOrderId, testApprovalId);
        List<Message> messages = messageMapper.selectList(wrapper);

        assertEquals(adminCount, messages.size(),
                "应为每个管理员创建一条通知消息");

        if (!messages.isEmpty()) {
            Message notification = messages.get(0);
            assertTrue(notification.getTitle().contains("请假"));
            assertTrue(notification.getContent().contains(testVolunteerName));
        }

        System.out.println("✅ 管理员审批通知消费者测试通过: adminCount=" + adminCount);
    }

    @Test
    @Transactional
    @Rollback
    public void testVolunteerApprovalConsumerApproved() {
        ApprovalResultMessage message = ApprovalResultMessage.builder()
                .approvalId(testApprovalId)
                .type("leave")
                .applicantId(testVolunteerId)
                .applicantName(testVolunteerName)
                .result("approved")
                .remark("同意请假")
                .approveTime(LocalDateTime.now())
                .build();

        org.springframework.amqp.core.Message testMsg = createTestMessage();
        volunteerApprovalConsumer.handleVolunteerApprovalResult(message, testMsg, null);

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, testVolunteerId);
        wrapper.eq(Message::getReceiverType, 1);
        wrapper.eq(Message::getRelatedOrderId, testApprovalId);
        List<Message> messages = messageMapper.selectList(wrapper);

        assertFalse(messages.isEmpty(), "未找到志愿者通知消息");

        Message notification = messages.get(0);
        assertEquals("请假申请已通过", notification.getTitle());
        assertTrue(notification.getContent().contains("审批意见：同意请假"));

        System.out.println("✅ 志愿者审批通过通知测试通过: volunteerId=" + testVolunteerId);
    }

    @Test
    @Transactional
    @Rollback
    public void testVolunteerApprovalConsumerRejected() {
        ApprovalResultMessage message = ApprovalResultMessage.builder()
                .approvalId(testApprovalId + 1)
                .type("leave")
                .applicantId(testVolunteerId)
                .applicantName(testVolunteerName)
                .result("rejected")
                .remark("请假时间冲突，请重新申请")
                .approveTime(LocalDateTime.now())
                .build();

        org.springframework.amqp.core.Message testMsg = createTestMessage();
        volunteerApprovalConsumer.handleVolunteerApprovalResult(message, testMsg, null);

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, testVolunteerId);
        wrapper.eq(Message::getReceiverType, 1);
        wrapper.eq(Message::getRelatedOrderId, testApprovalId + 1);
        List<Message> messages = messageMapper.selectList(wrapper);

        assertFalse(messages.isEmpty(), "未找到志愿者通知消息");

        Message notification = messages.get(0);
        assertEquals("请假申请已拒绝", notification.getTitle());
        assertTrue(notification.getContent().contains("审批意见：请假时间冲突，请重新申请"));

        System.out.println("✅ 志愿者审批拒绝通知测试通过: volunteerId=" + testVolunteerId);
    }

    @Test
    @Transactional
    @Rollback
    public void testSkipNotificationWhenVolunteerIdNull() {
        ApprovalResultMessage message = ApprovalResultMessage.builder()
                .approvalId(testApprovalId + 2)
                .type("leave")
                .applicantId(null)
                .applicantName(testVolunteerName)
                .result("approved")
                .remark("测试")
                .approveTime(LocalDateTime.now())
                .build();

        org.springframework.amqp.core.Message testMsg = createTestMessage();
        volunteerApprovalConsumer.handleVolunteerApprovalResult(message, testMsg, null);

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getRelatedOrderId, testApprovalId + 2);
        List<Message> messages = messageMapper.selectList(wrapper);

        assertTrue(messages.isEmpty(), "志愿者ID为空时不应发送通知");

        System.out.println("✅ 空志愿者ID保护测试通过");
    }

    @Test
    @Transactional
    @Rollback
    public void testDifferentApprovalTypes() {
        String[] types = {"leave", "service_days_change", "service_change", "points_appeal"};

        for (String type : types) {
            ApprovalResultMessage message = ApprovalResultMessage.builder()
                    .approvalId(testApprovalId + 10 + types.length)
                    .type(type)
                    .applicantId(testVolunteerId)
                    .applicantName(testVolunteerName)
                    .result("approved")
                    .remark("测试" + type)
                    .approveTime(LocalDateTime.now())
                    .build();

            org.springframework.amqp.core.Message testMsg = createTestMessage();
            volunteerApprovalConsumer.handleVolunteerApprovalResult(message, testMsg, null);
        }

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, testVolunteerId);
        wrapper.eq(Message::getReceiverType, 1);
        List<Message> messages = messageMapper.selectList(wrapper);

        assertEquals(types.length, messages.size(),
                "应收到" + types.length + "条不同类型审批通知");

        System.out.println("✅ 多种审批类型测试通过: 共收到" + messages.size() + "条通知");
    }

    private org.springframework.amqp.core.Message createTestMessage() {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(1L);
        return new org.springframework.amqp.core.Message(new byte[0], properties);
    }
}

