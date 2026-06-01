package com.me.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.dto.OrderDTO;
import com.me.dto.OrderItemDTO;
import com.me.vo.OrderVO;
import java.util.List;

public interface OrderService {

    // 分页获取订单列表
    Page<OrderVO> getMyOrderList(Long userId, Integer page, Integer pageSize, Integer status);

    // 获取订单详情（含明细）
    OrderVO getOrderDetail(Long userId, Long orderId);

    // 创建订单
    OrderVO createOrder(Long userId, OrderDTO orderDTO, List<OrderItemDTO> itemDTOList);

    // 取消订单
    void cancelOrder(Long userId, Long orderId);

    // 确认完成
    void confirmOrder(Long userId, Long orderId);

    // 评价订单
    void evaluateOrder(Long userId, Long orderId);
}