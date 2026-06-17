package com.me.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.dto.OrderDTO;
import com.me.dto.OrderItemDTO;
import com.me.dto.OrderTimeoutMessage;
import com.me.entity.Order;
import com.me.entity.OrderItem;
import com.me.mapper.OrderItemMapper;
import com.me.mapper.OrderMapper;
import com.me.mq.config.RabbitMQConfig;
import com.me.mq.consumer.OrderTimeoutConsumer;
import com.me.mq.producer.MessageProducer;
import com.me.service.OrderService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class OrderTimeoutMqTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private OrderTimeoutConsumer orderTimeoutConsumer;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private OrderService orderService;

    // 注入连接工厂，用来获取临时Channel
    @Autowired
    private CachingConnectionFactory connectionFactory;

    private Long testOrderId;
    private String testOrderNo;
    private Long testUserId = 1L;

    // 工具方法：获取可用Channel
    private Channel getTestChannel() throws Exception {
        return connectionFactory.createConnection().createChannel(false);
    }

    @BeforeEach
    public void cleanup() {
        redisTemplate.delete("mq:idempotent:order:timeout:*");

        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.like(Order::getOrderNo, "TEST_TIMEOUT_");
        List<Order> testOrders = orderMapper.selectList(orderWrapper);
        for (Order order : testOrders) {
            LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(OrderItem::getOrderId, order.getId());
            orderItemMapper.delete(itemWrapper);
        }
        orderMapper.delete(orderWrapper);

        Queue testQueue = new Queue("test.connection.queue", true, false, false);
        rabbitAdmin.declareQueue(testQueue);

        System.out.println("✅ 测试前置清理完成");
    }

    @Test
    @Transactional
    @Rollback
    public void testSendOrderTimeoutMessage() {
        OrderDTO orderDTO = new OrderDTO();

        OrderItemDTO itemDTO = new OrderItemDTO();
        itemDTO.setServiceId(1L);
        itemDTO.setServiceName("测试服务");
        itemDTO.setServicePrice(100);
        itemDTO.setQuantity(1);
        itemDTO.setItemPrice(100);
        itemDTO.setServiceDate(LocalDate.now().plusDays(1).toString());
        itemDTO.setServiceTime("10:00");
        itemDTO.setAddress("测试地址");
        itemDTO.setRemark("测试备注");

        List<OrderItemDTO> itemList = new ArrayList<>();
        itemList.add(itemDTO);

        var orderVO = orderService.createOrder(testUserId, orderDTO, itemList);
        testOrderId = orderVO.getId();
        testOrderNo = orderVO.getOrderNo();

        assertNotNull(testOrderId, "订单创建失败");
        assertEquals(0, orderVO.getStatus(), "订单初始状态应为待接单");

        Order order = orderMapper.selectById(testOrderId);
        assertNotNull(order, "数据库中未找到订单");
        assertEquals(0, order.getStatus(), "数据库中订单状态应为待接单");

        System.out.println("✅ 测试订单创建成功: orderId=" + testOrderId + ", orderNo=" + testOrderNo);
    }

    @Test
    public void testRabbitMQConnection() {
        String testQueue = "test.connection.queue";
        String testMessage = "connection-test-" + System.currentTimeMillis();

        rabbitTemplate.convertAndSend("", testQueue, testMessage);

        Object received = rabbitTemplate.receiveAndConvert(testQueue, 5000);

        assertNotNull(received, "RabbitMQ连接失败，未收到消息");
        assertEquals(testMessage, received.toString(), "消息内容不匹配");

        System.out.println("✅ RabbitMQ连接测试通过");
    }

    @Test
    public void testOrderTimeoutMessageStructure() {
        OrderTimeoutMessage message = new OrderTimeoutMessage(123L, "TEST_ORDER_NO", 456L);

        assertEquals(123L, message.getOrderId());
        assertEquals("TEST_ORDER_NO", message.getOrderNo());
        assertEquals(456L, message.getUserId());

        messageProducer.sendMessage(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATE_ROUTING_KEY,
                message
        );

        System.out.println("✅ 订单超时消息结构测试通过");
    }

    @Test
    @Transactional
    @Rollback
    public void testOrderTimeoutConsumerLogic() {
        Order order = new Order();
        order.setUserId(testUserId);
        order.setOrderNo("TEST_TIMEOUT_" + System.currentTimeMillis());
        order.setTotalPrice(100);
        order.setStatus(0);
        order.setIsReviewed(0);
        order.setCreateTime(LocalDateTime.now());
        order.setServiceDate(LocalDate.now().toString());
        order.setServiceTime("10:00");
        orderMapper.insert(order);
        testOrderId = order.getId();
        testOrderNo = order.getOrderNo();
        
        OrderItem item = new OrderItem();
        item.setOrderId(testOrderId);
        item.setServiceId(1L);
        item.setServiceName("测试服务");
        item.setServicePrice(100);
        item.setQuantity(1);
        item.setItemPrice(100);
        item.setItemStatus(0);
        item.setCreateTime(LocalDateTime.now());
        orderItemMapper.insert(item);
        
        OrderTimeoutMessage timeoutMessage = new OrderTimeoutMessage(testOrderId, testOrderNo, testUserId);
        
        orderTimeoutConsumer.handleOrderTimeout(timeoutMessage, null, null);
        
        Order updatedOrder = orderMapper.selectById(testOrderId);
        assertNotNull(updatedOrder, "订单不存在");
        assertEquals(5, updatedOrder.getStatus(), "订单状态应更新为已取消");
        
        OrderItem updatedItem = orderItemMapper.selectById(item.getId());
        assertNotNull(updatedItem, "订单项不存在");
        assertEquals(5, updatedItem.getItemStatus(), "订单项状态应更新为已取消");
        
        System.out.println("✅ 订单超时消费者逻辑测试通过: orderId=" + testOrderId);
    }

    @Test
    @Transactional
    @Rollback
    public void testIdempotency() {
        Order order = new Order();
        order.setUserId(testUserId);
        order.setOrderNo("TEST_IDEMPOTENCY_" + System.currentTimeMillis());
        order.setTotalPrice(100);
        order.setStatus(0);
        order.setIsReviewed(0);
        order.setCreateTime(LocalDateTime.now());
        order.setServiceDate(LocalDate.now().toString());
        order.setServiceTime("10:00");
        orderMapper.insert(order);
        testOrderId = order.getId();
        
        OrderItem item = new OrderItem();
        item.setOrderId(testOrderId);
        item.setServiceId(1L);
        item.setServiceName("测试服务");
        item.setServicePrice(100);
        item.setQuantity(1);
        item.setItemPrice(100);
        item.setItemStatus(0);
        item.setCreateTime(LocalDateTime.now());
        orderItemMapper.insert(item);
        
        OrderTimeoutMessage timeoutMessage = new OrderTimeoutMessage(testOrderId, order.getOrderNo(), testUserId);
        
        orderTimeoutConsumer.handleOrderTimeout(timeoutMessage, null, null);
        
        String lockKey = "mq:idempotent:order:timeout:" + testOrderId;
        Boolean isLocked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 300, TimeUnit.SECONDS);
        assertFalse(isLocked, "幂等锁应已存在，第二次消费应被阻止");
        
        Order updatedOrder = orderMapper.selectById(testOrderId);
        assertEquals(5, updatedOrder.getStatus(), "订单应保持已取消状态");
        
        System.out.println("✅ 幂等性测试通过: 重复消息已被阻止");
    }

    @Test
    @Transactional
    @Rollback
    public void testCancelAlreadyAcceptedOrder() {
        Order order = new Order();
        order.setUserId(testUserId);
        order.setOrderNo("TEST_ACCEPTED_" + System.currentTimeMillis());
        order.setTotalPrice(100);
        order.setStatus(1);
        order.setIsReviewed(0);
        order.setCreateTime(LocalDateTime.now());
        order.setServiceDate(LocalDate.now().toString());
        order.setServiceTime("10:00");
        orderMapper.insert(order);
        testOrderId = order.getId();
        
        OrderItem item = new OrderItem();
        item.setOrderId(testOrderId);
        item.setVolunteerId(999L);
        item.setServiceId(1L);
        item.setServiceName("测试服务");
        item.setServicePrice(100);
        item.setQuantity(1);
        item.setItemPrice(100);
        item.setItemStatus(1);
        item.setCreateTime(LocalDateTime.now());
        orderItemMapper.insert(item);
        
        OrderTimeoutMessage timeoutMessage = new OrderTimeoutMessage(testOrderId, order.getOrderNo(), testUserId);
        
        orderTimeoutConsumer.handleOrderTimeout(timeoutMessage, null, null);
        
        Order updatedOrder = orderMapper.selectById(testOrderId);
        assertEquals(1, updatedOrder.getStatus(), "已接单的订单不应被取消");
        
        System.out.println("✅ 已接单订单保护测试通过: 订单未被错误取消");
    }
}