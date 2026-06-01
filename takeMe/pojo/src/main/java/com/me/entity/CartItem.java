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
@TableName("cart_item")
public class CartItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long cartId;
    private Long productId;
    private String productName;
    private Integer productPrice;
    private Integer quantity;
    private Integer selected; // 0=否 1=是
    private LocalDateTime createTime;
    private Integer serviceType;//0 = 代购 1 = 助洁 2 = 助餐 3 = 助医 4 = 陪伴
}