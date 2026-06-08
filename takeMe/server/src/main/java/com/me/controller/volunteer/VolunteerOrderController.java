package com.me.controller.volunteer;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.context.BaseContext;
import com.me.service.OrderService;
import com.me.vo.OrderVO;
import com.me.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/volunteer/order")
@RequiredArgsConstructor
public class VolunteerOrderController {

    private final OrderService orderService;

    @GetMapping("/list")
    public Result<Page<OrderVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderNo
    ) {
        Long volunteerId = BaseContext.getLoginId();
        Page<OrderVO> pageResult = orderService.getVolunteerOrderList(volunteerId, page, pageSize, status, orderNo);
        return Result.success(pageResult);
    }

    @GetMapping("/available")
    public Result<Page<OrderVO>> available(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        Page<OrderVO> pageResult = orderService.getAvailableOrderList(page, pageSize);
        return Result.success(pageResult);
    }

    @GetMapping("/detail")
    public Result<OrderVO> detail(@RequestParam Long orderId) {
        Long volunteerId = BaseContext.getLoginId();
        OrderVO orderVO = orderService.getVolunteerOrderDetail(volunteerId, orderId);
        return Result.success(orderVO);
    }

    @PostMapping("/confirm")
    public Result<Void> confirm(@RequestParam Long orderItemId) {
        Long volunteerId = BaseContext.getLoginId();
        orderService.volunteerConfirmOrder(volunteerId, orderItemId);
        return Result.success();
    }

    @PostMapping("/abandon")
    public Result<Void> abandon(@RequestParam Long orderItemId) {
        Long volunteerId = BaseContext.getLoginId();
        orderService.volunteerAbandonOrder(volunteerId, orderItemId);
        return Result.success();
    }

    @PostMapping("/complete")
    public Result<Void> complete(@RequestParam Long orderItemId) {
        Long volunteerId = BaseContext.getLoginId();
        orderService.volunteerCompleteOrder(volunteerId, orderItemId);
        return Result.success();
    }
}
