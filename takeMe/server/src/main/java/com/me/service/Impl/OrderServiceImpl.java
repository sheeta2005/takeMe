package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.dto.OrderDTO;
import com.me.dto.OrderItemDTO;
import com.me.entity.Order;
import com.me.entity.OrderItem;
import com.me.mapper.OrderItemMapper;
import com.me.mapper.OrderMapper;
import com.me.service.OrderService;
import com.me.vo.OrderItemVO;
import com.me.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    // ===================== 订单列表 =====================
    @Override
    public Page<OrderVO> getMyOrderList(Long userId, Integer page, Integer pageSize, Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> orderPage = orderMapper.selectPage(new Page<>(page, pageSize), wrapper);
        Page<OrderVO> voPage = new Page<>();
        BeanUtils.copyProperties(orderPage, voPage);

        // ===================== 🔥 核心修改：查询订单明细并赋值 =====================
        List<OrderVO> records = orderPage.getRecords().stream().map(order -> {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);

            // 查询当前订单的明细
            LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(OrderItem::getOrderId, order.getId());
            List<OrderItem> orderItems = orderItemMapper.selectList(itemWrapper);

            // 转换为VO并设置
            List<OrderItemVO> itemVOList = orderItems.stream().map(item -> {
                OrderItemVO itemVO = new OrderItemVO();
                BeanUtils.copyProperties(item, itemVO);
                return itemVO;
            }).collect(Collectors.toList());
            vo.setItems(itemVOList);

            return vo;
        }).collect(Collectors.toList());
        // ===================== 🔥 修改结束 =====================

        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public Page<OrderVO> getVolunteerOrderList(Long volunteerId, Integer page, Integer pageSize, Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getVolunteerId, volunteerId);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> orderPage = orderMapper.selectPage(new Page<>(page, pageSize), wrapper);
        Page<OrderVO> voPage = new Page<>();
        BeanUtils.copyProperties(orderPage, voPage);

        List<OrderVO> records = orderPage.getRecords().stream().map(order -> {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);

            LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(OrderItem::getOrderId, order.getId());
            List<OrderItem> orderItems = orderItemMapper.selectList(itemWrapper);

            List<OrderItemVO> itemVOList = orderItems.stream().map(item -> {
                OrderItemVO itemVO = new OrderItemVO();
                BeanUtils.copyProperties(item, itemVO);
                return itemVO;
            }).collect(Collectors.toList());
            vo.setItems(itemVOList);

            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(records);
        return voPage;
    }

    // ===================== 订单详情（含明细） =====================
    @Override
    public OrderVO getOrderDetail(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new RuntimeException("订单不存在");
        }

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);

        // 封装订单明细
        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
        List<OrderItemVO> itemVOList = items.stream().map(item -> {
            OrderItemVO vo = new OrderItemVO();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).collect(Collectors.toList());

        // 前端展示需要，手动塞入明细
        orderVO.setItems(itemVOList);
        return orderVO;
    }

    @Override
    public OrderVO getVolunteerOrderDetail(Long volunteerId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getVolunteerId().equals(volunteerId)) {
            throw new RuntimeException("订单不存在");
        }

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);

        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
        List<OrderItemVO> itemVOList = items.stream().map(item -> {
            OrderItemVO vo = new OrderItemVO();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).collect(Collectors.toList());

        orderVO.setItems(itemVOList);
        return orderVO;
    }

    // ===================== 创建订单（事务） =====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(Long userId, OrderDTO orderDTO, List<OrderItemDTO> itemDTOList) {
        // 1. 构建主订单实体
        Order order = new Order();
        BeanUtils.copyProperties(orderDTO, order);
        order.setUserId(userId);
        order.setOrderNo(generateOrderNo());
        order.setStatus(0);
        order.setIsReviewed(0);
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);

        // 2. 构建订单明细
        List<OrderItem> itemList = itemDTOList.stream().map(dto -> {
            OrderItem item = new OrderItem();
            BeanUtils.copyProperties(dto, item);
            item.setOrderId(order.getId());
            item.setCreateTime(LocalDateTime.now());
            return item;
        }).collect(Collectors.toList());

        // 批量插入明细
        itemList.forEach(orderItemMapper::insert);

        // 3. 返回VO
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);
        return orderVO;
    }

    // ===================== 取消订单 =====================
    @Override
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != 0 && order.getStatus() != 1) {
            throw new RuntimeException("仅待接单/已接单可取消");
        }

        order.setStatus(5);
        orderMapper.updateById(order);
    }

    @Override
    public void volunteerConfirmOrder(Long volunteerId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getVolunteerId().equals(volunteerId)) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new RuntimeException("订单状态不允许接单");
        }

        order.setStatus(1);
        orderMapper.updateById(order);
    }

    @Override
    public void volunteerAbandonOrder(Long volunteerId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getVolunteerId().equals(volunteerId)) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != 1) {
            throw new RuntimeException("订单状态不允许放弃");
        }

        order.setStatus(0);
        orderMapper.updateById(order);
    }

    @Override
    public void volunteerCompleteOrder(Long volunteerId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getVolunteerId().equals(volunteerId)) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != 1) {
            throw new RuntimeException("订单状态不允许完成");
        }

        order.setStatus(2);
        orderMapper.updateById(order);
    }

    @Override
    public void confirmOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != 2) {
            throw new RuntimeException("订单状态不允许确认");
        }

        order.setStatus(3);
        orderMapper.updateById(order);
    }

    @Override
    public void evaluateOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != 3) {
            throw new RuntimeException("订单状态不允许评价");
        }

        order.setIsReviewed(1);
        orderMapper.updateById(order);
    }

    // ===================== 管理员功能 =====================
    @Override
    public Page<Order> getAdminOrderPage(Integer page, Integer pageSize, Integer status) {
        Page<Order> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        
        wrapper.orderByDesc(Order::getCreateTime);
        return orderMapper.selectPage(pageParam, wrapper);
    }
    
    @Override
    public Page<Order> searchAdminOrder(
            Integer page, Integer pageSize, Integer status,
            String orderNo, Long userId, String userName,
            Long volunteerId, String volunteerName, Integer serviceType,
            String startDate, String endDate
    ) {
        Page<Order> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            wrapper.like(Order::getOrderNo, orderNo);
        }
        if (userId != null) {
            wrapper.eq(Order::getUserId, userId);
        }
        if (volunteerId != null) {
            wrapper.eq(Order::getVolunteerId, volunteerId);
        }
        if (startDate != null && !startDate.trim().isEmpty()) {
            wrapper.ge(Order::getCreateTime, startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            wrapper.le(Order::getCreateTime, endDate + " 23:59:59");
        }
        
        wrapper.orderByDesc(Order::getCreateTime);
        return orderMapper.selectPage(pageParam, wrapper);
    }
    
    @Override
    public Order getAdminOrderDetail(Long id) {
        return orderMapper.selectById(id);
    }
    
    @Override
    public boolean adminCancelOrder(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return false;
        }
        
        order.setStatus(4);
        return orderMapper.updateById(order) > 0;
    }
    
    @Override
    public boolean adminCompleteOrder(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return false;
        }
        
        order.setStatus(2);
        return orderMapper.updateById(order) > 0;
    }
    
    @Override
    public Long countOrders(LambdaQueryWrapper<Order> wrapper) {
        return orderMapper.selectCount(wrapper);
    }

    // 生成订单号
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }
}