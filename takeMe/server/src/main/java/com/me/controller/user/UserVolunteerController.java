package com.me.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.entity.OrderItem;
import com.me.entity.Review;
import com.me.entity.Volunteer;
import com.me.mapper.OrderItemMapper;
import com.me.mapper.ReviewMapper;
import com.me.mapper.VolunteerMapper;
import com.me.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/volunteer")
@RequiredArgsConstructor
public class UserVolunteerController {

    private final VolunteerMapper volunteerMapper;
    private final OrderItemMapper orderItemMapper;
    private final ReviewMapper reviewMapper;

    @GetMapping("/detail")
    public Result<Map<String, Object>> getVolunteerDetail(@RequestParam Long volunteerId) {
        Volunteer volunteer = volunteerMapper.selectById(volunteerId);
        if (volunteer == null) {
            return Result.error("志愿者不存在");
        }

        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getVolunteerId, volunteerId);
        wrapper.eq(OrderItem::getItemStatus, 3);
        Long completedCount = orderItemMapper.selectCount(wrapper);

        LambdaQueryWrapper<Review> reviewWrapper = new LambdaQueryWrapper<>();
        reviewWrapper.eq(Review::getVolunteerId, volunteerId);
        List<Review> reviews = reviewMapper.selectList(reviewWrapper);

        double averageRating = 0.0;
        if (!reviews.isEmpty()) {
            averageRating = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("volunteer", volunteer);
        result.put("completedCount", completedCount);
        result.put("reviews", reviews);
        result.put("averageRating", String.format("%.1f", averageRating));

        return Result.success(result);
    }
}
