package com.me.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private Long serviceId;
    private String serviceName;
    private Integer servicePrice;
    private Integer quantity;
    private Integer itemPrice;

    private LocalDateTime createTime;
    private Integer serviceType;//0 = 代购 1 = 助洁 2 = 助餐 3 = 助医 4 = 陪伴

}