package com.me.service;

import com.me.dto.PaymentDTO;
import com.me.vo.PaymentResultVO;

public interface PaymentService {

    /**
     * 模拟支付
     * @param userId 用户ID
     * @param paymentDTO 支付请求参数
     * @return 支付结果
     */
    PaymentResultVO mockPayment(Long userId, PaymentDTO paymentDTO);

    /**
     * 取消订单并退款
     * @param userId 用户ID
     * @param orderId 订单ID
     */
    void cancelOrderWithRefund(Long userId, Long orderId);
}
