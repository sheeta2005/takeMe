package com.me.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemVO {

    private Long id;
    private Long cartId;
    private Long serviceId;
    private String serviceName;
    private Integer servicePrice;
    private Integer quantity;
    private Integer selected;
    private Integer serviceType;
    private String serviceDate;
    private String serviceTime;
    private String address;
    private String remark;
    private LocalDateTime createTime;
}