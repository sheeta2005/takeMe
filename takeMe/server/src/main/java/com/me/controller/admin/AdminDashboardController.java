package com.me.controller.admin;

import com.me.result.Result;
import com.me.service.AdminDashboardService;
import com.me.service.OnlineUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "管理员-仪表盘")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final OnlineUserService onlineUserService;

    @Operation(summary = "获取仪表盘信息")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboardData() {
        Map<String, Object> data = adminDashboardService.getDashboardData();
        return Result.success(data);
    }
    @Operation(summary = "获取订单七天趋势")
    @GetMapping("/order/trend7d")
    public Result<List<Integer>> getOrderTrend7d() {
        List<Integer> trend = adminDashboardService.getOrderTrend7d();
        return Result.success(trend);
    }
    @Operation(summary = "获取服务分布信息")
    @GetMapping("/service/type/dist")
    public Result<List<Map<String, Object>>> getServiceTypeDist() {
        List<Map<String, Object>> dist = adminDashboardService.getServiceTypeDist();
        return Result.success(dist);
    }
    @Operation(summary = "分类统计在线人数")
    @GetMapping("/online")
    public Result<Map<String, Object>> getOnlineStats() {
        Map<String, Object> stats = onlineUserService.getOnlineStats();
        return Result.success(stats);
    }
}
