package com.me.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.me.dto.OrderDTO;
import com.me.dto.OrderItemDTO;
import com.me.dto.PageResultDTO;
import com.me.entity.Order;
import com.me.vo.OrderVO;

import java.util.List;

public interface OrderService {
    IPage<OrderVO> getMyOrderList(Long userId, Integer status, String orderNo, PageResultDTO pageResultDTO);

    OrderVO getOrderDetail(Long userId, Long orderId);

    OrderVO createOrder(Long userId, OrderDTO orderDTO, List<OrderItemDTO> itemDTOList);

    void cancelOrder(Long userId, Long orderId);

    void confirmOrder(Long userId, Long orderId);

    void evaluateOrderItem(Long userId, Long orderItemId, Integer rating, String comment);

    void userStartService(Long userId, Long orderItemId);

    void cancelOrderItem(Long userId, Long orderItemId);

    void volunteerStartService(Long volunteerId, Long orderItemId);

    IPage<OrderVO> getVolunteerOrderList(Long volunteerId, Integer status, String orderNo, PageResultDTO pageResultDTO);

    OrderVO getVolunteerOrderDetail(Long volunteerId, Long orderId);

    void volunteerConfirmOrder(Long volunteerId, Long orderItemId);

    void volunteerAbandonOrder(Long volunteerId, Long orderItemId);

    void volunteerCompleteOrder(Long volunteerId, Long orderItemId);

    IPage<OrderVO> getAvailableOrderList(PageResultDTO pageResultDTO);

    IPage<Order> getAdminOrderPage(Integer status, PageResultDTO pageResultDTO);

    IPage<Order> searchAdminOrder(
            Integer status, String orderNo, Long userId, String userName, Long volunteerId, String volunteerName, Integer serviceType, String startDate, String endDate, PageResultDTO pageResultDTO);

    Order getAdminOrderDetail(Long id);

    OrderVO getAdminOrderDetailVO(Long id);

    boolean adminCompleteOrder(Long id);

    boolean adminCancelOrder(Long id);

    boolean adminCancelOrderItem(Long orderItemId);

    Long countOrders(LambdaQueryWrapper<Order> wrapper);
}