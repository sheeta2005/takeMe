package com.me.service.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.entity.Order;
import com.me.entity.PaymentTransaction;
import com.me.mapper.OrderItemMapper;
import com.me.mapper.OrderMapper;
import com.me.mapper.PaymentTransactionMapper;
import com.me.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderTimeoutCancelTask {

    private final OrderMapper orderMapper;
    private final PaymentTransactionMapper paymentTransactionMapper;
    private final OrderItemMapper orderItemMapper;
    private final MessageService messageService;

    @Scheduled(fixedDelay = 300000)
    @Transactional(rollbackFor = Exception.class)
    public void cancelTimeoutOrders() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(15);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStatus, 6);
        wrapper.le(Order::getCreateTime, timeoutThreshold);
        List<Order> timeoutOrders = orderMapper.selectList(wrapper);

        if (timeoutOrders.isEmpty()) {
            return;
        }

        log.info("开始处理超时订单，数量: {}", timeoutOrders.size());

        for (Order order : timeoutOrders) {
            try {
                cancelTimeoutOrder(order);
            } catch (Exception e) {
                log.error("取消超时订单失败: orderId={}", order.getId(), e);
            }
        }
    }

    private void cancelTimeoutOrder(Order order) {
        order.setStatus(5);
        orderMapper.updateById(order);

        LambdaQueryWrapper<PaymentTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentTransaction::getOrderId, order.getId());
        wrapper.eq(PaymentTransaction::getPaymentStatus, 0);
        PaymentTransaction transaction = paymentTransactionMapper.selectOne(wrapper);

        if (transaction != null) {
            transaction.setPaymentStatus(3);
            transaction.setUpdateTime(LocalDateTime.now());
            transaction.setRemark("订单超时自动取消");
            paymentTransactionMapper.updateById(transaction);
        }

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.me.entity.OrderItem> itemWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        itemWrapper.eq(com.me.entity.OrderItem::getOrderId, order.getId());
        itemWrapper.isNotNull(com.me.entity.OrderItem::getVolunteerId);
        List<com.me.entity.OrderItem> assignedItems = orderItemMapper.selectList(itemWrapper);

        for (com.me.entity.OrderItem item : assignedItems) {
            item.setVolunteerId(null);
            item.setItemStatus(5);
            orderItemMapper.updateById(item);
        }

        log.info("超时订单已取消: orderId={}, orderNo={}", order.getId(), order.getOrderNo());
    }
}
