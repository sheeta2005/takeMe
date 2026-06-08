package com.me.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.entity.Order;
import com.me.entity.Review;
import com.me.entity.User;
import com.me.mapper.OrderMapper;
import com.me.mapper.ReviewMapper;
import com.me.service.UserService;
import com.me.result.Result;
import com.me.vo.PageResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final OrderMapper orderMapper;
    private final ReviewMapper reviewMapper;

    @GetMapping("/page")
    public Result<PageResultVO<User>> getUserPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        Page<User> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(User::getCreateTime);
        
        Page<User> mpPage = userService.page(pageParam, wrapper);
        mpPage.getRecords().forEach(u -> u.setPassword(null));
        
        PageResultVO<User> result = new PageResultVO<>(mpPage.getTotal(), mpPage.getRecords());
        return Result.success(result);
    }

    @GetMapping("/search")
    public Result<PageResultVO<User>> searchUser(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer gender,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        Page<User> mpPage = userService.searchUser(page, pageSize, keyword, gender, startDate, endDate);
        mpPage.getRecords().forEach(u -> u.setPassword(null));
        
        PageResultVO<User> result = new PageResultVO<>(mpPage.getTotal(), mpPage.getRecords());
        return Result.success(result);
    }

    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> getUserDetail(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPassword(null);

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);

        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Order::getUserId, id);
        orderWrapper.orderByDesc(Order::getCreateTime);
        orderWrapper.last("LIMIT 5");
        List<Order> recentOrders = orderMapper.selectList(orderWrapper);
        result.put("recentOrders", recentOrders);

        LambdaQueryWrapper<Review> reviewWrapper = new LambdaQueryWrapper<>();
        reviewWrapper.eq(Review::getUserId, id);
        reviewWrapper.orderByDesc(Review::getCreateTime);
        List<Review> reviews = reviewMapper.selectList(reviewWrapper);
        result.put("reviews", reviews);

        return Result.success(result);
    }

    @PostMapping("/add")
    public Result<Void> addUser(@RequestBody User user) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        Long count = userService.count(wrapper);
        if (count > 0) {
            return Result.error("账号已存在");
        }
        
        user.setPassword(passwordEncoder.encode("123456"));
        user.setStatus(1);
        userService.save(user);
        
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        userService.removeById(id);
        return Result.success();
    }

    @PostMapping("/status/{id}")
    public Result<Void> updateUserStatus(
            @PathVariable Long id,
            @RequestParam Integer status
    ) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        user.setStatus(status);
        userService.updateById(user);
        return Result.success();
    }
}
