package com.me.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.entity.Order;
import com.me.entity.OrderItem;
import com.me.entity.Review;
import com.me.mapper.OrderItemMapper;
import com.me.mapper.OrderMapper;
import com.me.mapper.ReviewMapper;
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
    private final ReviewMapper reviewMapper;

    @Scheduled(cron = "0 0 2 * * ?")
    public void autoEvaluateCompletedOrders() {
        log.info("开始执行订单自动评价定时任务");

        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Order::getStatus, 4);
        orderWrapper.le(Order::getCompleteTime, threeDaysAgo);
        List<Order> orders = orderMapper.selectList(orderWrapper);

        int totalEvaluated = 0;

        for (Order order : orders) {
            LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(OrderItem::getOrderId, order.getId());
            itemWrapper.in(OrderItem::getItemStatus, 3, 4);
            List<OrderItem> items = orderItemMapper.selectList(itemWrapper);

            for (OrderItem item : items) {
                if (item.getVolunteerId() == null) {
                    continue;
                }

                LambdaQueryWrapper<Review> reviewWrapper = new LambdaQueryWrapper<>();
                reviewWrapper.eq(Review::getOrderItemId, item.getId());
                Long count = reviewMapper.selectCount(reviewWrapper);

                if (count == 0) {
                    Review review = new Review();
                    review.setOrderId(item.getOrderId());
                    review.setOrderItemId(item.getId());
                    review.setUserId(order.getUserId());
                    review.setVolunteerId(item.getVolunteerId());
                    review.setRating(5);
                    review.setComment("系统默认好评");
                    review.setCreateTime(LocalDateTime.now());
                    reviewMapper.insert(review);

                    totalEvaluated++;
                    log.info("自动评价成功: orderItemId={}, volunteerId={}", item.getId(), item.getVolunteerId());
                }
            }
        }

        log.info("订单自动评价定时任务执行完成，共评价 {} 个服务项", totalEvaluated);
    }
}
