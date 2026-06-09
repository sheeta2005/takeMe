package com.me.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.me.dto.PageResultDTO;
import com.me.entity.Order;
import com.me.result.Result;
import com.me.service.OrderService;
import com.me.vo.OrderVO;
import com.me.vo.PageResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/order")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping("/page")
    public Result<PageResultVO<Order>> getOrderPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status
    ) {
        PageResultDTO pageResultDTO = new PageResultDTO();
        pageResultDTO.setPageNum(pageNum);
        pageResultDTO.setPageSize(pageSize);
        
        IPage<Order> iPage = orderService.getAdminOrderPage(status, pageResultDTO);
        PageResultVO<Order> result = PageResultVO.from(iPage);
        return Result.success(result);
    }

    @GetMapping("/search")
    public Result<PageResultVO<Order>> searchOrder(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) Long volunteerId,
            @RequestParam(required = false) String volunteerName,
            @RequestParam(required = false) Integer serviceType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
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

    @GetMapping("/detail/{id}")
    public Result<OrderVO> getOrderDetail(@PathVariable Long id) {
        OrderVO order = orderService.getAdminOrderDetailVO(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        return Result.success(order);
    }

    @PostMapping("/cancel/{id}")
    public Result<Void> cancelOrder(@PathVariable Long id) {
        boolean success = orderService.adminCancelOrder(id);
        if (!success) {
            return Result.error("订单不存在");
        }
        return Result.success();
    }

    @PostMapping("/complete/{id}")
    public Result<Void> completeOrder(@PathVariable Long id) {
        boolean success = orderService.adminCompleteOrder(id);
        if (!success) {
            return Result.error("订单不存在");
        }
        return Result.success();
    }

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
