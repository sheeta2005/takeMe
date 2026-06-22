package com.me.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "支付请求参数")
public class PaymentDTO {

    @Schema(description = "订单ID", example = "1001", required = true)
    private Long orderId;
}
