package com.me.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.me.dto.CartItemDTO;
import com.me.entity.Cart;
import com.me.entity.CartItem;
import com.me.mapper.CartItemMapper;
import com.me.mapper.CartMapper;
import com.me.service.CartService;
import com.me.vo.CartItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

    private final CartItemMapper cartItemMapper;

    // 服务类型常量定义
    private static final int SERVICE_TYPE_MEAL = 2; // 助餐服务（唯一可叠加数量的类型）

    @Override
    public List<CartItemVO> getCartItemList(Long userId) {
        Cart cart = getOrCreateCart(userId);
        LambdaQueryWrapper<CartItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CartItem::getCartId, cart.getId());

        List<CartItem> itemList = cartItemMapper.selectList(wrapper);

        // 实体类转VO返回给前端
        return itemList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addItem(Long userId, CartItemDTO dto) {
        Cart cart = getOrCreateCart(userId);

        // DTO转实体类
        CartItem item = convertToEntity(dto);
        item.setCartId(cart.getId());

        // 检查购物车中是否已存在相同商品
        LambdaQueryWrapper<CartItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CartItem::getCartId, cart.getId())
                .eq(CartItem::getProductId, item.getProductId());
        CartItem existingItem = cartItemMapper.selectOne(wrapper);

        if (existingItem != null) {
            // 只有助餐服务(2)可以叠加数量
            if (existingItem.getServiceType() == SERVICE_TYPE_MEAL) {
                existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
                cartItemMapper.updateById(existingItem);
            } else {
                throw new RuntimeException("该服务只能预约1次，如需多个时间请重新下单");
            }
        } else {
            // 非助餐服务强制数量为1，不管前端传什么
            if (item.getServiceType() != SERVICE_TYPE_MEAL) {
                item.setQuantity(1);
            }
            item.setSelected(1); // 默认选中
            item.setCreateTime(LocalDateTime.now());
            cartItemMapper.insert(item);
        }

        // 更新购物车最后修改时间
        cart.setUpdateTime(LocalDateTime.now());
        this.updateById(cart);
    }

    @Override
    @Transactional
    public void updateItemQuantity(Long userId, Long productId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);

        LambdaQueryWrapper<CartItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CartItem::getCartId, cart.getId())
                .eq(CartItem::getProductId, productId);
        CartItem item = cartItemMapper.selectOne(wrapper);

        if (item == null) {
            throw new RuntimeException("购物车商品不存在");
        }

        // 只有助餐服务(2)可以修改数量
        if (item.getServiceType() != SERVICE_TYPE_MEAL) {
            throw new RuntimeException("该服务只能预约1次，无法修改数量");
        }

        // 数量不能小于1
        item.setQuantity(Math.max(1, quantity));
        cartItemMapper.updateById(item);

        cart.setUpdateTime(LocalDateTime.now());
        this.updateById(cart);
    }

    @Override
    @Transactional
    public void deleteItem(Long userId, Long productId) {
        Cart cart = getOrCreateCart(userId);

        LambdaQueryWrapper<CartItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CartItem::getCartId, cart.getId())
                .eq(CartItem::getProductId, productId);
        cartItemMapper.delete(wrapper);

        cart.setUpdateTime(LocalDateTime.now());
        this.updateById(cart);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);

        LambdaQueryWrapper<CartItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CartItem::getCartId, cart.getId());
        cartItemMapper.delete(wrapper);

        cart.setUpdateTime(LocalDateTime.now());
        this.updateById(cart);
    }

    /**
     * 私有工具方法：获取用户的购物车，不存在则自动创建
     */
    private Cart getOrCreateCart(Long userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        Cart cart = this.getOne(wrapper);

        if (cart == null) {
            cart = new Cart();
            cart.setUserId(userId);
            cart.setCreateTime(LocalDateTime.now());
            cart.setUpdateTime(LocalDateTime.now());
            this.save(cart);
        }

        return cart;
    }

    /**
     * DTO转实体类
     */
    private CartItem convertToEntity(CartItemDTO dto) {
        CartItem item = new CartItem();
        BeanUtils.copyProperties(dto, item);
        return item;
    }

    /**
     * 实体类转VO
     */
    private CartItemVO convertToVO(CartItem item) {
        CartItemVO vo = new CartItemVO();
        BeanUtils.copyProperties(item, vo);
        return vo;
    }
}