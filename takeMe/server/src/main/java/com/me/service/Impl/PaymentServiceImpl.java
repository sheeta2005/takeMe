package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.dto.PaymentDTO;
import com.me.entity.Order;
import com.me.entity.PaymentTransaction;
import com.me.mapper.OrderMapper;
import com.me.mapper.PaymentTransactionMapper;
import com.me.mq.producer.MessageProducer;
import com.me.service.PaymentService;
import com.me.vo.PaymentResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentTransactionMapper paymentTransactionMapper;
    private final OrderMapper orderMapper;
    private final MessageProducer messageProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResultVO mockPayment(Long userId, PaymentDTO paymentDTO) {
        Long orderId = paymentDTO.getOrderId();

        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new RuntimeException("订单不存在");
        }

        if (order.getStatus() != 6) {
            throw new RuntimeException("订单状态不允许支付，仅未支付订单可支付");
        }

        LambdaQueryWrapper<PaymentTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentTransaction::getOrderId, orderId);
        wrapper.eq(PaymentTransaction::getPaymentStatus, 0);
        PaymentTransaction existingTransaction = paymentTransactionMapper.selectOne(wrapper);

        if (existingTransaction != null) {
            throw new RuntimeException("订单已有待支付流水，请勿重复支付");
        }

        String transactionNo = generateTransactionNo();

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionNo(transactionNo);
        transaction.setOrderId(orderId);
        transaction.setOrderNo(order.getOrderNo());
        transaction.setUserId(userId);
        transaction.setAmount(order.getTotalPrice());
        transaction.setPaymentMethod("mock");
        transaction.setPaymentStatus(0);
        transaction.setCreateTime(LocalDateTime.now());
        transaction.setUpdateTime(LocalDateTime.now());
        paymentTransactionMapper.insert(transaction);

        transaction.setPaymentStatus(1);
        transaction.setPaymentTime(LocalDateTime.now());
        transaction.setUpdateTime(LocalDateTime.now());
        paymentTransactionMapper.updateById(transaction);

        order.setStatus(0);
        orderMapper.updateById(order);

        log.info("模拟支付成功: orderId={}, transactionNo={}, amount={}",
                orderId, transactionNo, order.getTotalPrice());

        PaymentResultVO resultVO = new PaymentResultVO();
        resultVO.setTransactionNo(transactionNo);
        resultVO.setOrderId(orderId);
        resultVO.setOrderNo(order.getOrderNo());
        resultVO.setAmount(order.getTotalPrice());
        resultVO.setPaymentStatus(1);
        resultVO.setPaymentTime(transaction.getPaymentTime());

        return resultVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrderWithRefund(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new RuntimeException("订单不存在");
        }

        if (order.getStatus() == 5) {
            throw new RuntimeException("订单已取消");
        }

        LambdaQueryWrapper<PaymentTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentTransaction::getOrderId, orderId);
        wrapper.eq(PaymentTransaction::getPaymentStatus, 1);
        PaymentTransaction transaction = paymentTransactionMapper.selectOne(wrapper);

        if (transaction != null) {
            transaction.setPaymentStatus(2);
            transaction.setRefundTime(LocalDateTime.now());
            transaction.setUpdateTime(LocalDateTime.now());
            paymentTransactionMapper.updateById(transaction);

            log.info("订单取消，流水退款: orderId={}, transactionNo={}",
                    orderId, transaction.getTransactionNo());
        }

        order.setStatus(5);
        orderMapper.updateById(order);

        log.info("订单取消成功: orderId={}", orderId);
    }

    private String generateTransactionNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "TXN" + timestamp + uuid;
    }
}
