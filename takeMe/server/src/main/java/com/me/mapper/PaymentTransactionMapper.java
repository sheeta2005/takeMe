package com.me.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.me.entity.PaymentTransaction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentTransactionMapper extends BaseMapper<PaymentTransaction> {
}
