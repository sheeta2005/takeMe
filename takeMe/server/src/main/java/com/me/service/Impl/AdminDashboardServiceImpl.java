package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.entity.Order;
import com.me.entity.User;
import com.me.entity.Volunteer;
import com.me.mapper.OrderMapper;
import com.me.mapper.UserMapper;
import com.me.mapper.VolunteerMapper;
import com.me.redis.annotation.RedisCache;
import com.me.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final VolunteerMapper volunteerMapper;

    @Override
    @RedisCache(prefix = "admin:dashboard:data", expire = 10, nullExpire = 2)
    public Map<String, Object> getDashboardData() {
        Map<String, Object> data = new HashMap<>();

        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        Long totalOrders = orderMapper.selectCount(orderWrapper);
        data.put("totalOrders", totalOrders);

        LambdaQueryWrapper<Order> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.in(Order::getStatus, 1, 2);
        Long activeOrders = orderMapper.selectCount(activeWrapper);
        data.put("activeOrders", activeOrders);

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LambdaQueryWrapper<Order> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(Order::getCreateTime, todayStart);
        List<Order> todayOrders = orderMapper.selectList(todayWrapper);
        int todayRevenue = todayOrders.stream().mapToInt(Order::getTotalPrice).sum();
        data.put("todayRevenue", todayRevenue);

        LambdaQueryWrapper<Volunteer> volunteerWrapper = new LambdaQueryWrapper<>();
        Long volunteerCount = volunteerMapper.selectCount(volunteerWrapper);
        data.put("volunteerCount", volunteerCount);

        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        Long elderCount = userMapper.selectCount(userWrapper);
        data.put("elderCount", elderCount);

        data.put("pointsIssued", 0);

        return data;
    }

    @Override
    @RedisCache(prefix = "admin:dashboard:trend:7d", expire = 30, nullExpire = 5)
    public List<Integer> getOrderTrend7d() {
        List<Integer> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime dayStart = LocalDate.now().minusDays(i).atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);

            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
            wrapper.between(Order::getCreateTime, dayStart, dayEnd);
            Long count = orderMapper.selectCount(wrapper);
            trend.add(count.intValue());
        }
        return trend;
    }

    @Override
    public List<Map<String, Object>> getServiceTypeDist() {
        List<Map<String, Object>> dist = new ArrayList<>();

        String[] serviceTypes = {"代购服务", "助洁服务", "助餐服务", "助医服务", "陪伴服务"};

        for (int i = 0; i < 5; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", serviceTypes[i]);
            item.put("value", 0);
            dist.add(item);
        }

        return dist;
    }
}
