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
    //分页查询用户订单
    IPage<OrderVO> getMyOrderList(Long userId, Integer status, String orderNo, PageResultDTO pageResultDTO);

    //查询用户订单详情
    OrderVO getOrderDetail(Long userId, Long orderId);

    //创建订单及服务项
    OrderVO createOrder(Long userId, OrderDTO orderDTO, List<OrderItemDTO> itemDTOList);
    //停用志愿者时取消已接服务
    boolean releaseAssignedItem(Long volunteerId, Long orderItemId);

    //用户取消订单
    void cancelOrder(Long userId, Long orderId);

    //用户确认订单完成
    void confirmOrder(Long userId, Long orderId);

    //提交或修改服务评价
    void evaluateOrderItem(Long userId, Long orderItemId, Integer rating, String comment);

    //用户确认开始服务
    void userStartService(Long userId, Long orderItemId);

    //用户取消单项服务
    void cancelOrderItem(Long userId, Long orderItemId);

    //取消超时未支付订单
    void expireUnpaidOrder(Long orderId);

    //取消订单内超时未接服务
    void expirePendingItems(Long orderId);

    //取消超时未接的单项服务
    void expirePendingItem(Long orderItemId);

    //取消超时未开始的服务
    void expireAcceptedItem(Long orderItemId, Long volunteerId);

    //志愿者开始服务
    void volunteerStartService(Long volunteerId, Long orderItemId);

    //分页查询志愿者服务
    IPage<OrderVO> getVolunteerOrderList(Long volunteerId, Integer status, String orderNo, PageResultDTO pageResultDTO);

    //查询志愿者订单详情
    OrderVO getVolunteerOrderDetail(Long volunteerId, Long orderId);

    //志愿者接取服务
    void volunteerConfirmOrder(Long volunteerId, Long orderItemId);

    //志愿者放弃服务并扣除积分
    void volunteerAbandonOrder(Long volunteerId, Long orderItemId);

    //志愿者完成服务并结算积分
    void volunteerCompleteOrder(Long volunteerId, Long orderItemId);

    //分页查询可接取服务
    IPage<OrderVO> getAvailableOrderList(PageResultDTO pageResultDTO);

    //分页查询管理端订单
    IPage<Order> getAdminOrderPage(Integer status, PageResultDTO pageResultDTO);

    //按条件查询管理端订单
    IPage<Order> searchAdminOrder(
            Integer status, String orderNo, Long userId, String userName, Long volunteerId, String volunteerName, Integer serviceType, String startDate, String endDate, PageResultDTO pageResultDTO);

    //查询管理端订单记录
    Order getAdminOrderDetail(Long id);

    //查询管理端订单及服务详情
    OrderVO getAdminOrderDetailVO(Long id);

    //管理员确认订单完成
    boolean adminCompleteOrder(Long id);

    //管理员取消订单
    boolean adminCancelOrder(Long id);

    //管理员取消单项服务
    boolean adminCancelOrderItem(Long orderItemId);

    //按条件统计订单数量
    Long countOrders(LambdaQueryWrapper<Order> wrapper);
}
