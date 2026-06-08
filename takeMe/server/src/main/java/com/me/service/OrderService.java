package com.me.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.dto.OrderDTO;
import com.me.dto.OrderItemDTO;
import com.me.entity.Order;
import com.me.vo.OrderVO;
import java.util.List;

public interface OrderService {

    Page<OrderVO> getMyOrderList(Long userId, Integer page, Integer pageSize, Integer status, String orderNo);

    OrderVO getOrderDetail(Long userId, Long orderId);

    OrderVO createOrder(Long userId, OrderDTO orderDTO, List<OrderItemDTO> itemDTOList);

    void cancelOrder(Long userId, Long orderId);

    void confirmOrder(Long userId, Long orderId);

    void evaluateOrder(Long userId, Long orderId, Integer rating, String comment);

    void userStartService(Long userId, Long orderItemId);

    Page<OrderVO> getVolunteerOrderList(Long volunteerId, Integer page, Integer pageSize, Integer status, String orderNo);

    OrderVO getVolunteerOrderDetail(Long volunteerId, Long orderId);

    void volunteerConfirmOrder(Long volunteerId, Long orderItemId);

    void volunteerAbandonOrder(Long volunteerId, Long orderItemId);

    void volunteerCompleteOrder(Long volunteerId, Long orderItemId);
    
    Page<OrderVO> getAvailableOrderList(Integer page, Integer pageSize);
    
    Page<Order> getAdminOrderPage(Integer page, Integer pageSize, Integer status);
    
    Page<Order> searchAdminOrder(
        Integer page,
        Integer pageSize,
        Integer status,
        String orderNo,
        Long userId,
        String userName,
        Long volunteerId,
        String volunteerName,
        Integer serviceType,
        String startDate,
        String endDate
    );
    
    Order getAdminOrderDetail(Long id);
    
    boolean adminCancelOrder(Long id);
    
    boolean adminCompleteOrder(Long id);
    
    Long countOrders(LambdaQueryWrapper<Order> wrapper);
}