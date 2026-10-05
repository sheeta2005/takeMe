package com.me.service.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.entity.Order;
import com.me.entity.OrderItem;
import com.me.mapper.OrderItemMapper;
import com.me.mapper.OrderMapper;
import com.me.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderTimeoutCancelTask {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderService orderService;

    @Scheduled(fixedDelay = 300000)
    public void cancelTimeoutOrders() {
        // 每项在独立事务中复核预约时间，避免首项超时取消整单。
        List<Order> unpaid = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, 6)
                .le(Order::getCreateTime, LocalDateTime.now().minusMinutes(15)));
        for (Order order : unpaid) {
            try {
                orderService.expireUnpaidOrder(order.getId());
            } catch (Exception e) {
                log.error("取消未支付订单失败: orderId={}", order.getId(), e);
            }
        }

        List<OrderItem> pending = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getItemStatus, 0));
        for (OrderItem item : pending) {
            try {
                orderService.expirePendingItem(item.getId());
            } catch (Exception e) {
                log.error("取消无人接单服务项失败: orderItemId={}", item.getId(), e);
            }
        }

        List<OrderItem> accepted = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getItemStatus, 1));
        for (OrderItem item : accepted) {
            try {
                orderService.expireAcceptedItem(item.getId(), null);
            } catch (Exception e) {
                log.error("取消未启动服务项失败: orderItemId={}", item.getId(), e);
            }
        }
    }
}
