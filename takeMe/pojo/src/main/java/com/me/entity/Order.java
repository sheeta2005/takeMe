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
@TableName("order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;        // 订单编号
    private Long userId;           // 老人ID
    private Long volunteerId;      // 志愿者ID

    private Integer totalPrice;    // 总金额
    private String serviceDate;    // 服务日期
    private String serviceTime;    // 服务时间
    private String address;        // 服务地址
    private String remark;         // 备注

    private Integer status;        // 订单状态
    private LocalDateTime createTime;
    private LocalDateTime completeTime;
    private Integer isReviewed;    // 是否评价 0/1
}