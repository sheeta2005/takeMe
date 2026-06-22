package com.me.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.entity.Order;
import com.me.entity.Review;
import com.me.entity.User;
import com.me.mapper.OrderMapper;
import com.me.mapper.ReviewMapper;
import com.me.result.Result;
import com.me.service.UserService;
import com.me.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "用户-信息管理")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final OrderMapper orderMapper;
    private final ReviewMapper reviewMapper;

    // 获取当前用户信息
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<User> getUserInfo(@RequestHeader("Authorization") String authHeader) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        User user = userService.getById(userId);

        if (user == null) {
            return Result.error("用户不存在");
        }

        user.setPassword(null);
        return Result.success(user);
    }

    // 修改用户信息
    @Operation(summary = "修改用户信息")
    @PostMapping("/update")
    public Result updateUserInfo(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody User user
    ) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        user.setId(userId);

        // 禁止修改敏感字段
        user.setUsername(null);
        user.setPassword(null);
        user.setStatus(null);
        user.setCreateTime(null);
        user.setLastLoginTime(null);

        userService.updateById(user);
        return Result.success();
    }

    // 获取用户首页统计数据
    @Operation(summary = "查用户仪表盘数据")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getUserStatistics(@RequestHeader("Authorization") String authHeader) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);

        // 总订单数
        LambdaQueryWrapper<Order> allOrderWrapper = new LambdaQueryWrapper<>();
        allOrderWrapper.eq(Order::getUserId, userId);
        allOrderWrapper.ne(Order::getStatus, 5); // 排除已取消
        Long totalCount = orderMapper.selectCount(allOrderWrapper);

        // 已完成订单数
        LambdaQueryWrapper<Order> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.eq(Order::getUserId, userId);
        completedWrapper.eq(Order::getStatus, 4);
        Long completedCount = orderMapper.selectCount(completedWrapper);

        // 进行中订单数（已接单1 + 服务中2 + 待确认3）
        LambdaQueryWrapper<Order> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(Order::getUserId, userId);
        pendingWrapper.in(Order::getStatus, 1, 2, 3);
        Long pendingCount = orderMapper.selectCount(pendingWrapper);

        // 平均评分
        LambdaQueryWrapper<Review> reviewWrapper = new LambdaQueryWrapper<>();
        reviewWrapper.eq(Review::getUserId, userId);
        List<Review> reviews = reviewMapper.selectList(reviewWrapper);

        double averageRating = 0.0;
        if (!reviews.isEmpty()) {
            averageRating = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", totalCount);
        stats.put("completed", completedCount);
        stats.put("pending", pendingCount);
        stats.put("rating", String.format("%.1f", averageRating));

        return Result.success(stats);
    }
}