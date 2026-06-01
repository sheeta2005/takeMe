package com.me.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO {

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
    private LocalDateTime createTime;
    private LocalDateTime completeTime;
    private Integer isReviewed;

    private List<OrderItemVO> items;
}