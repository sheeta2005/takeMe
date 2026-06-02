package com.me.controller.volunteer;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.context.BaseContext;
import com.me.result.Result;
import com.me.service.OrderService;
import com.me.vo.OrderVO;
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
            @RequestParam(required = false) Integer status
    ) {
        Long volunteerId = BaseContext.getLoginId();
        Page<OrderVO> pageResult = orderService.getVolunteerOrderList(volunteerId, page, pageSize, status);
        return Result.success(pageResult);
    }

    @GetMapping("/detail")
    public Result<OrderVO> detail(@RequestParam Long orderId) {
        Long volunteerId = BaseContext.getLoginId();
        OrderVO orderVO = orderService.getVolunteerOrderDetail(volunteerId, orderId);
        return Result.success(orderVO);
    }

    @PostMapping("/confirm")
    public Result<Void> confirm(@RequestParam Long orderId) {
        Long volunteerId = BaseContext.getLoginId();
        orderService.volunteerConfirmOrder(volunteerId, orderId);
        return Result.success();
    }

    @PostMapping("/abandon")
    public Result<Void> abandon(@RequestParam Long orderId) {
        Long volunteerId = BaseContext.getLoginId();
        orderService.volunteerAbandonOrder(volunteerId, orderId);
        return Result.success();
    }

    @PostMapping("/complete")
    public Result<Void> complete(@RequestParam Long orderId) {
        Long volunteerId = BaseContext.getLoginId();
        orderService.volunteerCompleteOrder(volunteerId, orderId);
        return Result.success();
    }
}
