package com.me.service.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.entity.Order;
import com.me.entity.OrderItem;
import com.me.mapper.OrderItemMapper;
import com.me.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoCompleteTask {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_KEY = "order:auto:complete:lock";
    private static final long LOCK_EXPIRE_TIME = 5;

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void autoCompleteOrders() {
        String lockValue = String.valueOf(System.currentTimeMillis());

        Boolean isLocked = redisTemplate.opsForValue().setIfAbsent(
                LOCK_KEY,
                lockValue,
                LOCK_EXPIRE_TIME,
                TimeUnit.MINUTES
        );

        if (Boolean.FALSE.equals(isLocked)) {
            log.warn("定时任务正在执行，跳过本次调度");
            return;
        }

        try {
            log.info("开始执行订单自动确认完成定时任务");

            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Order::getStatus, 3);
            wrapper.le(Order::getCompleteTime, yesterday);

            List<Order> pendingOrders = orderMapper.selectList(wrapper);

            for (Order order : pendingOrders) {
                LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
                itemWrapper.eq(OrderItem::getOrderId, order.getId());
                itemWrapper.in(OrderItem::getItemStatus, 3);

                List<OrderItem> pendingItems = orderItemMapper.selectList(itemWrapper);

                for (OrderItem item : pendingItems) {
                    item.setItemStatus(4);
                    orderItemMapper.updateById(item);
                }

                order.setStatus(4);
                order.setCompleteTime(LocalDateTime.now());
                orderMapper.updateById(order);

                log.info("订单 {} 已自动确认完成", order.getOrderNo());
            }

            log.info("订单自动确认完成定时任务执行完毕，共处理 {} 个订单", pendingOrders.size());

        } catch (Exception e) {
            log.error("订单自动确认完成定时任务执行失败", e);
        } finally {
            String currentLockValue = redisTemplate.opsForValue().get(LOCK_KEY);
            if (lockValue.equals(currentLockValue)) {
                redisTemplate.delete(LOCK_KEY);
            }
        }
    }
}
