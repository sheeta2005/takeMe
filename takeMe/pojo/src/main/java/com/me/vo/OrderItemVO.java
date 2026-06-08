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
    private Long serviceId;
    private String serviceName;
    private Integer servicePrice;
    private Integer quantity;
    private Integer itemPrice;
    private Integer serviceType;
    
    private String serviceDate;
    private String serviceTime;
    private String address;
    private String remark;
    
    private Long volunteerId;
    private Integer itemStatus;
    
    private LocalDateTime createTime;
}