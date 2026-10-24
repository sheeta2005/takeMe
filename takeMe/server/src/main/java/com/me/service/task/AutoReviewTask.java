package com.me.task;

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

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoReviewTask {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderService orderService;

    //为超时未评价订单补默认好评
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoEvaluateCompletedOrders() {
        log.info("开始执行订单自动评价定时任务");

        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

        List<Order> orders = orderMapper.selectReviewCandidates(threeDaysAgo);

        int totalEvaluated = 0;

        for (Order order : orders) {
            List<OrderItem> items = orderItemMapper.selectCompletedByOrderId(order.getId());

            for (OrderItem item : items) {
                if (item.getVolunteerId() == null) {
                    continue;
                }

                // 每个候选项通过独立事务重新检查，与人工评价使用相同的加锁顺序。
                if (orderService.autoEvaluateOrderItem(item.getId())) {
                    totalEvaluated++;
                    log.info("自动评价成功: orderItemId={}, volunteerId={}", item.getId(), item.getVolunteerId());
                }
            }
        }

        log.info("订单自动评价定时任务执行完成，共评价 {} 个服务项", totalEvaluated);
    }
}
