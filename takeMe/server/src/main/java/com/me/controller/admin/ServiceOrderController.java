package com.me.controller.admin;

import com.me.dto.OrderApproveDTO;
import com.me.dto.OrderCreateDTO;
import com.me.service.ServiceOrderService;
import com.me.vo.OrderDetailVO;
import com.me.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
public class ServiceOrderController {

    @Autowired
    private ServiceOrderService orderService;

    @PostMapping("/create")
    public ResultVO<Void> createOrder(@RequestBody OrderCreateDTO dto) {
        try {
            orderService.createOrder(dto);
            return ResultVO.success();
        } catch (Exception e) {
            return ResultVO.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    public ResultVO<List<OrderDetailVO>> getOrderList(
            @RequestParam Long userId,
            @RequestParam Integer role) {
        try {
            return ResultVO.success(orderService.getOrderList(userId, role));
        } catch (Exception e) {
            return ResultVO.error(e.getMessage());
        }
    }

    @PostMapping("/approve")
    public ResultVO<Void> approveOrder(@RequestBody OrderApproveDTO dto) {
        try {
            orderService.approveOrder(dto);
            return ResultVO.success();
        } catch (Exception e) {
            return ResultVO.error(e.getMessage());
        }
    }

    @PutMapping("/status/{orderId}")
    public ResultVO<Void> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam Integer status) {
        try {
            orderService.updateOrderStatus(orderId, status);
            return ResultVO.success();
        } catch (Exception e) {
            return ResultVO.error(e.getMessage());
        }
    }
}