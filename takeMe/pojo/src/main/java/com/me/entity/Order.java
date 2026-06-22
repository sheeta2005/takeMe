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
@TableName("`order`")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private Long userId;
    private String volunteerIds;

    private Integer totalPrice;
    private String serviceDate;
    private String serviceTime;
    private String address;
    private String remark;

    /**
     * 0: 待接单 - 刚创建或所有服务项都被放弃
     * 1: 已接单 - 至少有一个服务项被接单
     * 2: 服务中 - 至少有一个服务项正在服务中
     * 3: 待确认 - 所有服务项都已完成(状态≥3)
     * 4: 已完成 - 用户确认完成
     * 5: 已取消 - 用户/系统取消
     * 6: 模拟未支付
     */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime completeTime;
    private Integer isReviewed;
}