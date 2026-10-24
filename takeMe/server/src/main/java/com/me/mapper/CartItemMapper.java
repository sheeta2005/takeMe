package com.me.mapper;

import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.me.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {

    // 按购物车、服务及已提供的预约时间查询重复项。
    default CartItem selectExisting(CartItem item) {
        LambdaQueryWrapper<CartItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CartItem::getCartId, item.getCartId()).eq(CartItem::getServiceId, item.getServiceId());

        if (item.getServiceDate() != null && !item.getServiceDate().isEmpty()) {
            wrapper.eq(CartItem::getServiceDate, item.getServiceDate());
        }
        if (item.getServiceTime() != null && !item.getServiceTime().isEmpty()) {
            wrapper.eq(CartItem::getServiceTime, item.getServiceTime());
        }

        return selectOne(wrapper);
    }

    // 读取购物车内服务。
    default List<CartItem> selectByCartId(Long cartId) {
        return selectList(new LambdaQueryWrapper<CartItem>().eq(CartItem::getCartId, cartId));
    }

    // 按购物车归属查询单项服务。
    default CartItem selectOwned(Long cartId, Long id) {
        return selectOne(new LambdaQueryWrapper<CartItem>().eq(CartItem::getCartId, cartId).eq(CartItem::getId, id));
    }

    // 删除当前购物车中的指定项。
    default int deleteOwned(Long cartId, Long id) {
        return delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getCartId, cartId).eq(CartItem::getId, id));
    }

    // 清空当前购物车服务。
    default int deleteByCartId(Long cartId) {
        return delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getCartId, cartId));
    }
}