package com.me.service.Impl;

import com.me.mapper.ApprovalMapper;
import com.me.mapper.OrderItemMapper;
import com.me.mapper.OrderMapper;
import com.me.mapper.UserMapper;
import com.me.mapper.VolunteerMapper;
import com.me.mapper.PaymentTransactionMapper;
import com.me.mapper.VolunteerPointsRecordMapper;
import com.me.redis.annotation.RedisCache;
import com.me.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final VolunteerMapper volunteerMapper;
    private final ApprovalMapper approvalMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentTransactionMapper paymentTransactionMapper;
    private final VolunteerPointsRecordMapper pointsRecordMapper;

    //查询管理端概览数据
    @Override
    @RedisCache(prefix = "admin:dashboard:data", expire = 1, nullExpire = 2)
    public Map<String, Object> getDashboardData() {
        Map<String, Object> data = new HashMap<>();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Map<String, Object> counts = orderMapper.selectDashboardOrderCounts(todayStart, todayStart.plusDays(1));
        // JDBC 的 COUNT/SUM 数字类型不同，对外保持原 Long 指标契约。
        counts.forEach((key, value) -> data.put(key, ((Number) value).longValue()));
        Long activeOrders = (Long) data.get("activeOrders");

        Long pendingApprovalCount = approvalMapper.countPending();
        
        Long pendingCount = activeOrders + pendingApprovalCount;
        data.put("pendingCount", pendingCount);

        Long todayRevenue = paymentTransactionMapper.selectNetMockAmount(todayStart, todayStart.plusDays(1));
        data.put("todayRevenue", todayRevenue);

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        Long monthRevenue = paymentTransactionMapper.selectNetMockAmount(monthStart, monthStart.plusMonths(1));
        data.put("monthRevenue", monthRevenue);

        Long volunteerCount = volunteerMapper.countEnabled();
        data.put("volunteerCount", volunteerCount);

        Long elderCount = userMapper.countEnabled();
        data.put("elderCount", elderCount);

        data.put("pointsIssued", pointsRecordMapper.sumIssuedPoints());

        return data;
    }

    //统计近七日订单数量
    @Override
    @RedisCache(prefix = "admin:dashboard:trend:7d", expire = 30, nullExpire = 5)
    public List<Integer> getOrderTrend7d() {
        List<Integer> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime dayStart = LocalDate.now().minusDays(i).atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);

            Long count = orderMapper.countCreatedBetween(dayStart, dayEnd);
            trend.add(count.intValue());
        }
        return trend;
    }

    //统计近七日支付净额
    @Override
    @RedisCache(prefix = "admin:dashboard:amount:7d", expire = 30, nullExpire = 5)
    public List<Integer> getOrderAmountTrend7d() {
        List<Integer> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime dayStart = LocalDate.now().minusDays(i).atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);

            int amount = Math.toIntExact(paymentTransactionMapper.selectNetMockAmount(dayStart, dayEnd));
            trend.add(amount);
        }
        return trend;
    }

    //统计服务类型分布
    @Override
    @RedisCache(prefix = "admin:dashboard:service:dist", expire = 30, nullExpire = 5)
    public List<Map<String, Object>> getServiceTypeDist() {
        List<Map<String, Object>> dist = new ArrayList<>();

        String[] serviceTypes = {"代购服务", "助洁服务", "助餐服务", "助医服务", "陪伴服务"};

        for (int i = 0; i < 5; i++) {
            Long count = orderItemMapper.countByServiceType(i);
            
            Map<String, Object> item = new HashMap<>();
            item.put("name", serviceTypes[i]);
            item.put("value", count.intValue());
            dist.add(item);
        }

        return dist;
    }
}
