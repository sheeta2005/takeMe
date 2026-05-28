package com.me.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {

    private Long id;
    private Long cartId;
    private Long productId;
    private String productName;
    private Integer productPrice;
    private Integer quantity;
    private Integer selected;
}