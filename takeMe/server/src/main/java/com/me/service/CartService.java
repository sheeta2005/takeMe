package com.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.me.dto.CartItemDTO;
import com.me.entity.Cart;
import com.me.vo.CartItemVO;

import java.util.List;

public interface CartService extends IService<Cart> {

    /**
     * 获取当前用户的购物车所有条目
     */
    List<CartItemVO> getCartItemList(Long userId);

    /**
     * 加入购物车
     */
    void addItem(Long userId, CartItemDTO dto);

    /**
     * 修改购物车商品数量
     */
    void updateItemQuantity(Long userId, Long productId, Integer quantity);

    /**
     * 删除购物车单个商品
     */
    void deleteItem(Long userId, Long productId);

    /**
     * 清空购物车
     */
    void clearCart(Long userId);
}