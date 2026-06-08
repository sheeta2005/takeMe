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

    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime completeTime;
    private Integer isReviewed;
}