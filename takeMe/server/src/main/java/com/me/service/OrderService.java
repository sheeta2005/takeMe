package com.me.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.dto.OrderDTO;
import com.me.dto.OrderItemDTO;
import com.me.entity.Order;
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
    
    /**
     * 管理员分页查询订单
     * @param page 页码
     * @param pageSize 每页数量
     * @param status 订单状态
     * @return 分页结果
     */
    Page<Order> getAdminOrderPage(Integer page, Integer pageSize, Integer status);
    
    /**
     * 管理员搜索订单
     * @param page 页码
     * @param pageSize 每页数量
     * @param status 订单状态
     * @param orderNo 订单编号
     * @param userId 用户ID
     * @param userName 用户姓名（暂不支持，需关联查询）
     * @param volunteerId 志愿者ID
     * @param volunteerName 志愿者姓名（暂不支持，需关联查询）
     * @param serviceType 服务类型（暂不支持，需关联查询）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分页结果
     */
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
    
    /**
     * 管理员获取订单详情
     * @param id 订单ID
     * @return 订单信息
     */
    Order getAdminOrderDetail(Long id);
    
    /**
     * 管理员取消订单
     * @param id 订单ID
     * @return 是否成功
     */
    boolean adminCancelOrder(Long id);
    
    /**
     * 管理员完成订单
     * @param id 订单ID
     * @return 是否成功
     */
    boolean adminCompleteOrder(Long id);
    
    /**
     * 统计订单数量
     * @param wrapper 查询条件
     * @return 订单数量
     */
    Long countOrders(LambdaQueryWrapper<Order> wrapper);
}