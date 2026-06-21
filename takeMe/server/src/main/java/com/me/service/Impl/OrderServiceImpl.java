package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.annotation.BizLog;
import com.me.dto.OrderDTO;
import com.me.dto.OrderItemDTO;
import com.me.dto.OrderStatusChangeMessage;
import com.me.dto.OrderTimeoutMessage;
import com.me.dto.VolunteerStartTimeoutMessage;
import com.me.dto.PageResultDTO;
import com.me.entity.*;
import com.me.mapper.OrderItemMapper;
import com.me.mapper.OrderMapper;
import com.me.mapper.ReviewMapper;
import com.me.mapper.UserMapper;
import com.me.mapper.VolunteerMapper;
import com.me.mapper.VolunteerPointsRecordMapper;
import com.me.mq.config.RabbitMQConfig;
import com.me.mq.producer.MessageProducer;
import com.me.redis.annotation.RedisCache;
import com.me.redis.annotation.RedisLock;
import com.me.service.MessageService;
import com.me.service.OrderService;
import com.me.vo.OrderItemVO;
import com.me.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final ReviewMapper reviewMapper;
    private final MessageProducer messageProducer;
    private final MessageService messageService;
    private final com.me.redis.utils.RedisUtil redisUtil;
    private final VolunteerMapper volunteerMapper;
    private final VolunteerPointsRecordMapper volunteerPointsRecordMapper;

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
        if (volunteerId == null) {
            throw new RuntimeException("志愿者ID不能为空");
        }
        
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
                
                if (order.getUserId() != null) {
                    User user = userMapper.selectById(order.getUserId());
                    if (user != null) {
                        vo.setUserName(user.getRealName());
                        vo.setUserPhone(user.getPhone());
                    }
                }
                
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
                
                // 填充用户信息
                if (order.getUserId() != null) {
                    User user = userMapper.selectById(order.getUserId());
                    if (user != null) {
                        vo.setUserName(user.getRealName());
                        vo.setUserPhone(user.getPhone());
                    }
                }
                
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
            .filter(vo -> {
                if (vo.getServiceDate() != null && vo.getServiceTime() != null) {
                    return com.me.utils.ServiceTimeValidator.isWithinVisibleRange(
                        vo.getServiceDate(), vo.getServiceTime()
                    );
                }
                return true;
            })
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
    @Transactional(rollbackFor = Exception.class)
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

        // 同步处理该订单下所有已分配志愿者的服务项目
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, orderId);
        itemWrapper.isNotNull(OrderItem::getVolunteerId);
        List<OrderItem> assignedItems = orderItemMapper.selectList(itemWrapper);
        
        for (OrderItem item : assignedItems) {
            // 清空志愿者ID，设置为已放弃状态（5）
            item.setVolunteerId(null);
            item.setItemStatus(5);
            orderItemMapper.updateById(item);
            
            // 发送通知给志愿者：订单已被用户取消
            sendMessageToVolunteer(item.getVolunteerId(), 1, 0, 
                "订单已取消", 
                "您接取的订单（订单号：" + order.getOrderNo() + "）已被用户取消，服务自动终止", 
                item.getId());
        }
        
        // 更新订单的志愿者ID列表
        updateOrderVolunteerIds(orderId);

        sendStatusChangeMessage(order, oldStatus, 5, "用户取消订单");
        
        redisUtil.deleteByPattern("order:detail:" + orderId);
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

        Volunteer volunteer = volunteerMapper.selectById(volunteerId);
        if (volunteer == null) {
            throw new RuntimeException("志愿者不存在");
        }
        if (volunteer.getPoints() != null && volunteer.getPoints() < 50) {
            throw new RuntimeException("积分不足（当前积分：" + volunteer.getPoints() + "），无法接单。需要至少50积分才能接单。");
        }

        com.me.utils.ServiceTimeValidator.validateCanAcceptOrder(
            item.getServiceDate(), item.getServiceTime()
        );

        item.setVolunteerId(volunteerId);
        item.setItemStatus(1);
        orderItemMapper.updateById(item);
        
        updateOrderVolunteerIds(item.getOrderId());
        Integer oldStatus = updateOrderStatus(item.getOrderId());

        redisUtil.deleteByPattern("order:detail:" + item.getOrderId());

        Order order = orderMapper.selectById(item.getOrderId());
        if (order != null) {
            sendMessage(order.getUserId(), 2, 2, "服务已接单", "您的订单服务已被志愿者接取，请耐心等待服务", order.getId());
            sendStatusChangeMessage(order, oldStatus, order.getStatus(), "志愿者接单", volunteerId);
            
            LocalDateTime serviceDateTime = LocalDateTime.of(
                LocalDate.parse(item.getServiceDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                LocalTime.parse(item.getServiceTime(), DateTimeFormatter.ofPattern("HH:mm"))
            );
            
            long delayMinutes = java.time.Duration.between(LocalDateTime.now(), serviceDateTime).toMinutes() - 10;
            
            if (delayMinutes > 0) {
                VolunteerStartTimeoutMessage timeoutMessage = VolunteerStartTimeoutMessage.builder()
                    .orderItemId(orderItemId)
                    .volunteerId(volunteerId)
                    .orderId(order.getId())
                    .orderNo(order.getOrderNo())
                    .build();
                
                messageProducer .sendMessage(
                    RabbitMQConfig.VOLUNTEER_START_TIMEOUT_EXCHANGE,
                    "volunteer.start.timeout.delay",
                    timeoutMessage
                );
            }
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

        Volunteer volunteer = volunteerMapper.selectById(volunteerId);
        if (volunteer != null) {
            int currentPoints = volunteer.getPoints() != null ? volunteer.getPoints() : 0;
            int deductPoints = 50;
            int actualDeduct = Math.min(deductPoints, currentPoints);
            int newPoints = currentPoints - actualDeduct;
            
            volunteer.setPoints(newPoints);
            volunteerMapper.updateById(volunteer);
            
            VolunteerPointsRecord record = new VolunteerPointsRecord();
            record.setVolunteerId(volunteerId);
            record.setOrderId(item.getOrderId());
            record.setPoints(-actualDeduct);
            record.setType(1);
            record.setDescription("放弃订单扣除" + actualDeduct + "积分");
            record.setCreateTime(LocalDateTime.now());
            volunteerPointsRecordMapper.insert(record);
            
            log.info("志愿者 {} 放弃订单，扣除积分：{}，剩余积分：{}", volunteerId, actualDeduct, newPoints);
        }

        item.setVolunteerId(null);
        item.setItemStatus(0);
        int updateCount = orderItemMapper.updateById(item);
        
        if (updateCount == 0) {
            throw new RuntimeException("放弃服务失败，请重试");
        }
        
        updateOrderVolunteerIds(item.getOrderId());
        updateOrderStatus(item.getOrderId());

        redisUtil.deleteByPattern("order:detail:" + item.getOrderId());

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

        com.me.utils.ServiceTimeValidator.validateCanStartService(
            item.getServiceDate(), item.getServiceTime()
        );

        item.setItemStatus(2);
        orderItemMapper.updateById(item);
        
        Integer oldStatus = updateOrderStatus(item.getOrderId());
        
        redisUtil.deleteByPattern("order:detail:" + item.getOrderId());
        
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
        
        addPointsForCompletedOrder(volunteerId, item);
        
        Integer oldStatus = checkAndCompleteOrder(item.getOrderId());

        redisUtil.deleteByPattern("order:detail:" + item.getOrderId());

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

        item.setItemStatus(2);
        orderItemMapper.updateById(item);
        
        Integer oldStatus = updateOrderStatus(item.getOrderId());
        
        redisUtil.deleteByPattern("order:detail:" + item.getOrderId());
        
        sendStatusChangeMessage(order, oldStatus, order.getStatus(), "用户确认开始服务");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrderItem(Long userId, Long orderItemId) {
        OrderItem item = orderItemMapper.selectById(orderItemId);
        if (item == null) {
            throw new RuntimeException("服务项目不存在");
        }
        
        Order order = orderMapper.selectById(item.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此服务");
        }
        
        if (item.getItemStatus() != 0 && item.getItemStatus() != 1) {
            throw new RuntimeException("当前状态不允许取消，仅待接单或已接单状态可取消");
        }
        
        Integer oldItemStatus = item.getItemStatus();
        item.setVolunteerId(null);
        item.setItemStatus(5);
        orderItemMapper.updateById(item);
        
        updateOrderVolunteerIds(item.getOrderId());
        
        boolean allCancelled = checkAllItemsCancelled(item.getOrderId());
        
        if (allCancelled) {
            order.setStatus(5);
            orderMapper.updateById(order);
            
            if (oldItemStatus == 1 && item.getVolunteerId() != null) {
                sendMessageToVolunteer(item.getVolunteerId(), 1, 0, 
                    "服务项已取消", 
                    "您接取的服务项（订单号：" + order.getOrderNo() + "）已被用户取消", 
                    item.getId());
            }
            
            sendStatusChangeMessage(order, order.getStatus(), 5, "所有服务项已取消，订单自动取消");
        } else {
            Integer oldOrderStatus = updateOrderStatus(item.getOrderId());
            
            if (oldItemStatus == 1 && item.getVolunteerId() != null) {
                sendMessageToVolunteer(item.getVolunteerId(), 1, 0, 
                    "服务项已取消", 
                    "您接取的服务项（订单号：" + order.getOrderNo() + "）已被用户取消", 
                    item.getId());
            }
            
            sendStatusChangeMessage(order, oldOrderStatus, order.getStatus(), "用户取消单项服务");
        }
        
        redisUtil.deleteByPattern("order:detail:" + item.getOrderId());
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
        
        boolean allPendingConfirm = items.stream().allMatch(item -> 
            item.getItemStatus() >= 3 && item.getItemStatus() != 5
        );
        
        if (allPendingConfirm) {
            if (order.getStatus() != 3) {
                order.setStatus(3);
                orderMapper.updateById(order);
            }
            return oldStatus;
        }
        
        boolean anyInProgress = items.stream().anyMatch(item -> 
            item.getItemStatus() == 2
        );
        
        boolean anyAccepted = items.stream().anyMatch(item -> 
            item.getItemStatus() == 1
        );
        
        if (anyInProgress) {
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

    private boolean checkAllItemsCancelled(Long orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        
        if (items.isEmpty()) {
            return true;
        }
        
        return items.stream().allMatch(item -> item.getItemStatus() == 5);
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
            log.info("订单状态变更消息发送成功: orderId={}, status={}→{}", 
                order.getId(), oldStatus, newStatus);
        } catch (Exception e) {
            log.error("订单状态变更消息发送失败: orderId={}, status={}→{}", 
                order.getId(), oldStatus, newStatus, e);
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
        if (order.getStatus() != 3) {
            throw new RuntimeException("订单状态不允许确认");
        }

        Integer oldStatus = order.getStatus();
        
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        
        for (OrderItem item : items) {
            if (item.getItemStatus() == 3) {
                item.setItemStatus(4);
                orderItemMapper.updateById(item);
            }
        }
        
        order.setStatus(4);
        order.setCompleteTime(LocalDateTime.now());
        orderMapper.updateById(order);

        sendStatusChangeMessage(order, oldStatus, 4, "用户确认服务完成");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void evaluateOrderItem(Long userId, Long orderItemId, Integer rating, String comment) {
        OrderItem item = orderItemMapper.selectById(orderItemId);
        if (item == null) {
            throw new RuntimeException("服务项目不存在");
        }
        
        Order order = orderMapper.selectById(item.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new RuntimeException("无权评价此服务");
        }
        
        if (item.getItemStatus() != 3 && item.getItemStatus() != 4) {
            throw new RuntimeException("服务未完成，无法评价");
        }
        
        LambdaQueryWrapper<Review> reviewWrapper = new LambdaQueryWrapper<>();
        reviewWrapper.eq(Review::getOrderItemId, orderItemId);
        Review existingReview = reviewMapper.selectOne(reviewWrapper);
        
        if (existingReview != null) {
            existingReview.setRating(rating);
            existingReview.setComment(comment);
            reviewMapper.updateById(existingReview);
        } else {
            Review review = new Review();
            review.setOrderId(item.getOrderId());
            review.setOrderItemId(orderItemId);
            review.setUserId(userId);
            review.setVolunteerId(item.getVolunteerId());
            review.setRating(rating);
            review.setComment(comment);
            review.setCreateTime(LocalDateTime.now());
            reviewMapper.insert(review);
            
            sendMessageToVolunteer(item.getVolunteerId(), 1, 0, 
                "收到新评价", 
                "您的服务（" + item.getServiceName() + "）收到了用户评价，评分：" + rating + " 星", 
                item.getId());
        }
    }

    private void autoEvaluateUnreviewedItems(Long orderId) {
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, orderId);
        itemWrapper.in(OrderItem::getItemStatus, 3, 4);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        
        for (OrderItem item : items) {
            if (item.getVolunteerId() == null) {
                continue;
            }
            
            LambdaQueryWrapper<Review> reviewWrapper = new LambdaQueryWrapper<>();
            reviewWrapper.eq(Review::getOrderItemId, item.getId());
            Long count = reviewMapper.selectCount(reviewWrapper);
            
            if (count == 0) {
                Review review = new Review();
                review.setOrderId(item.getOrderId());
                review.setOrderItemId(item.getId());
                review.setUserId(order.getUserId());
                review.setVolunteerId(item.getVolunteerId());
                review.setRating(5);
                review.setComment("系统默认好评");
                review.setCreateTime(LocalDateTime.now());
                reviewMapper.insert(review);
                
                sendMessageToVolunteer(item.getVolunteerId(), 1, 0, 
                    "收到新评价", 
                    "您的服务（" + item.getServiceName() + "）收到了用户评价，评分：5 星", 
                    item.getId());
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
        
        Integer oldStatus = order.getStatus();
        order.setStatus(5);
        int updateCount = orderMapper.updateById(order);
        
        if (updateCount > 0) {
            LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(OrderItem::getOrderId, id);
            List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
            
            for (OrderItem item : items) {
                if (item.getItemStatus() != 4 && item.getItemStatus() != 5) {
                    Long previousVolunteerId = item.getVolunteerId();
                    item.setVolunteerId(null);
                    item.setItemStatus(5);
                    orderItemMapper.updateById(item);
                    
                    if (previousVolunteerId != null) {
                        sendMessageToVolunteer(previousVolunteerId, 1, 0, 
                            "订单已取消", 
                            "管理员取消了订单（订单号：" + order.getOrderNo() + "），服务自动终止", 
                            item.getId());
                    }
                }
            }
            
            updateOrderVolunteerIds(id);
            sendStatusChangeMessage(order, oldStatus, 5, "管理员取消订单");
        }
        
        return updateCount > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adminCancelOrderItem(Long orderItemId) {
        OrderItem item = orderItemMapper.selectById(orderItemId);
        if (item == null) {
            return false;
        }
        
        if (item.getItemStatus() != 0 && item.getItemStatus() != 1) {
            return false;
        }
        
        Order order = orderMapper.selectById(item.getOrderId());
        if (order == null) {
            return false;
        }
        
        Integer oldItemStatus = item.getItemStatus();
        Long previousVolunteerId = item.getVolunteerId();
        
        item.setVolunteerId(null);
        item.setItemStatus(5);
        int updateCount = orderItemMapper.updateById(item);
        
        if (updateCount > 0) {
            updateOrderVolunteerIds(item.getOrderId());
            
            boolean allCancelled = checkAllItemsCancelled(item.getOrderId());
            
            if (allCancelled) {
                order.setStatus(5);
                orderMapper.updateById(order);
                
                if (oldItemStatus == 1 && previousVolunteerId != null) {
                    sendMessageToVolunteer(previousVolunteerId, 1, 0, 
                        "服务项已取消", 
                        "管理员取消了服务项（订单号：" + order.getOrderNo() + "）", 
                        item.getId());
                }
                
                sendStatusChangeMessage(order, order.getStatus(), 5, "所有服务项已取消，订单自动取消");
            } else {
                Integer oldOrderStatus = updateOrderStatus(item.getOrderId());
                
                if (oldItemStatus == 1 && previousVolunteerId != null) {
                    sendMessageToVolunteer(previousVolunteerId, 1, 0, 
                        "服务项已取消", 
                        "管理员取消了服务项（订单号：" + order.getOrderNo() + "）", 
                        item.getId());
                }
                
                sendStatusChangeMessage(order, oldOrderStatus, order.getStatus(), "管理员取消单项服务");
            }
        }
        
        return updateCount > 0;
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

    private void sendMessage(Long receiverId, Integer receiverType, Integer type, String title, String content, Long relatedOrderId) {
        com.me.entity.Message message = new com.me.entity.Message();
        message.setReceiverId(receiverId);
        message.setReceiverType(receiverType);
        message.setType(type);
        message.setTitle(title);
        message.setContent(content);
        message.setIsRead(0);
        message.setRelatedOrderId(relatedOrderId);
        message.setCreateTime(LocalDateTime.now());
        messageService.sendMessage(message);
    }

    private void sendMessageToVolunteer(Long volunteerId, Integer receiverType, Integer type, String title, String content, Long relatedOrderId) {
        if (volunteerId == null) {
            return;
        }
        sendMessage(volunteerId, receiverType, type, title, content, relatedOrderId);
    }

    private void addPointsForCompletedOrder(Long volunteerId, OrderItem item) {
        if (item.getItemPrice() == null || item.getItemPrice() <= 0) {
            log.warn("订单项 {} 的价格无效，跳过积分计算", item.getId());
            return;
        }

        int earnedPoints;
        if (item.getServiceType() != null && item.getServiceType() == 2) {
            earnedPoints = (int) Math.floor(item.getItemPrice() / 10.0);
        } else {
            earnedPoints = item.getItemPrice();
        }

        if (earnedPoints <= 0) {
            log.warn("订单项 {} 计算的积分为0或负数，跳过", item.getId());
            return;
        }

        Volunteer volunteer = volunteerMapper.selectById(volunteerId);
        if (volunteer == null) {
            log.error("志愿者 {} 不存在，无法增加积分", volunteerId);
            return;
        }

        int currentPoints = volunteer.getPoints() != null ? volunteer.getPoints() : 0;
        int newPoints = currentPoints + earnedPoints;
        
        volunteer.setPoints(newPoints);
        volunteerMapper.updateById(volunteer);
        
        VolunteerPointsRecord record = new VolunteerPointsRecord();
        record.setVolunteerId(volunteerId);
        record.setOrderId(item.getOrderId());
        record.setPoints(earnedPoints);
        record.setType(0);
        record.setDescription("完成" + item.getServiceName() + "服务获得" + earnedPoints + "积分");
        record.setCreateTime(LocalDateTime.now());
        volunteerPointsRecordMapper.insert(record);
        
        log.info("志愿者 {} 完成订单 {}，获得积分：{}，当前总积分：{}", volunteerId, item.getId(), earnedPoints, newPoints);
    }
}