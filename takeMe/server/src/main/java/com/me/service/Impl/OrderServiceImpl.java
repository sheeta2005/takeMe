package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.dto.OrderDTO;
import com.me.dto.OrderItemDTO;
import com.me.entity.Order;
import com.me.entity.OrderItem;
import com.me.entity.Review;
import com.me.mapper.OrderItemMapper;
import com.me.mapper.OrderMapper;
import com.me.mapper.ReviewMapper;
import com.me.service.OrderService;
import com.me.vo.OrderItemVO;
import com.me.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ReviewMapper reviewMapper;

    @Override
    public Page<OrderVO> getMyOrderList(Long userId, Integer page, Integer pageSize, Integer status, String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            wrapper.like(Order::getOrderNo, orderNo);
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

    @Override
    public Page<OrderVO> getVolunteerOrderList(Long volunteerId, Integer page, Integer pageSize, Integer status, String orderNo) {
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getVolunteerId, volunteerId);
        if (status != null) {
            itemWrapper.eq(OrderItem::getItemStatus, status);
        }
        itemWrapper.orderByDesc(OrderItem::getCreateTime);

        Page<OrderItem> itemPage = orderItemMapper.selectPage(new Page<>(page, pageSize), itemWrapper);
        
        Page<OrderVO> voPage = new Page<>();
        BeanUtils.copyProperties(itemPage, voPage, "records");
        
        List<OrderVO> records = itemPage.getRecords().stream()
            .map(item -> {
                Order order = orderMapper.selectById(item.getOrderId());
                if (order == null) return null;
                
                OrderVO vo = new OrderVO();
                BeanUtils.copyProperties(order, vo);
                
                LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(OrderItem::getOrderId, order.getId());
                List<OrderItem> items = orderItemMapper.selectList(wrapper);
                
                List<OrderItemVO> itemVOList = items.stream().map(i -> {
                    OrderItemVO itemVO = new OrderItemVO();
                    BeanUtils.copyProperties(i, itemVO);
                    return itemVO;
                }).collect(Collectors.toList());
                
                vo.setItems(itemVOList);
                return vo;
            })
            .filter(vo -> vo != null)
            .collect(Collectors.toList());
        
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public Page<OrderVO> getAvailableOrderList(Integer page, Integer pageSize) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(OrderItem::getVolunteerId);
        wrapper.eq(OrderItem::getItemStatus, 0);
        wrapper.orderByDesc(OrderItem::getCreateTime);

        Page<OrderItem> itemPage = orderItemMapper.selectPage(new Page<>(page, pageSize), wrapper);
        Page<OrderVO> voPage = new Page<>();
        BeanUtils.copyProperties(itemPage, voPage, "records");

        List<OrderVO> records = itemPage.getRecords().stream()
            .map(item -> {
                Order order = orderMapper.selectById(item.getOrderId());
                if (order == null) return null;
                
                OrderVO vo = new OrderVO();
                BeanUtils.copyProperties(order, vo);
                
                LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
                itemWrapper.eq(OrderItem::getOrderId, order.getId());
                List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
                
                List<OrderItemVO> itemVOList = items.stream().map(i -> {
                    OrderItemVO itemVO = new OrderItemVO();
                    BeanUtils.copyProperties(i, itemVO);
                    return itemVO;
                }).collect(Collectors.toList());
                
                vo.setItems(itemVOList);
                return vo;
            })
            .filter(vo -> vo != null)
            .collect(Collectors.toList());
        
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public OrderVO getOrderDetail(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
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

    @Override
    public OrderVO getVolunteerOrderDetail(Long volunteerId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(Long userId, OrderDTO orderDTO, List<OrderItemDTO> itemDTOList) {
        if (itemDTOList == null || itemDTOList.isEmpty()) {
            throw new RuntimeException("订单商品不能为空");
        }

        int totalPrice = itemDTOList.stream()
                .mapToInt(item -> item.getItemPrice() != null ? item.getItemPrice() : 0)
                .sum();

        OrderItemDTO firstItem = itemDTOList.get(0);
        
        Order order = new Order();
        BeanUtils.copyProperties(orderDTO, order);
        order.setUserId(userId);
        order.setOrderNo(generateOrderNo());
        order.setTotalPrice(totalPrice);
        
        order.setServiceDate(firstItem.getServiceDate());
        order.setServiceTime(firstItem.getServiceTime());
        order.setAddress(firstItem.getAddress());
        order.setRemark(firstItem.getRemark());
        
        order.setStatus(0);
        order.setIsReviewed(0);
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);

        List<OrderItem> itemList = itemDTOList.stream().map(dto -> {
            OrderItem item = new OrderItem();
            BeanUtils.copyProperties(dto, item);
            item.setOrderId(order.getId());
            item.setCreateTime(LocalDateTime.now());
            item.setItemStatus(0);
            
            if (item.getItemPrice() == null && item.getServicePrice() != null && item.getQuantity() != null) {
                item.setItemPrice(item.getServicePrice() * item.getQuantity());
            }
            
            return item;
        }).collect(Collectors.toList());

        itemList.forEach(orderItemMapper::insert);

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);
        
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, order.getId());
        List<OrderItem> orderItems = orderItemMapper.selectList(itemWrapper);
        
        List<OrderItemVO> itemVOList = orderItems.stream().map(item -> {
            OrderItemVO itemVO = new OrderItemVO();
            BeanUtils.copyProperties(item, itemVO);
            return itemVO;
        }).collect(Collectors.toList());
        
        orderVO.setItems(itemVOList);
        
        return orderVO;
    }

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
    @Transactional(rollbackFor = Exception.class)
    public void volunteerConfirmOrder(Long volunteerId, Long orderItemId) {
        OrderItem item = orderItemMapper.selectById(orderItemId);
        if (item == null) {
            throw new RuntimeException("服务项目不存在");
        }
        if (item.getVolunteerId() != null || item.getItemStatus() != 0) {
            throw new RuntimeException("该服务项目已被接取");
        }

        item.setVolunteerId(volunteerId);
        item.setItemStatus(1);
        orderItemMapper.updateById(item);
        
        updateOrderVolunteerIds(item.getOrderId());
        updateOrderStatus(item.getOrderId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void volunteerAbandonOrder(Long volunteerId, Long orderItemId) {
        OrderItem item = orderItemMapper.selectById(orderItemId);
        if (item == null) {
            throw new RuntimeException("服务项目不存在");
        }
        if (!item.getVolunteerId().equals(volunteerId)) {
            throw new RuntimeException("无权操作此服务项目");
        }
        if (item.getItemStatus() != 1) {
            throw new RuntimeException("当前状态不允许放弃");
        }

        item.setVolunteerId(null);
        item.setItemStatus(0);
        orderItemMapper.updateById(item);
        
        updateOrderVolunteerIds(item.getOrderId());
        updateOrderStatus(item.getOrderId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void volunteerCompleteOrder(Long volunteerId, Long orderItemId) {
        OrderItem item = orderItemMapper.selectById(orderItemId);
        if (item == null) {
            throw new RuntimeException("服务项目不存在");
        }
        if (!item.getVolunteerId().equals(volunteerId)) {
            throw new RuntimeException("无权操作此服务项目");
        }
        if (item.getItemStatus() != 1) {
            throw new RuntimeException("当前状态不允许完成");
        }

        item.setItemStatus(2);
        orderItemMapper.updateById(item);
        
        checkAndCompleteOrder(item.getOrderId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void userStartService(Long userId, Long orderItemId) {
        OrderItem item = orderItemMapper.selectById(orderItemId);
        if (item == null) {
            throw new RuntimeException("服务项目不存在");
        }
        
        Order order = orderMapper.selectById(item.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此服务");
        }
        
        if (item.getItemStatus() != 1) {
            throw new RuntimeException("当前状态不允许开始服务");
        }

        item.setItemStatus(1);
        orderItemMapper.updateById(item);
        
        updateOrderStatus(item.getOrderId());
    }

    private void updateOrderVolunteerIds(Long orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        wrapper.isNotNull(OrderItem::getVolunteerId);
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        
        Set<Long> volunteerIdSet = items.stream()
            .map(OrderItem::getVolunteerId)
            .filter(id -> id != null)
            .collect(Collectors.toSet());
        
        Order order = orderMapper.selectById(orderId);
        if (order != null) {
            if (volunteerIdSet.isEmpty()) {
                order.setVolunteerIds(null);
            } else {
                order.setVolunteerIds(volunteerIdSet.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
            }
            orderMapper.updateById(order);
        }
    }

    private void updateOrderStatus(Long orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        
        if (items.isEmpty()) return;
        
        boolean allCompleted = items.stream().allMatch(item -> 
            item.getItemStatus() == 3
        );
        
        if (allCompleted) {
            Order order = orderMapper.selectById(orderId);
            if (order != null && order.getStatus() != 3) {
                order.setStatus(3);
                order.setCompleteTime(LocalDateTime.now());
                orderMapper.updateById(order);
                return;
            }
        }
        
        boolean anyPendingConfirm = items.stream().anyMatch(item -> 
            item.getItemStatus() == 2
        );
        
        boolean anyInProgress = items.stream().anyMatch(item -> 
            item.getItemStatus() == 1
        );
        
        Order order = orderMapper.selectById(orderId);
        if (order == null) return;
        
        if (anyPendingConfirm) {
            order.setStatus(2);
        } else if (anyInProgress) {
            order.setStatus(1);
        } else {
            order.setStatus(0);
        }
        
        orderMapper.updateById(order);
    }

    private void checkAndCompleteOrder(Long orderId) {
        updateOrderStatus(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != 2) {
            throw new RuntimeException("订单状态不允许确认");
        }

        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        wrapper.eq(OrderItem::getItemStatus, 2);
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        
        for (OrderItem item : items) {
            item.setItemStatus(3);
            orderItemMapper.updateById(item);
        }
        
        updateOrderStatus(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void evaluateOrder(Long userId, Long orderId, Integer rating, String comment) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != 2 && order.getStatus() != 3) {
            throw new RuntimeException("订单状态不允许评价");
        }

        LambdaQueryWrapper<Review> reviewWrapper = new LambdaQueryWrapper<>();
        reviewWrapper.eq(Review::getOrderId, orderId);
        Review existingReview = reviewMapper.selectOne(reviewWrapper);
        
        if (existingReview != null) {
            existingReview.setRating(rating);
            existingReview.setComment(comment);
            reviewMapper.updateById(existingReview);
        } else {
            Review review = new Review();
            review.setOrderId(orderId);
            review.setUserId(userId);
            review.setVolunteerId(order.getVolunteerIds() != null ? 
                Long.parseLong(order.getVolunteerIds().split(",")[0]) : null);
            review.setRating(rating);
            review.setComment(comment);
            review.setCreateTime(LocalDateTime.now());
            reviewMapper.insert(review);
            
            order.setIsReviewed(1);
            orderMapper.updateById(order);
        }
    }

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
            wrapper.apply("FIND_IN_SET({0}, volunteer_ids)", volunteerId);
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
        
        order.setStatus(5);
        return orderMapper.updateById(order) > 0;
    }
    
    @Override
    public boolean adminCompleteOrder(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return false;
        }
        
        order.setStatus(3);
        return orderMapper.updateById(order) > 0;
    }
    
    @Override
    public Long countOrders(LambdaQueryWrapper<Order> wrapper) {
        return orderMapper.selectCount(wrapper);
    }

    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }
}