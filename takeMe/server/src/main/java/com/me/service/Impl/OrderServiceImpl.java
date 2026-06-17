package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.annotation.BizLog;
import com.me.dto.OrderDTO;
import com.me.dto.OrderItemDTO;
import com.me.dto.OrderStatusChangeMessage;
import com.me.dto.OrderTimeoutMessage;
import com.me.dto.PageResultDTO;
import com.me.entity.Message;
import com.me.entity.Order;
import com.me.entity.OrderItem;
import com.me.entity.Review;
import com.me.mapper.OrderItemMapper;
import com.me.mapper.OrderMapper;
import com.me.mapper.ReviewMapper;
import com.me.mq.config.RabbitMQConfig;
import com.me.mq.producer.MessageProducer;
import com.me.redis.annotation.RedisCache;
import com.me.redis.annotation.RedisLock;
import com.me.service.MessageService;
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
    private final MessageService messageService;
    private final MessageProducer messageProducer;

    @Override
    public IPage<OrderVO> getMyOrderList(Long userId, Integer status, String orderNo, PageResultDTO pageResultDTO) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            wrapper.like(Order::getOrderNo, orderNo);
        }
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> orderPage = orderMapper.selectPage(new Page<>(pageResultDTO.getPageNum(), pageResultDTO.getPageSize()), wrapper);

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

        Page<OrderVO> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public IPage<OrderVO> getVolunteerOrderList(Long volunteerId, Integer status, String orderNo, PageResultDTO pageResultDTO) {
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getVolunteerId, volunteerId);
        if (status != null) {
            itemWrapper.eq(OrderItem::getItemStatus, status);
        }
        itemWrapper.orderByDesc(OrderItem::getCreateTime);

        Page<OrderItem> itemPage = orderItemMapper.selectPage(new Page<>(pageResultDTO.getPageNum(), pageResultDTO.getPageSize()), itemWrapper);
        
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
        
        Page<OrderVO> voPage = new Page<>(itemPage.getCurrent(), itemPage.getSize(), itemPage.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public IPage<OrderVO> getAvailableOrderList(PageResultDTO pageResultDTO) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(OrderItem::getVolunteerId);
        wrapper.eq(OrderItem::getItemStatus, 0);
        wrapper.orderByDesc(OrderItem::getCreateTime);

        Page<OrderItem> itemPage = orderItemMapper.selectPage(new Page<>(pageResultDTO.getPageNum(), pageResultDTO.getPageSize()), wrapper);

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
        
        Page<OrderVO> voPage = new Page<>(itemPage.getCurrent(), itemPage.getSize(), itemPage.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    @RedisCache(prefix = "order:detail", keyArgs = {1}, expire = 120, nullExpire = 2)
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
    @RedisCache(prefix = "order:detail", keyArgs = {1}, expire = 120, nullExpire = 2)
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
    @RedisLock(prefix = "order:create:lock", keyArgs = {0}, timeout = 5)
    @Transactional(rollbackFor = Exception.class)
    @BizLog(value = "创建订单", logParams = true)
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

        sendMessage(userId, 2, 1, "订单已提交", "您的订单已成功提交，等待志愿者接单", order.getId());

        OrderTimeoutMessage timeoutMessage = new OrderTimeoutMessage(
            order.getId(), 
            order.getOrderNo(), 
            order.getUserId()
        );
        messageProducer.sendMessage(
            "order.exchange",
            "order.create",
            timeoutMessage
        );

        return orderVO;
    }

    @Override
    @BizLog(value = "取消订单", logParams = true)
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != 0 && order.getStatus() != 1) {
            throw new RuntimeException("仅待接单/已接单可取消");
        }

        Integer oldStatus = order.getStatus();
        order.setStatus(5);
        orderMapper.updateById(order);

        sendStatusChangeMessage(order, oldStatus, 5, "用户取消订单");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void volunteerConfirmOrder(Long volunteerId, Long orderItemId) {
        LambdaQueryWrapper<OrderItem> inProgressWrapper = new LambdaQueryWrapper<>();
        inProgressWrapper.eq(OrderItem::getVolunteerId, volunteerId);
        inProgressWrapper.in(OrderItem::getItemStatus, 1, 2);
        Long count = orderItemMapper.selectCount(inProgressWrapper);
        if (count > 0) {
            throw new RuntimeException("您有正在进行中的服务，请先完成当前服务");
        }

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
        Integer oldStatus = updateOrderStatus(item.getOrderId());

        Order order = orderMapper.selectById(item.getOrderId());
        if (order != null) {
            sendMessage(order.getUserId(), 2, 2, "服务已接单", "您的订单服务已被志愿者接取，请耐心等待服务", order.getId());
            sendStatusChangeMessage(order, oldStatus, order.getStatus(), "志愿者接单", volunteerId);
        }
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
        if (item.getItemStatus() != 1 && item.getItemStatus() != 2) {
            throw new RuntimeException("当前状态不允许放弃");
        }

        item.setVolunteerId(null);
        item.setItemStatus(0);
        int updateCount = orderItemMapper.updateById(item);
        
        if (updateCount == 0) {
            throw new RuntimeException("放弃服务失败，请重试");
        }
        
        updateOrderVolunteerIds(item.getOrderId());
        updateOrderStatus(item.getOrderId());

        Order order = orderMapper.selectById(item.getOrderId());
        if (order != null) {
            sendMessage(order.getUserId(), 2, 0, "志愿者已放弃服务", "您订单的志愿者已放弃服务，系统将重新安排接单", item.getOrderId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void volunteerStartService(Long volunteerId, Long orderItemId) {
        OrderItem item = orderItemMapper.selectById(orderItemId);
        if (item == null) {
            throw new RuntimeException("服务项目不存在");
        }
        
        if (!item.getVolunteerId().equals(volunteerId)) {
            throw new RuntimeException("无权操作此服务");
        }
        
        if (item.getItemStatus() != 1) {
            throw new RuntimeException("当前状态不允许开始服务");
        }

        item.setItemStatus(2);
        orderItemMapper.updateById(item);
        
        Integer oldStatus = updateOrderStatus(item.getOrderId());
        
        Order order = orderMapper.selectById(item.getOrderId());
        if (order != null) {
            sendStatusChangeMessage(order, oldStatus, order.getStatus(), "志愿者开始服务", volunteerId);
        }
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
        if (item.getItemStatus() != 2) {
            throw new RuntimeException("当前状态不允许完成");
        }

        item.setItemStatus(3);
        orderItemMapper.updateById(item);
        
        Integer oldStatus = checkAndCompleteOrder(item.getOrderId());

        Order order = orderMapper.selectById(item.getOrderId());
        if (order != null) {
            sendMessage(order.getUserId(), 2, 0, "服务已完成", "您的订单服务已完成，请前往确认", order.getId());
            sendStatusChangeMessage(order, oldStatus, order.getStatus(), "志愿者完成服务", volunteerId);
        }
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
        wrapper.ne(OrderItem::getItemStatus, 0);
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

    private Integer updateOrderStatus(Long orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        
        if (items.isEmpty()) return null;
        
        Order order = orderMapper.selectById(orderId);
        if (order == null) return null;
        
        Integer oldStatus = order.getStatus();
        
        boolean allCompleted = items.stream().allMatch(item -> 
            item.getItemStatus() == 4
        );
        
        if (allCompleted) {
            if (order.getStatus() != 4) {
                order.setStatus(4);
                order.setCompleteTime(LocalDateTime.now());
                orderMapper.updateById(order);
            }
            return oldStatus;
        }
        
        boolean anyPendingConfirm = items.stream().anyMatch(item -> 
            item.getItemStatus() == 3
        );
        
        boolean anyInProgress = items.stream().anyMatch(item -> 
            item.getItemStatus() == 2
        );
        
        boolean anyAccepted = items.stream().anyMatch(item -> 
            item.getItemStatus() == 1
        );
        
        if (anyPendingConfirm) {
            order.setStatus(3);
        } else if (anyInProgress) {
            order.setStatus(2);
        } else if (anyAccepted) {
            order.setStatus(1);
        } else {
            order.setStatus(0);
        }
        
        orderMapper.updateById(order);
        return oldStatus;
    }

    private Integer checkAndCompleteOrder(Long orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        
        if (items.isEmpty()) return null;
        
        Order order = orderMapper.selectById(orderId);
        if (order == null) return null;
        
        Integer oldStatus = order.getStatus();
        
        boolean allCompleted = items.stream().allMatch(item -> 
            item.getItemStatus() == 4
        );
        
        if (allCompleted) {
            if (order.getStatus() != 4) {
                order.setStatus(4);
                order.setCompleteTime(LocalDateTime.now());
                orderMapper.updateById(order);
            }
        }
        
        return oldStatus;
    }

    private void sendStatusChangeMessage(Order order, Integer oldStatus, Integer newStatus, String remark) {
        sendStatusChangeMessage(order, oldStatus, newStatus, remark, null);
    }

    private void sendStatusChangeMessage(Order order, Integer oldStatus, Integer newStatus, String remark, Long volunteerId) {
        if (oldStatus == null || oldStatus.equals(newStatus)) {
            return;
        }

        OrderStatusChangeMessage statusMessage = OrderStatusChangeMessage.builder()
            .orderId(order.getId())
            .orderNo(order.getOrderNo())
            .oldStatus(oldStatus)
            .newStatus(newStatus)
            .userId(order.getUserId())
            .volunteerId(volunteerId)
            .changeTime(LocalDateTime.now())
            .remark(remark)
            .build();

        try {
            messageProducer.sendMessage(
                RabbitMQConfig.ORDER_STATUS_FANOUT_EXCHANGE,
                "",
                statusMessage
            );
        } catch (Exception e) {

        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @BizLog(value = "志愿者接单", logParams = true)
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

            if (order.getVolunteerIds() != null && !order.getVolunteerIds().isEmpty()) {
                String[] ids = order.getVolunteerIds().split(",");
                for (String id : ids) {
                    try {
                        Long vid = Long.parseLong(id.trim());
                        sendMessage(vid, 1, 0, "收到新评价", "您的服务收到了用户的评价，评分：" + rating + " 星", orderId);
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }
        }
    }

    @Override
    public IPage<Order> getAdminOrderPage(Integer status, PageResultDTO pageResultDTO) {
        Page<Order> pageParam = new Page<>(pageResultDTO.getPageNum(), pageResultDTO.getPageSize());
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        
        wrapper.orderByDesc(Order::getCreateTime);
        return orderMapper.selectPage(pageParam, wrapper);
    }
    
    @Override
    public IPage<Order> searchAdminOrder(
            Integer status, String orderNo, Long userId, String userName,
            Long volunteerId, String volunteerName, Integer serviceType,
            String startDate, String endDate, PageResultDTO pageResultDTO
    ) {
        Page<Order> pageParam = new Page<>(pageResultDTO.getPageNum(), pageResultDTO.getPageSize());
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
    @RedisCache(prefix = "order:detail", keyArgs = {0}, expire = 120, nullExpire = 2)
    public OrderVO getAdminOrderDetailVO(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return null;
        }

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);

        List<OrderItem> items = orderItemMapper.selectByOrderId(id);
        List<OrderItemVO> itemVOList = items.stream().map(item -> {
            OrderItemVO vo = new OrderItemVO();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).collect(Collectors.toList());

        orderVO.setItems(itemVOList);
        return orderVO;
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

    private void sendMessage(Long receiverId, Integer receiverType, Integer type, 
                             String title, String content, Long relatedOrderId) {
        Message msg = new Message();
        msg.setReceiverId(receiverId);
        msg.setReceiverType(receiverType);
        msg.setType(type);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setIsRead(0);
        msg.setRelatedOrderId(relatedOrderId);
        msg.setCreateTime(LocalDateTime.now());
        messageService.sendMessage(msg);
    }
}