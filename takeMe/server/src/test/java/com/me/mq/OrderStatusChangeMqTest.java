package com.me.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.dto.OrderStatusChangeMessage;
import com.me.entity.Message;
import com.me.entity.Order;
import com.me.entity.OrderItem;
import com.me.mapper.MessageMapper;
import com.me.mapper.OrderItemMapper;
import com.me.mapper.OrderMapper;
import com.me.mq.config.RabbitMQConfig;
import com.me.mq.consumer.AdminAuditConsumer;
import com.me.mq.consumer.UserNotificationConsumer;
import com.me.mq.consumer.VolunteerNotificationConsumer;
import com.me.mq.producer.MessageProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
public class OrderStatusChangeMqTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private UserNotificationConsumer userNotificationConsumer;

    @Autowired
    private VolunteerNotificationConsumer volunteerNotificationConsumer;

    @Autowired
    private AdminAuditConsumer adminAuditConsumer;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private MessageMapper messageMapper;

    private Long testOrderId;
    private String testOrderNo;
    private Long testUserId = 1L;
    private Long testVolunteerId = 999L;

    @BeforeEach
    public void cleanup() {
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.like(Order::getOrderNo, "TEST_STATUS_");
        List<Order> testOrders = orderMapper.selectList(orderWrapper);
        for (Order order : testOrders) {
            LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(OrderItem::getOrderId, order.getId());
            orderItemMapper.delete(itemWrapper);

            LambdaQueryWrapper<Message> msgWrapper = new LambdaQueryWrapper<>();
            msgWrapper.eq(Message::getRelatedOrderId, order.getId());
            messageMapper.delete(msgWrapper);
        }
        orderMapper.delete(orderWrapper);

        System.out.println("✅ 测试前置清理完成");
    }

    @Test
    public void testFanoutExchangeExists() {
        assertNotNull(rabbitAdmin.getQueueInfo(RabbitMQConfig.NOTIFICATION_USER_QUEUE),
                "用户通知队列不存在");
        assertNotNull(rabbitAdmin.getQueueInfo(RabbitMQConfig.NOTIFICATION_VOLUNTEER_QUEUE),
                "志愿者通知队列不存在");
        assertNotNull(rabbitAdmin.getQueueInfo(RabbitMQConfig.NOTIFICATION_ADMIN_QUEUE),
                "管理员审计队列不存在");

        System.out.println("✅ Fanout Exchange和队列配置测试通过");
    }

    @Test
    public void testSendStatusChangeMessage() {
        OrderStatusChangeMessage message = OrderStatusChangeMessage.builder()
                .orderId(123L)
                .orderNo("TEST_ORDER_001")
                .oldStatus(0)
                .newStatus(1)
                .userId(testUserId)
                .volunteerId(testVolunteerId)
                .changeTime(LocalDateTime.now())
                .remark("志愿者接单")
                .build();

        messageProducer.sendMessage(
                RabbitMQConfig.ORDER_STATUS_FANOUT_EXCHANGE,
                "",
                message
        );

        System.out.println("✅ 订单状态变更消息发送测试通过");
    }

    @Test
    @Transactional
    @Rollback
    public void testUserNotificationConsumer() {
        Order order = createTestOrder(0);
        testOrderId = order.getId();
        testOrderNo = order.getOrderNo();

        OrderStatusChangeMessage message = OrderStatusChangeMessage.builder()
                .orderId(testOrderId)
                .orderNo(testOrderNo)
                .oldStatus(0)
                .newStatus(1)
                .userId(testUserId)
                .volunteerId(testVolunteerId)
                .changeTime(LocalDateTime.now())
                .remark("志愿者接单")
                .build();

        userNotificationConsumer.handleUserNotification(message, null, null);

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, testUserId);
        wrapper.eq(Message::getReceiverType, 2);
        wrapper.eq(Message::getRelatedOrderId, testOrderId);
        List<Message> messages = messageMapper.selectList(wrapper);

        assertFalse(messages.isEmpty(), "未找到用户通知消息");

        Message notification = messages.get(0);
        assertEquals("服务已接单", notification.getTitle());
        assertTrue(notification.getContent().contains("状态已更新"));
        assertTrue(notification.getContent().contains("待接单 → 已接单"));

        System.out.println("✅ 用户通知消费者测试通过: userId=" + testUserId);
    }

    @Test
    @Transactional
    @Rollback
    public void testVolunteerNotificationConsumer() {
        Order order = createTestOrder(0);
        testOrderId = order.getId();
        testOrderNo = order.getOrderNo();

        OrderStatusChangeMessage message = OrderStatusChangeMessage.builder()
                .orderId(testOrderId)
                .orderNo(testOrderNo)
                .oldStatus(1)
                .newStatus(2)
                .userId(testUserId)
                .volunteerId(testVolunteerId)
                .changeTime(LocalDateTime.now())
                .remark("志愿者开始服务")
                .build();

        volunteerNotificationConsumer.handleVolunteerNotification(message, null, null);

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, testVolunteerId);
        wrapper.eq(Message::getReceiverType, 1);
        wrapper.eq(Message::getRelatedOrderId, testOrderId);
        List<Message> messages = messageMapper.selectList(wrapper);

        assertFalse(messages.isEmpty(), "未找到志愿者通知消息");

        Message notification = messages.get(0);
        assertEquals("开始服务提醒", notification.getTitle());
        assertTrue(notification.getContent().contains("状态已更新"));
        assertTrue(notification.getContent().contains("已接单 → 服务中"));

        System.out.println("✅ 志愿者通知消费者测试通过: volunteerId=" + testVolunteerId);
    }

    @Test
    public void testAdminAuditConsumer() {
        OrderStatusChangeMessage message = OrderStatusChangeMessage.builder()
                .orderId(456L)
                .orderNo("TEST_AUDIT_001")
                .oldStatus(2)
                .newStatus(3)
                .userId(testUserId)
                .volunteerId(testVolunteerId)
                .changeTime(LocalDateTime.now())
                .remark("志愿者完成服务")
                .build();

        adminAuditConsumer.handleAdminAudit(message, null, null);

        System.out.println("✅ 管理员审计消费者测试通过（日志已输出）");
    }

    @Test
    @Transactional
    @Rollback
    public void testSkipNotificationWhenVolunteerIdNull() {
        Order order = createTestOrder(0);
        testOrderId = order.getId();
        testOrderNo = order.getOrderNo();

        OrderStatusChangeMessage message = OrderStatusChangeMessage.builder()
                .orderId(testOrderId)
                .orderNo(testOrderNo)
                .oldStatus(0)
                .newStatus(1)
                .userId(testUserId)
                .volunteerId(null)
                .changeTime(LocalDateTime.now())
                .remark("无志愿者ID")
                .build();

        volunteerNotificationConsumer.handleVolunteerNotification(message, null, null);

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getRelatedOrderId, testOrderId);
        wrapper.eq(Message::getReceiverType, 1);
        List<Message> messages = messageMapper.selectList(wrapper);

        assertTrue(messages.isEmpty(), "志愿者ID为空时不应发送通知");

        System.out.println("✅ 空志愿者ID保护测试通过");
    }

    @Test
    @Transactional
    @Rollback
    public void testMultipleStatusChanges() {
        Order order = createTestOrder(0);
        testOrderId = order.getId();
        testOrderNo = order.getOrderNo();

        int[][] statusChanges = {
                {0, 1},
                {1, 2},
                {2, 3},
                {3, 4}
        };

        for (int[] change : statusChanges) {
            OrderStatusChangeMessage message = OrderStatusChangeMessage.builder()
                    .orderId(testOrderId)
                    .orderNo(testOrderNo)
                    .oldStatus(change[0])
                    .newStatus(change[1])
                    .userId(testUserId)
                    .volunteerId(testVolunteerId)
                    .changeTime(LocalDateTime.now())
                    .remark("状态变更测试")
                    .build();

            userNotificationConsumer.handleUserNotification(message, null, null);
        }

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, testUserId);
        wrapper.eq(Message::getRelatedOrderId, testOrderId);
        List<Message> messages = messageMapper.selectList(wrapper);

        assertEquals(4, messages.size(), "应收到4条状态变更通知");

        System.out.println("✅ 多次状态变更测试通过: 共收到" + messages.size() + "条通知");
    }

    private Order createTestOrder(Integer status) {
        Order order = new Order();
        order.setUserId(testUserId);
        order.setOrderNo("TEST_STATUS_" + System.currentTimeMillis());
        order.setTotalPrice(100);
        order.setStatus(status);
        order.setIsReviewed(0);
        order.setCreateTime(LocalDateTime.now());
        order.setServiceDate(java.time.LocalDate.now().toString());
        order.setServiceTime(LocalDateTime.now().toString());
        order.setAddress("wqeqw");
        orderMapper.insert(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setServiceId(1L);
        item.setServiceName("测试服务");
        item.setServicePrice(100);
        item.setQuantity(1);
        item.setItemPrice(100);
        item.setItemStatus(status);
        item.setCreateTime(LocalDateTime.now());
        if (status >= 1) {
            item.setVolunteerId(testVolunteerId);
        }
        orderItemMapper.insert(item);

        return order;
    }


}

