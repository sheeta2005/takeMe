package com.me.controller.user;

import com.me.result.Result;
import com.me.dto.PaymentDTO;
import com.me.service.PaymentService;
import com.me.utils.JwtUtil;
import com.me.vo.PaymentResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/payment")
@RequiredArgsConstructor
@Tag(name = "支付管理", description = "订单支付相关接口")
public class PaymentController {

    private final PaymentService paymentService;
    private final JwtUtil jwtUtil;

    @PostMapping("/mock")
    @Operation(summary = "模拟支付", description = "对未支付订单进行模拟支付")
    public Result<PaymentResultVO> mockPayment(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PaymentDTO paymentDTO) {
        try {
            Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
            PaymentResultVO result = paymentService.mockPayment(userId, paymentDTO);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消订单并退款", description = "取消已支付订单并生成退款流水")
    public Result<Void> cancelOrder(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Long orderId) {
        try {
            Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
            paymentService.cancelOrderWithRefund(userId, orderId);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
