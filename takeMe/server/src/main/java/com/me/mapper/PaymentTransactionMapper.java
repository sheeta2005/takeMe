package com.me.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.me.entity.PaymentTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;

@Mapper
public interface PaymentTransactionMapper extends BaseMapper<PaymentTransaction> {
    // 保留原支付金额，按实际支付/退款发生日统计模拟净收款。
    @Select("""
            SELECT COALESCE((
                SELECT SUM(amount) FROM payment_transaction
                WHERE payment_method = 'mock' AND payment_status IN (1,2)
                  AND payment_time >= #{start} AND payment_time < #{end}
            ), 0) - COALESCE((
                SELECT SUM(amount) FROM payment_transaction
                WHERE payment_method = 'mock_refund' AND payment_status = 2
                  AND refund_time >= #{start} AND refund_time < #{end}
            ), 0)
            """)
    Long selectNetMockAmount(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
