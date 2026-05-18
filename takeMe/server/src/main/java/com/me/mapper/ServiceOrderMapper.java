package com.me.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.me.entity.ServiceOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ServiceOrderMapper extends BaseMapper<ServiceOrder> {
}