package com.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.me.dto.OrderApproveDTO;
import com.me.dto.OrderCreateDTO;
import com.me.entity.ServiceOrder;
import com.me.vo.OrderDetailVO;
import java.util.List;

public interface ServiceOrderService extends IService<ServiceOrder> {
    void createOrder(OrderCreateDTO dto);
    List<OrderDetailVO> getOrderList(Long userId, Integer role);
    void approveOrder(OrderApproveDTO dto);
    void updateOrderStatus(Long orderId, Integer status);
}