package com.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.me.dto.CartItemDTO;
import com.me.entity.Cart;
import com.me.vo.CartItemVO;
import com.me.vo.OrderVO;

import java.util.List;

public interface CartService extends IService<Cart> {

    //查询购物车服务
    List<CartItemVO> getCartItemList(Long userId);

    //添加服务到购物车
    void addItem(Long userId, CartItemDTO dto);

    //修改购物车餐饮数量
    void updateItemQuantity(Long userId, Long productId, Integer quantity);

    //删除购物车服务
    void deleteItem(Long userId, Long productId);

    //清空购物车
    void clearCart(Long userId);
    
    //结算购物车并创建订单
    OrderVO checkout(Long userId, String requestId);
}
