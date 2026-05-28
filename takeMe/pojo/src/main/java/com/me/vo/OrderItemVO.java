package com.me.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemVO {
    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private Integer productPrice;
    private Integer quantity;
    private Integer itemPrice;
    private LocalDateTime createTime;
}