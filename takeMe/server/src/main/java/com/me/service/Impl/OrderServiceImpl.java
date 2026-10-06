package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.annotation.BizLog;
import com.me.dto.OrderDTO;
import com.me.dto.OrderItemDTO;
import com.me.dto.OrderStatusChangeMessage;
import com.me.dto.PageResultDTO;
import com.me.entity.*;
import com.me.exception.OrderBusinessException;
import com.me.mapper.*;
import com.me.mq.config.RabbitMQConfig;
import com.me.service.MessageService;
import com.me.service.OrderService;
import com.me.service.OutboxService;
import com.me.vo.OrderItemVO;
import com.me.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
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
    private final OutboxService outboxService;
    private final MessageService messageService;
    private final VolunteerMapper volunteerMapper;
    private final VolunteerPointsRecordMapper volunteerPointsRecordMapper;
    private final ServicePackageMapper servicePackageMapper;
    private final PaymentTransactionMapper paymentTransactionMapper;

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
        wrapper.orderByDesc(Order::getCreateTime, Order::getId);

        Page<Order> orderPage = orderMapper.selectPage(new Page<>(pageResultDTO.getPageNum(), pageResultDTO.getPageSize()), wrapper);
        List<Long> orderIds = orderPage.getRecords().stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrder = orderIds.isEmpty() ? Map.of()
                : orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                        .in(OrderItem::getOrderId, orderIds).orderByAsc(OrderItem::getId))
                .stream().collect(Collectors.groupingBy(OrderItem::getOrderId));

        List<OrderVO> records = orderPage.getRecords().stream().map(order -> {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);

            List<OrderItem> orderItems = itemsByOrder.getOrDefault(order.getId(), List.of());

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
            throw new OrderBusinessException("志愿者ID不能为空");
        }

        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getVolunteerId, volunteerId);
        if (status != null) {
            itemWrapper.eq(OrderItem::getItemStatus, status);
        }
        itemWrapper.orderByDesc(OrderItem::getCreateTime, OrderItem::getId);

        Page<OrderItem> itemPage = orderItemMapper.selectPage(new Page<>(pageResultDTO.getPageNum(), pageResultDTO.getPageSize()), itemWrapper);

        List<OrderVO> records = buildVolunteerRecords(itemPage.getRecords(), false);

        Page<OrderVO> voPage = new Page<>(itemPage.getCurrent(), itemPage.getSize(), itemPage.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public IPage<OrderVO> getAvailableOrderList(PageResultDTO pageResultDTO) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(OrderItem::getVolunteerId);
        wrapper.eq(OrderItem::getItemStatus, 0);
        // 日期范围先缩小索引扫描；上下界分开比较，同样支持跨午夜，且不对列做 CONCAT。
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.plusHours(1);
        LocalDateTime end = now.plusHours(4);
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");
        String startDate = start.toLocalDate().toString();
        String endDate = end.toLocalDate().toString();
        wrapper.between(OrderItem::getServiceDate, startDate, endDate)
                .and(w -> w.gt(OrderItem::getServiceDate, startDate)
                        .or().ge(OrderItem::getServiceTime, start.format(timeFormat)))
                .and(w -> w.lt(OrderItem::getServiceDate, endDate)
                        .or().le(OrderItem::getServiceTime, end.format(timeFormat)));
        wrapper.inSql(OrderItem::getOrderId, "SELECT id FROM `order` WHERE status IN (0, 1, 2)");
        wrapper.orderByDesc(OrderItem::getCreateTime, OrderItem::getId);

        Page<OrderItem> itemPage = orderItemMapper.selectPage(new Page<>(pageResultDTO.getPageNum(), pageResultDTO.getPageSize()), wrapper);

        List<OrderVO> records = buildVolunteerRecords(itemPage.getRecords(), true);

        Page<OrderVO> voPage = new Page<>(itemPage.getCurrent(), itemPage.getSize(), itemPage.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    private List<OrderVO> buildVolunteerRecords(List<OrderItem> items, boolean availableOnly) {
        if (items.isEmpty()) return List.of();
        // 两个志愿者列表共用本页批量装配，父订单和老人各只查一次。
        List<Long> orderIds = items.stream().map(OrderItem::getOrderId).distinct().toList();
        Map<Long, Order> orders = orderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, order -> order));
        List<Long> userIds = orders.values().stream().map(Order::getUserId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, User> users = userIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        return items.stream()
                .map(item -> {
                    Order order = orders.get(item.getOrderId());
                    if (order == null) return null;

                    if (availableOnly && (order.getStatus() == null || order.getStatus() < 0 || order.getStatus() > 2)) {
                        return null;
                    }

                    OrderVO vo = new OrderVO();
                    BeanUtils.copyProperties(order, vo);
                    vo.setServiceDate(null);
                    vo.setServiceTime(null);
                    vo.setAddress(null);
                    vo.setRemark(null);

                    // 填充用户信息
                    if (order.getUserId() != null) {
                        User user = users.get(order.getUserId());
                        if (user != null) {
                            vo.setUserName(user.getRealName());
                            vo.setUserPhone(user.getPhone());
                        }
                    }

                    // 一条可接单记录仅对应一个服务项，预约时间以服务项为准。
                    OrderItemVO itemVO = new OrderItemVO();
                    BeanUtils.copyProperties(item, itemVO);
                    vo.setItems(List.of(itemVO));
                    return vo;
                })
                .filter(vo -> vo != null)
                .collect(Collectors.toList());
    }

    @Override
    public OrderVO getOrderDetail(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new OrderBusinessException("订单不存在");
        }

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);

        LambdaQueryWrapper<OrderItem> orderItemWrapper = new LambdaQueryWrapper<>();
        orderItemWrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(orderItemWrapper);
        // List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
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
            throw new OrderBusinessException("订单不存在");
        }
        LambdaQueryWrapper<OrderItem> assigned = new LambdaQueryWrapper<>();
        assigned.eq(OrderItem::getOrderId, orderId).eq(OrderItem::getVolunteerId, volunteerId);
        if (orderItemMapper.selectCount(assigned) == 0) {
            throw new OrderBusinessException("无权查看此订单");
        }

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);
        orderVO.setServiceDate(null);
        orderVO.setServiceTime(null);
        orderVO.setAddress(null);
        orderVO.setRemark(null);

        LambdaQueryWrapper<OrderItem> orderItemWrapper = new LambdaQueryWrapper<>();
        orderItemWrapper.eq(OrderItem::getOrderId, orderId);
        orderItemWrapper.eq(OrderItem::getVolunteerId, volunteerId);
        List<OrderItem> items = orderItemMapper.selectList(orderItemWrapper);
        //List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
        List<OrderItemVO> itemVOList = items.stream().map(item -> {
            OrderItemVO vo = new OrderItemVO();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).collect(Collectors.toList());

        orderVO.setItems(itemVOList);
        return orderVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @BizLog(value = "创建订单", logParams = false)
    public OrderVO createOrder(Long userId, OrderDTO orderDTO, List<OrderItemDTO> itemDTOList) {
        if (orderDTO == null || orderDTO.getRequestId() == null
                || !orderDTO.getRequestId().matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("提交标识缺失或格式错误");
        }
        // 先锁账号再查幂等键；并发重试只能返回第一次提交的订单。
        User user = userMapper.selectForUpdate(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new IllegalArgumentException("账号不存在或已停用");
        }
        Order existing = orderMapper.selectByRequestForUpdate(userId, orderDTO.getRequestId());
        if (existing != null) return getOrderDetail(userId, existing.getId());
        if (itemDTOList == null || itemDTOList.isEmpty()) {
            throw new OrderBusinessException("订单商品不能为空");
        }

        int totalPrice = 0;
        for (OrderItemDTO item : itemDTOList) {
            ServicePackage service = item.getServiceId() == null ? null
                    : servicePackageMapper.selectById(item.getServiceId());
            if (service == null || service.getStatus() == null || service.getStatus() != 1
                    || service.getPrice() == null || service.getPrice() < 0
                    || item.getQuantity() == null || item.getQuantity() < 1
                    || (service.getType() != null && service.getType() != 2 && item.getQuantity() != 1)) {
                throw new IllegalArgumentException("服务已下架或数量无效");
            }
            if (item.getServiceDate() == null || item.getServiceTime() == null
                    || item.getAddress() == null || item.getAddress().isBlank()) {
                throw new IllegalArgumentException("预约时间和地址不能为空");
            }
            try {
                LocalDateTime appointment = LocalDateTime.of(
                        LocalDate.parse(item.getServiceDate()),
                        LocalTime.parse(item.getServiceTime()));
                // 接单最晚在预约前一小时结束，新订单不能创建无法接取的服务。
                if (!appointment.isAfter(LocalDateTime.now().plusHours(1))) {
                    throw new IllegalArgumentException("预约时间至少需晚于当前时间一小时");
                }
            } catch (java.time.format.DateTimeParseException e) {
                throw new IllegalArgumentException("预约时间格式错误", e);
            }
            item.setServiceName(service.getName());
            item.setServiceType(service.getType());
            item.setServicePrice(service.getPrice());
            item.setItemPrice(Math.multiplyExact(service.getPrice(), item.getQuantity()));
            totalPrice = Math.addExact(totalPrice, item.getItemPrice());
        }

        OrderItemDTO firstItem = itemDTOList.get(0);

        Order order = new Order();
        // 主键、金额、状态和归属不接收客户端覆盖。
        order.setRequestId(orderDTO.getRequestId());
        order.setUserId(userId);
        order.setOrderNo(generateOrderNo());
        order.setTotalPrice(totalPrice);

        order.setServiceDate(firstItem.getServiceDate());
        order.setServiceTime(firstItem.getServiceTime());
        order.setAddress(firstItem.getAddress());
        order.setRemark(firstItem.getRemark());

        order.setStatus(6);
        order.setIsReviewed(0);
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);

        List<OrderItem> itemList = itemDTOList.stream().map(dto -> {
            OrderItem item = new OrderItem();
            BeanUtils.copyProperties(dto, item);
            item.setId(null);
            item.setOrderId(order.getId());
            item.setCreateTime(LocalDateTime.now());
            item.setItemStatus(0);

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

        sendMessage(userId, 2, 1, "订单已创建", "请先完成模拟支付，支付后进入待接单", order.getId());

        // 超时统一由数据库定时扫描按预约时间判断，不再生产固定 TTL 消息。

        return orderVO;
    }

    @Override
    @BizLog(value = "取消订单", logParams = false)
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectForUpdate(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new OrderBusinessException("订单不存在");
        }
        if (order.getStatus() != 0 && order.getStatus() != 1 && order.getStatus() != 6) {
            throw new OrderBusinessException("仅未支付、待接单或已接单订单可取消");
        }
        cancelAllItems(order, "用户取消订单");
    }


    //rc
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void volunteerConfirmOrder(Long volunteerId, Long orderItemId) {
        // 先锁志愿者，再锁订单及服务项；读已提交避免空活动项查询的间隙锁。
        Volunteer volunteer = volunteerMapper.selectForUpdate(volunteerId);
        if (volunteer == null || !Integer.valueOf(1).equals(volunteer.getStatus())) {
            throw new OrderBusinessException("志愿者不存在或已停用");
        }
        if (!orderItemMapper.selectActiveForVolunteer(volunteerId).isEmpty()) {
            throw new OrderBusinessException("您有正在进行中的服务，请先完成当前服务");
        }
        Order order = orderMapper.selectByItemForUpdate(orderItemId);
        if (order == null || order.getStatus() == 6 || order.getStatus() == 5
                || order.getStatus() == 4) {
            throw new OrderBusinessException("订单不可接取");
        }
        OrderItem item = orderItemMapper.selectForUpdate(orderItemId);
        if (item == null) {
            throw new OrderBusinessException("服务项目不存在");
        }
        if (item.getVolunteerId() != null || item.getItemStatus() != 0) {
            throw new OrderBusinessException("该服务项目已被接取");
        }

        if (volunteer.getPoints() == null || volunteer.getPoints() < 50) {
            throw new OrderBusinessException("积分不足（当前积分：" + volunteer.getPoints() + "），无法接单。需要至少50积分才能接单。");
        }

        com.me.utils.ServiceTimeValidator.validateCanAcceptOrder(
                item.getServiceDate(), item.getServiceTime()
        );

        OrderItem claimed = new OrderItem();
        claimed.setVolunteerId(volunteerId);
        claimed.setItemStatus(1);
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<OrderItem> claim = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        claim.eq(OrderItem::getId, orderItemId).eq(OrderItem::getItemStatus, 0)
                .isNull(OrderItem::getVolunteerId);
        if (orderItemMapper.update(claimed, claim) != 1) {
            throw new OrderBusinessException("该服务项目已被接取");
        }

        updateOrderVolunteerIds(item.getOrderId());
        Integer oldStatus = updateOrderStatus(item.getOrderId());


        order = orderMapper.selectById(item.getOrderId());
        if (order != null) {
            sendStatusChangeMessage(order, oldStatus, order.getStatus(), "志愿者接单", volunteerId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void volunteerAbandonOrder(Long volunteerId, Long orderItemId) {
        Volunteer volunteer = volunteerMapper.selectForUpdate(volunteerId);
        Order order = orderMapper.selectByItemForUpdate(orderItemId);
        OrderItem item = orderItemMapper.selectForUpdate(orderItemId);
        if (item == null) {
            throw new OrderBusinessException("服务项目不存在");
        }
        if (order == null || order.getStatus() == 5 || order.getStatus() == 6) {
            throw new OrderBusinessException("订单不可操作");
        }
        if (!volunteerId.equals(item.getVolunteerId())) {
            throw new OrderBusinessException("无权操作此服务项目");
        }
        if (item.getItemStatus() != 1 && item.getItemStatus() != 2) {
            throw new OrderBusinessException("当前状态不允许放弃");
        }

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

        clearItemVolunteer(item, 0);

        updateOrderVolunteerIds(item.getOrderId());
        Integer oldStatus = updateOrderStatus(item.getOrderId());


        order = orderMapper.selectById(item.getOrderId());
        if (order != null) {
            sendStatusChangeMessage(order, oldStatus, order.getStatus(), "志愿者已放弃服务");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void volunteerStartService(Long volunteerId, Long orderItemId) {
        Order order = orderMapper.selectByItemForUpdate(orderItemId);
        OrderItem item = orderItemMapper.selectForUpdate(orderItemId);
        if (item == null) {
            throw new OrderBusinessException("服务项目不存在");
        }
        if (order == null || order.getStatus() == 5 || order.getStatus() == 6) {
            throw new OrderBusinessException("订单不可操作");
        }

        if (!volunteerId.equals(item.getVolunteerId())) {
            throw new OrderBusinessException("无权操作此服务");
        }

        if (item.getItemStatus() != 1) {
            throw new OrderBusinessException("当前状态不允许开始服务");
        }

        com.me.utils.ServiceTimeValidator.validateCanStartService(
                item.getServiceDate(), item.getServiceTime()
        );

        changeItemStatus(item, 2);

        Integer oldStatus = updateOrderStatus(item.getOrderId());


        order = orderMapper.selectById(item.getOrderId());
        if (order != null) {
            sendStatusChangeMessage(order, oldStatus, order.getStatus(), "志愿者开始服务", volunteerId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void volunteerCompleteOrder(Long volunteerId, Long orderItemId) {
        volunteerMapper.selectForUpdate(volunteerId);
        Order order = orderMapper.selectByItemForUpdate(orderItemId);
        OrderItem item = orderItemMapper.selectForUpdate(orderItemId);
        if (item == null) {
            throw new OrderBusinessException("服务项目不存在");
        }
        if (order == null || order.getStatus() == 5 || order.getStatus() == 6) {
            throw new OrderBusinessException("订单不可操作");
        }
        if (!volunteerId.equals(item.getVolunteerId())) {
            throw new OrderBusinessException("无权操作此服务项目");
        }
        if (item.getItemStatus() != 2) {
            throw new OrderBusinessException("当前状态不允许完成");
        }

        changeItemStatus(item, 3);

        addPointsForCompletedOrder(volunteerId, item);

        Integer oldStatus = updateOrderStatus(item.getOrderId());


        order = orderMapper.selectById(item.getOrderId());
        if (order != null) {
            sendStatusChangeMessage(order, oldStatus, order.getStatus(), "志愿者完成服务", volunteerId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void userStartService(Long userId, Long orderItemId) {
        Order order = orderMapper.selectByItemForUpdate(orderItemId);
        OrderItem item = orderItemMapper.selectForUpdate(orderItemId);
        if (item == null) {
            throw new OrderBusinessException("服务项目不存在");
        }
        if (order == null || !order.getUserId().equals(userId)) {
            throw new OrderBusinessException("无权操作此服务");
        }
        if (order.getStatus() == 5 || order.getStatus() == 6) {
            throw new OrderBusinessException("订单不可操作");
        }
        if (item.getItemStatus() != 1) {
            throw new OrderBusinessException("当前状态不允许开始服务");
        }

        changeItemStatus(item, 2);

        Integer oldStatus = updateOrderStatus(item.getOrderId());


        order = orderMapper.selectById(item.getOrderId());
        sendStatusChangeMessage(order, oldStatus, order.getStatus(), "用户确认开始服务");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrderItem(Long userId, Long orderItemId) {
        Order order = orderMapper.selectByItemForUpdate(orderItemId);
        OrderItem item = orderItemMapper.selectForUpdate(orderItemId);
        if (item == null) {
            throw new OrderBusinessException("服务项目不存在");
        }

        if (order == null || !order.getUserId().equals(userId)) {
            throw new OrderBusinessException("无权操作此服务");
        }
        if (order.getStatus() == 5 || order.getStatus() == 6 || order.getStatus() == 4) {
            throw new OrderBusinessException("当前订单状态不允许取消服务");
        }
        if (item.getItemStatus() != 0 && item.getItemStatus() != 1) {
            throw new OrderBusinessException("当前状态不允许取消，仅待接单或已接单状态可取消");
        }
        cancelItem(order, item, "用户取消单项服务");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean releaseAssignedItem(Long volunteerId, Long orderItemId) {
        // 停用时沿用订单取消事务，保证父订单、退款与通知同时更新。
        Order order = orderMapper.selectByItemForUpdate(orderItemId);
        OrderItem item = orderItemMapper.selectForUpdate(orderItemId);
        if (order == null || item == null || !volunteerId.equals(item.getVolunteerId())
                || (item.getItemStatus() != 1 && item.getItemStatus() != 2)) return false;
        cancelItem(order, item, "志愿者已停用，服务取消");
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void expireUnpaidOrder(Long orderId) {
        Order order = orderMapper.selectForUpdate(orderId);
        if (order != null && order.getStatus() == 6
                && !order.getCreateTime().isAfter(LocalDateTime.now().minusMinutes(15))) {
            cancelAllItems(order, "订单未支付超时");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void expirePendingItems(Long orderId) {
        Order order = orderMapper.selectForUpdate(orderId);
        if (order == null || order.getStatus() == 5 || order.getStatus() == 6) return;
        for (OrderItem item : orderItemMapper.selectByOrderForUpdate(orderId)) {
            if (item.getItemStatus() == 0 && !LocalDateTime.now().isBefore(appointment(item))) {
                cancelItem(order, item, "预约时间已到仍无人接单");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void expirePendingItem(Long orderItemId) {
        Order order = orderMapper.selectByItemForUpdate(orderItemId);
        OrderItem item = orderItemMapper.selectForUpdate(orderItemId);
        if (item == null) return;
        if (order != null && order.getStatus() != 5 && order.getStatus() != 6
                && item.getItemStatus() == 0 && !LocalDateTime.now().isBefore(appointment(item))) {
            cancelItem(order, item, "预约时间已到仍无人接单");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void expireAcceptedItem(Long orderItemId, Long volunteerId) {
        Order order = orderMapper.selectByItemForUpdate(orderItemId);
        OrderItem item = orderItemMapper.selectForUpdate(orderItemId);
        if (item == null) return;
        if (order != null && order.getStatus() != 5 && order.getStatus() != 6
                && item.getItemStatus() == 1 && item.getVolunteerId() != null
                && (volunteerId == null || volunteerId.equals(item.getVolunteerId()))
                && !LocalDateTime.now().isBefore(appointment(item).plusMinutes(10))) {
            cancelItem(order, item, "志愿者超时未开始服务");
        }
    }

    private LocalDateTime appointment(OrderItem item) {
        return LocalDateTime.of(LocalDate.parse(item.getServiceDate()), LocalTime.parse(item.getServiceTime()));
    }

    // 条件更新失败必须回滚，显式 SQL 才能将接单者写为 NULL。
    private void clearItemVolunteer(OrderItem item, int status) {
        if (orderItemMapper.clearVolunteer(item.getId(), item.getItemStatus(), status, item.getVolunteerId()) != 1) {
            throw new IllegalStateException("服务状态已变化，请重试");
        }
        item.setVolunteerId(null);
        item.setItemStatus(status);
    }

    private void changeItemStatus(OrderItem item, int status) {
        if (orderItemMapper.changeStatus(item.getId(), item.getItemStatus(), status) != 1) {
            throw new IllegalStateException("服务状态已变化，请重试");
        }
        item.setItemStatus(status);
    }

    private void cancelAllItems(Order order, String reason) {
        List<OrderItem> items = orderItemMapper.selectByOrderForUpdate(order.getId());
        if (items.stream().anyMatch(item -> item.getItemStatus() != 0
                && item.getItemStatus() != 1 && item.getItemStatus() != 5)) {
            throw new IllegalStateException("已有服务开始或完成，不能取消整个订单");
        }
        Integer oldStatus = order.getStatus();
        List<OrderItem> cancelled = items.stream().filter(item -> item.getItemStatus() != 5).toList();
        for (OrderItem item : cancelled) {
            Long volunteerId = item.getVolunteerId();
            clearItemVolunteer(item, 5);
            sendMessageToVolunteer(volunteerId, 1, 0, "订单已取消", reason, item.getId());
        }
        order.setVolunteerIds(null);
        order.setStatus(5);
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Order> cancel =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        cancel.eq(Order::getId, order.getId()).eq(Order::getStatus, oldStatus)
                .set(Order::getVolunteerIds, null).set(Order::getStatus, 5);
        if (orderMapper.update(null, cancel) != 1) {
            throw new IllegalStateException("订单状态已变化，请重试");
        }
        recordMockRefunds(order, cancelled, reason);
        sendStatusChangeMessage(order, oldStatus, 5, reason);
    }

    private void cancelItem(Order order, OrderItem item, String reason) {
        Integer oldStatus = order.getStatus();
        Long volunteerId = item.getVolunteerId();
        clearItemVolunteer(item, 5);
        updateOrderVolunteerIds(order.getId());
        updateOrderStatus(order.getId());
        recordMockRefunds(order, List.of(item), reason);
        sendMessageToVolunteer(volunteerId, 1, 0, "服务项已取消", reason, item.getId());
        Order updated = orderMapper.selectById(order.getId());
        sendStatusChangeMessage(updated, oldStatus, updated.getStatus(), reason);
    }

    private void recordMockRefunds(Order order, List<OrderItem> cancelled, String reason) {
        if (cancelled.isEmpty()) return;
        LambdaQueryWrapper<PaymentTransaction> paymentQuery = new LambdaQueryWrapper<>();
        paymentQuery.eq(PaymentTransaction::getOrderId, order.getId())
                .eq(PaymentTransaction::getPaymentMethod, "mock")
                .in(PaymentTransaction::getPaymentStatus, 1, 2);
        List<PaymentTransaction> payments = paymentTransactionMapper.selectList(paymentQuery);
        if (payments.isEmpty()) return;
        PaymentTransaction payment = payments.get(0);
        for (OrderItem item : cancelled) {
            // 使用服务项 ID 标识模拟退款，保留原始支付金额供核对。
            String refundNo = "REF" + item.getId();
            LambdaQueryWrapper<PaymentTransaction> existing = new LambdaQueryWrapper<>();
            existing.eq(PaymentTransaction::getTransactionNo, refundNo);
            if (paymentTransactionMapper.selectCount(existing) > 0) continue;
            PaymentTransaction refund = new PaymentTransaction();
            refund.setTransactionNo(refundNo);
            refund.setOrderId(order.getId());
            refund.setOrderNo(order.getOrderNo());
            refund.setUserId(order.getUserId());
            refund.setAmount(item.getItemPrice());
            refund.setPaymentMethod("mock_refund");
            refund.setPaymentStatus(2);
            refund.setPaymentTime(payment.getPaymentTime());
            refund.setRefundTime(LocalDateTime.now());
            refund.setCreateTime(LocalDateTime.now());
            refund.setUpdateTime(LocalDateTime.now());
            refund.setRemark(reason);
            paymentTransactionMapper.insert(refund);
        }
        if (checkAllItemsCancelled(order.getId())) {
            payment.setPaymentStatus(2);
            payment.setRefundTime(LocalDateTime.now());
            payment.setUpdateTime(LocalDateTime.now());
            paymentTransactionMapper.updateById(payment);
        }
    }

    private void updateOrderVolunteerIds(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectByOrderForUpdate(orderId);

        Set<Long> volunteerIdSet = items.stream()
                .filter(item -> item.getItemStatus() != 0 && item.getItemStatus() != 5)
                .map(OrderItem::getVolunteerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        String ids = volunteerIdSet.isEmpty() ? null : volunteerIdSet.stream()
                .sorted().map(String::valueOf).collect(Collectors.joining(","));
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Order> update =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        update.eq(Order::getId, orderId).set(Order::getVolunteerIds, ids);
        if (orderMapper.update(null, update) != 1) {
            throw new IllegalStateException("订单不存在");
        }
    }

    private Integer updateOrderStatus(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectByOrderForUpdate(orderId);

        if (items.isEmpty()) return null;

        Order order = orderMapper.selectForUpdate(orderId);
        if (order == null) return null;

        Integer oldStatus = order.getStatus();

        List<OrderItem> active = items.stream().filter(item -> item.getItemStatus() != 5).toList();
        if (active.isEmpty()) {
            order.setStatus(5);
            orderMapper.updateById(order);
            return oldStatus;
        }
        boolean allCompleted = active.stream().allMatch(item -> item.getItemStatus() == 4);

        if (allCompleted) {
            if (order.getStatus() != 4) {
                order.setStatus(4);
                order.setCompleteTime(LocalDateTime.now());
                orderMapper.updateById(order);
            }
            return oldStatus;
        }

        boolean allPendingConfirm = active.stream().allMatch(item -> item.getItemStatus() >= 3);

        if (allPendingConfirm) {
            if (order.getStatus() != 3 || order.getCompleteTime() == null) {
                order.setStatus(3);
                if (order.getCompleteTime() == null) {
                    order.setCompleteTime(LocalDateTime.now());
                }
                orderMapper.updateById(order);
            }
            return oldStatus;
        }

        boolean anyInProgress = active.stream().anyMatch(item ->
                item.getItemStatus() == 2
        );

        boolean anyAccepted = active.stream().anyMatch(item ->
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

    private boolean checkAllItemsCancelled(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectByOrderForUpdate(orderId);

        if (items.isEmpty()) {
            return true;
        }

        return items.stream().allMatch(item -> item.getItemStatus() == 5);
    }

    private void sendStatusChangeMessage(Order order, Integer oldStatus, Integer newStatus, String remark) {
        sendStatusChangeMessage(order, oldStatus, newStatus, remark, null);
    }

    private void sendStatusChangeMessage(Order order, Integer oldStatus, Integer newStatus, String remark, Long volunteerId) {
        if (oldStatus == null) {
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

        // 状态更新与待发送事件一起提交，不能在事务内直接投递。
        outboxService.enqueue(RabbitMQConfig.ORDER_STATUS_FANOUT_EXCHANGE, "", statusMessage);
        log.info("订单状态变更消息已登记: orderId={}, status={}→{}",
                order.getId(), oldStatus, newStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @BizLog(value = "志愿者接单", logParams = false)
    public void confirmOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectForUpdate(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new OrderBusinessException("订单不存在");
        }
        if (order.getStatus() != 3) {
            throw new OrderBusinessException("订单状态不允许确认");
        }

        Integer oldStatus = order.getStatus();
        List<OrderItem> items = orderItemMapper.selectByOrderForUpdate(orderId);

        for (OrderItem item : items) {
            if (item.getItemStatus() == 3) {
                changeItemStatus(item, 4);
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
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("评分必须为1至5星");
        }
        OrderItem item = orderItemMapper.selectById(orderItemId);
        if (item == null) {
            throw new OrderBusinessException("服务项目不存在");
        }

        Order order = orderMapper.selectById(item.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new OrderBusinessException("无权评价此服务");
        }

        if (item.getItemStatus() != 3 && item.getItemStatus() != 4) {
            throw new OrderBusinessException("服务未完成，无法评价");
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
    public OrderVO getAdminOrderDetailVO(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return null;
        }

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);

        LambdaQueryWrapper<OrderItem> orderItemWrapper = new LambdaQueryWrapper<>();
        orderItemWrapper.eq(OrderItem::getOrderId, id);
        List<OrderItem> items = orderItemMapper.selectList(orderItemWrapper);
        //List<OrderItem> items = orderItemMapper.selectByOrderId(id);
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
    public boolean adminCancelOrder(Long id) {
        Order order = orderMapper.selectForUpdate(id);
        if (order == null || (order.getStatus() != 0 && order.getStatus() != 1 && order.getStatus() != 6)) {
            return false;
        }
        cancelAllItems(order, "管理员取消订单");
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adminCancelOrderItem(Long orderItemId) {
        Order order = orderMapper.selectByItemForUpdate(orderItemId);
        OrderItem item = orderItemMapper.selectForUpdate(orderItemId);
        if (item == null) {
            return false;
        }

        if (item.getItemStatus() != 0 && item.getItemStatus() != 1) {
            return false;
        }

        if (order == null || order.getStatus() == 5 || order.getStatus() == 6 || order.getStatus() == 4) {
            return false;
        }
        cancelItem(order, item, "管理员取消单项服务");
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adminCompleteOrder(Long id) {
        Order order = orderMapper.selectForUpdate(id);
        if (order == null || order.getStatus() != 3) {
            return false;
        }

        List<OrderItem> items = orderItemMapper.selectByOrderForUpdate(id);

        if (items.stream().noneMatch(item -> item.getItemStatus() == 3)
                || items.stream().anyMatch(item -> item.getItemStatus() != 3
                && item.getItemStatus() != 4 && item.getItemStatus() != 5)) {
            return false;
        }
        for (OrderItem item : items) {
            if (item.getItemStatus() == 3) {
                changeItemStatus(item, 4);
            }
        }

        order.setStatus(4);
        order.setCompleteTime(LocalDateTime.now());
        boolean updated = orderMapper.updateById(order) > 0;
        return updated;
    }

    @Override
    public Long countOrders(LambdaQueryWrapper<Order> wrapper) {
        return orderMapper.selectCount(wrapper);
    }

    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "");
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

        Volunteer volunteer = volunteerMapper.selectForUpdate(volunteerId);
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
