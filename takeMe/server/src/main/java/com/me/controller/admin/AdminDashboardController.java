package com.me.controller.admin;

import com.me.result.Result;
import com.me.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboardData() {
        Map<String, Object> data = adminDashboardService.getDashboardData();
        return Result.success(data);
    }

    @GetMapping("/order/trend7d")
    public Result<List<Integer>> getOrderTrend7d() {
        List<Integer> trend = adminDashboardService.getOrderTrend7d();
        return Result.success(trend);
    }

    @GetMapping("/service/type/dist")
    public Result<List<Map<String, Object>>> getServiceTypeDist() {
        List<Map<String, Object>> dist = adminDashboardService.getServiceTypeDist();
        return Result.success(dist);
    }
}
