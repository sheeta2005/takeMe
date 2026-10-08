package com.me.service.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.entity.Order;
import com.me.entity.OrderItem;
import com.me.mapper.OrderItemMapper;
import com.me.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoCompleteTask {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    //自动确认超时未确认订单
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void autoCompleteOrders() {
        // 单实例调度依靠订单行锁和状态条件，不需要额外的分布式锁。
        try {
            log.info("开始执行订单自动确认完成定时任务");

            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Order::getStatus, 3);
            wrapper.and(w -> w.le(Order::getCompleteTime, yesterday)
                    .or()
                    .isNull(Order::getCompleteTime)
                    .le(Order::getCreateTime, yesterday));

            List<Order> pendingOrders = orderMapper.selectList(wrapper);

            for (Order order : pendingOrders) {
                // 与用户确认/取消共用订单行锁，防止调度基于旧状态写回。
                order = orderMapper.selectForUpdate(order.getId());
                if (order == null || order.getStatus() != 3) {
                    continue;
                }
                List<OrderItem> items = orderItemMapper.selectByOrderForUpdate(order.getId());
                if (items.stream().noneMatch(item -> item.getItemStatus() == 3)
                        || items.stream().anyMatch(item -> item.getItemStatus() < 3)) {
                    continue;
                }

                for (OrderItem item : items) {
                    if (item.getItemStatus() == 3) {
                        if (orderItemMapper.changeStatus(item.getId(), 3, 4) != 1) {
                            throw new IllegalStateException("自动确认时服务状态已变化");
                        }
                    }
                }

                order.setStatus(4);
                order.setCompleteTime(LocalDateTime.now());
                orderMapper.updateById(order);

                log.info("订单 {} 已自动确认完成", order.getOrderNo());
            }

            log.info("订单自动确认完成定时任务执行完毕，共处理 {} 个订单", pendingOrders.size());

        } catch (Exception e) {
            log.error("订单自动确认完成定时任务执行失败", e);
            throw new IllegalStateException("订单自动确认失败", e);
        }
    }
}
