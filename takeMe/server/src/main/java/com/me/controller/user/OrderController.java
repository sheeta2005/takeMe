package com.me.controller.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.me.dto.PageResultDTO;
import com.me.result.Result;
import com.me.dto.OrderDTO;
import com.me.dto.OrderItemDTO;
import com.me.service.OrderService;
import com.me.utils.JwtUtil;
import com.me.vo.OrderVO;
import com.me.vo.PageResultVO;
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
    public Result<PageResultVO<OrderVO>> list(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderNo
    ) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        PageResultDTO pageResultDTO = new PageResultDTO();
        pageResultDTO.setPageNum(pageNum);
        pageResultDTO.setPageSize(pageSize);

        IPage<OrderVO> iPage = orderService.getMyOrderList(userId, status, orderNo, pageResultDTO);
        PageResultVO<OrderVO> result = PageResultVO.from(iPage);
        return Result.success(result);
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

    // ===================== 确认开始服务 =====================
    @PostMapping("/startService")
    public Result<Void> startService(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Long orderItemId
    ) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        orderService.userStartService(userId, orderItemId);
        return Result.success();
    }

    // ===================== 评价订单 =====================
    @PostMapping("/evaluate")
    public Result<Void> evaluate(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Long orderId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment
    ) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        orderService.evaluateOrder(userId, orderId, rating, comment);
        return Result.success();
    }
}