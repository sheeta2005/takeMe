package com.me.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.me.entity.Review;

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

    // 用户订单按服务项返回评价状态及原内容，用于入口文案和修改回显。
    private Boolean reviewed;
    private Review review;
    
    private LocalDateTime createTime;
}
