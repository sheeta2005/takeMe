package com.me.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "支付结果")
public class PaymentResultVO {

    @Schema(description = "流水号", example = "TXN202606220001")
    private String transactionNo;

    @Schema(description = "订单ID", example = "1001")
    private Long orderId;

    @Schema(description = "订单号", example = "ORD202606220001")
    private String orderNo;

    @Schema(description = "支付金额（分）", example = "5000")
    private Integer amount;

    @Schema(description = "支付状态（0-待支付 1-已支付）", example = "1")
    private Integer paymentStatus;

    @Schema(description = "支付时间", example = "2026-06-22 10:30:00")
    private LocalDateTime paymentTime;
}
