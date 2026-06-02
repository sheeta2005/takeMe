package com.me.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private Long id;
    private Long orderId;
    private Long serviceId;
    private String serviceName;
    private Integer servicePrice;
    private Integer quantity;
    private Integer itemPrice;
    private Integer serviceType;
}