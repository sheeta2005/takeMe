package com.me.service;

import java.util.List;
import java.util.Map;

public interface AdminDashboardService {

    /**
     * 获取工作台统计数据
     */
    Map<String, Object> getDashboardData();

    /**
     * 获取近7天订单数量趋势
     */
    List<Integer> getOrderTrend7d();

    /**
     * 获取近7天订单金额趋势
     */
    List<Integer> getOrderAmountTrend7d();

    /**
     * 获取服务类型分布
     */
    List<Map<String, Object>> getServiceTypeDist();
}
