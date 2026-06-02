package com.me.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.dto.OrderDTO;
import com.me.dto.OrderItemDTO;
import com.me.vo.OrderVO;
import java.util.List;

public interface OrderService {

    Page<OrderVO> getMyOrderList(Long userId, Integer page, Integer pageSize, Integer status);

    OrderVO getOrderDetail(Long userId, Long orderId);

    OrderVO createOrder(Long userId, OrderDTO orderDTO, List<OrderItemDTO> itemDTOList);

    void cancelOrder(Long userId, Long orderId);

    void confirmOrder(Long userId, Long orderId);

    void evaluateOrder(Long userId, Long orderId);

    Page<OrderVO> getVolunteerOrderList(Long volunteerId, Integer page, Integer pageSize, Integer status);

    OrderVO getVolunteerOrderDetail(Long volunteerId, Long orderId);

    void volunteerConfirmOrder(Long volunteerId, Long orderId);

    void volunteerAbandonOrder(Long volunteerId, Long orderId);

    void volunteerCompleteOrder(Long volunteerId, Long orderId);
}