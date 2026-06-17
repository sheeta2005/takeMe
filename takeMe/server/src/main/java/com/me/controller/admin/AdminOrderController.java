package com.me.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.me.dto.PageResultDTO;
import com.me.entity.Order;
import com.me.result.Result;
import com.me.service.OrderService;
import com.me.vo.OrderVO;
import com.me.vo.PageResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "管理员-订单管理", description = "订单查询、搜索、取消、完成等操作")
@RestController
@RequestMapping("/api/admin/order")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @Operation(summary = "分页查询订单", description = "按状态筛选订单列表，支持分页")
    @GetMapping("/page")
    public Result<PageResultVO<Order>> getOrderPage(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "订单状态（0-待支付 1-待审批 2-进行中 3-已完成 4-已取消 5-已拒绝）") @RequestParam(required = false) Integer status
    ) {
        PageResultDTO pageResultDTO = new PageResultDTO();
        pageResultDTO.setPageNum(pageNum);
        pageResultDTO.setPageSize(pageSize);
        
        IPage<Order> iPage = orderService.getAdminOrderPage(status, pageResultDTO);
        PageResultVO<Order> result = PageResultVO.from(iPage);
        return Result.success(result);
    }

    @Operation(summary = "高级搜索订单", description = "支持多条件组合搜索订单")
    @GetMapping("/search")
    public Result<PageResultVO<Order>> searchOrder(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "订单状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "订单号") @RequestParam(required = false) String orderNo,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "用户姓名") @RequestParam(required = false) String userName,
            @Parameter(description = "志愿者ID") @RequestParam(required = false) Long volunteerId,
            @Parameter(description = "志愿者姓名") @RequestParam(required = false) String volunteerName,
            @Parameter(description = "服务类型（1-代购 2-清洁 3-送餐 4-陪医）") @RequestParam(required = false) Integer serviceType,
            @Parameter(description = "开始日期", example = "2026-06-01") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期", example = "2026-06-30") @RequestParam(required = false) String endDate
    ) {
        PageResultDTO pageResultDTO = new PageResultDTO();
        pageResultDTO.setPageNum(pageNum);
        pageResultDTO.setPageSize(pageSize);
        
        IPage<Order> iPage = orderService.searchAdminOrder(
            status, orderNo, userId, userName, 
            volunteerId, volunteerName, serviceType, startDate, endDate, pageResultDTO
        );
        PageResultVO<Order> result = PageResultVO.from(iPage);
        return Result.success(result);
    }

    @Operation(summary = "查询订单详情", description = "根据订单ID获取完整订单信息（包含订单项）")
    @GetMapping("/detail/{id}")
    public Result<OrderVO> getOrderDetail(@Parameter(description = "订单ID", required = true) @PathVariable Long id) {
        OrderVO order = orderService.getAdminOrderDetailVO(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        return Result.success(order);
    }

    @Operation(summary = "取消订单", description = "管理员强制取消订单")
    @PostMapping("/cancel/{id}")
    public Result<Void> cancelOrder(@Parameter(description = "订单ID", required = true) @PathVariable Long id) {
        boolean success = orderService.adminCancelOrder(id);
        if (!success) {
            return Result.error("订单不存在");
        }
        return Result.success();
    }

    @Operation(summary = "完成订单", description = "管理员手动标记订单为已完成")
    @PostMapping("/complete/{id}")
    public Result<Void> completeOrder(@Parameter(description = "订单ID", required = true) @PathVariable Long id) {
        boolean success = orderService.adminCompleteOrder(id);
        if (!success) {
            return Result.error("订单不存在");
        }
        return Result.success();
    }

    @Operation(summary = "订单统计", description = "获取各状态订单数量统计")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getOrderStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        LambdaQueryWrapper<Order> allWrapper = new LambdaQueryWrapper<>();
        Long totalCount = orderService.countOrders(allWrapper);
        stats.put("totalCount", totalCount);
        
        for (int i = 0; i <= 5; i++) {
            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Order::getStatus, i);
            Long count = orderService.countOrders(wrapper);
            stats.put("status" + i + "Count", count);
        }
        
        return Result.success(stats);
    }
}
