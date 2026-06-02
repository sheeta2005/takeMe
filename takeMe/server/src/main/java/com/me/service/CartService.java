package com.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.me.dto.CartItemDTO;
import com.me.entity.Cart;
import com.me.vo.CartItemVO;
import com.me.vo.OrderVO;

import java.util.List;

public interface CartService extends IService<Cart> {

    List<CartItemVO> getCartItemList(Long userId);

    void addItem(Long userId, CartItemDTO dto);

    void updateItemQuantity(Long userId, Long productId, Integer quantity);

    void deleteItem(Long userId, Long productId);

    void clearCart(Long userId);
    
    OrderVO checkout(Long userId);
}