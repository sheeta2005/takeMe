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
@TableName("payment_transaction")
public class PaymentTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String transactionNo;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private Integer amount;
    private String paymentMethod;
    private Integer paymentStatus;
    private LocalDateTime paymentTime;
    private LocalDateTime refundTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String remark;
}
