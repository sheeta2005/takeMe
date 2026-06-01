package com.me.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.result.Result;
import com.me.dto.OrderDTO;
import com.me.dto.OrderItemDTO;
import com.me.service.OrderService;
import com.me.utils.JwtUtil;
import com.me.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final JwtUtil jwtUtil;

    // ===================== 订单列表 =====================
    @GetMapping("/list")
    public Result<Page<OrderVO>> list(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status
    ) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        Page<OrderVO> pageResult = orderService.getMyOrderList(userId, page, pageSize, status);
        return Result.success(pageResult);
    }

    // ===================== 订单详情 =====================
    @GetMapping("/detail")
    public Result<OrderVO> detail(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Long orderId
    ) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        OrderVO orderVO = orderService.getOrderDetail(userId, orderId);
        return Result.success(orderVO);
    }

    // ===================== 创建订单 =====================
    @PostMapping("/create")
    public Result<OrderVO> create(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> params
    ) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);

        // 解析前端参数
        OrderDTO orderDTO = new OrderDTO();
        BeanUtils.copyProperties(params.get("order"), orderDTO);

        List<OrderItemDTO> itemList = (List<OrderItemDTO>) params.get("items");

        OrderVO orderVO = orderService.createOrder(userId, orderDTO, itemList);
        return Result.success(orderVO);
    }

    // ===================== 取消订单 =====================
    @PostMapping("/cancel")
    public Result<Void> cancel(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Long orderId
    ) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        orderService.cancelOrder(userId, orderId);
        return Result.success();
    }

    // ===================== 确认完成 =====================
    @PostMapping("/confirm")
    public Result<Void> confirm(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Long orderId
    ) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        orderService.confirmOrder(userId, orderId);
        return Result.success();
    }

    // ===================== 评价订单 =====================
    @PostMapping("/evaluate")
    public Result<Void> evaluate(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Long orderId
    ) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        orderService.evaluateOrder(userId, orderId);
        return Result.success();
    }
}