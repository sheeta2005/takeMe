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
    private Integer serviceType;//0=代购 1=助洁 2=助餐 3=助医 4=陪伴
    
    private String serviceDate;
    private String serviceTime;
    private String address;
    private String remark;

    private Long volunteerId;

    /**
     * 0: 待接单 - 初始状态或被志愿者放弃
     * 1: 已接单 - 志愿者已接单但未开始
     * 2: 服务中 - 志愿者已开始服务
     * 3: 待确认 - 志愿者已完成服务
     * 4: 已完成 - 用户确认完成
     * 5: 已放弃 - 志愿者放弃或订单取消
     */
    private Integer itemStatus;

    private LocalDateTime createTime;
}