package com.me.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.me.entity.Cart;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {

    // 按账号读取唯一购物车，锁由调用方事务持有。
    default Cart selectByUserId(Long userId) {
        return selectOne(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
    }
}