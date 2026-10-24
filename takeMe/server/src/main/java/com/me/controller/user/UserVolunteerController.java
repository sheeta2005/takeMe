package com.me.controller.user;

import com.me.entity.Review;
import com.me.entity.Volunteer;
import com.me.mapper.OrderItemMapper;
import com.me.mapper.ReviewMapper;
import com.me.mapper.VolunteerMapper;
import com.me.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Tag(name = "用户-志愿者相关")
@RestController
@RequestMapping("/api/user/volunteer")
@RequiredArgsConstructor
public class UserVolunteerController {

    private final VolunteerMapper volunteerMapper;
    private final OrderItemMapper orderItemMapper;
    private final ReviewMapper reviewMapper;

    //查询志愿者详情
    @Operation(summary = "查志愿者详情")
    @GetMapping("/detail")
    public Result<Map<String, Object>> getVolunteerDetail(@RequestParam Long volunteerId) {
        Volunteer volunteer = volunteerMapper.selectById(volunteerId);
        if (volunteer == null) {
            return Result.error("志愿者不存在");
        }

        Long completedCount = orderItemMapper.countForVolunteerStatus(volunteerId, 3);

        List<Review> reviews = reviewMapper.selectByVolunteerId(volunteerId);

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
