package com.me.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long volunteerId;

    private Integer totalPrice;
    private String serviceDate;
    private String serviceTime;
    private String address;
    private String remark;

    private Integer status;
    private Integer isReviewed;
}