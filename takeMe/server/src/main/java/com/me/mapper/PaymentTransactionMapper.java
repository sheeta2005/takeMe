package com.me.mapper;

import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.me.entity.PaymentTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;

@Mapper
public interface PaymentTransactionMapper extends BaseMapper<PaymentTransaction> {
    // 保留原支付金额，按实际支付/退款发生日统计模拟净收款。
    Long selectNetMockAmount(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 读取原模拟支付流水，保留已支付及已退款状态。
    default List<PaymentTransaction> selectMockPayments(Long orderId) {
        LambdaQueryWrapper<PaymentTransaction> paymentQuery = new LambdaQueryWrapper<>();
        paymentQuery.eq(PaymentTransaction::getOrderId, orderId)
                .eq(PaymentTransaction::getPaymentMethod, "mock")
                .in(PaymentTransaction::getPaymentStatus, 1, 2);
        return selectList(paymentQuery);
    }

    // 查询同订单待支付流水，保留原单条查询约束。
    default PaymentTransaction selectPendingByOrderId(Long orderId) {
        LambdaQueryWrapper<PaymentTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentTransaction::getOrderId, orderId);
        wrapper.eq(PaymentTransaction::getPaymentStatus, 0);
        return selectOne(wrapper);
    }

    // 按退款流水号检查是否已经登记。
    Long countByTransactionNo(@Param("transactionNo") String transactionNo);
}
