package com.me.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.me.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

}