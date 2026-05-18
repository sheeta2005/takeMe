package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.me.dto.OrderApproveDTO;
import com.me.dto.OrderCreateDTO;
import com.me.entity.Admin;
import com.me.entity.ServiceOrder;
import com.me.entity.User;
import com.me.entity.Volunteer;
import com.me.mapper.AdminMapper;
import com.me.mapper.ServiceOrderMapper;
import com.me.mapper.UserMapper;
import com.me.mapper.VolunteerMapper;
import com.me.service.ServiceOrderService;
import com.me.vo.OrderDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ServiceOrderServiceImpl extends ServiceImpl<ServiceOrderMapper, ServiceOrder> implements ServiceOrderService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private VolunteerMapper volunteerMapper;
    @Autowired
    private AdminMapper adminMapper;

    private static final Map<Integer, String> DAY_MAP = Map.of(
            1, "周一", 2, "周二", 3, "周三", 4, "周四", 5, "周五", 6, "周六", 7, "周日"
    );

    private static final Map<Integer, String> TIME_MAP = Map.of(
            1, "08:00-12:00", 2, "14:00-18:00"
    );

    private static final Map<Integer, String> STATUS_MAP = Map.of(
            1, "待接单", 2, "服务中", 3, "已完成", 4, "已取消"
    );

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(OrderCreateDTO dto) {
        Volunteer volunteer = volunteerMapper.selectById(dto.getVolunteerId());
        if (volunteer == null || volunteer.getCurrentStatus() != 1) {
            throw new RuntimeException("志愿者不可用");
        }
        ServiceOrder order = new ServiceOrder();
        order.setElderlyId(dto.getElderlyId());
        order.setVolunteerId(dto.getVolunteerId());
        order.setServiceArea(dto.getServiceArea());
        order.setServiceDay(dto.getServiceDay());
        order.setServiceTime(dto.getServiceTime());
        order.setOrderStatus(1);
        this.save(order);
    }

    @Override
    public List<OrderDetailVO> getOrderList(Long userId, Integer role) {
        LambdaQueryWrapper<ServiceOrder> wrapper = new LambdaQueryWrapper<>();
        if (role == 1) {
            wrapper.eq(ServiceOrder::getElderlyId, userId);
        } else if (role == 2) {
            wrapper.eq(ServiceOrder::getVolunteerId, userId);
        }
        List<ServiceOrder> orderList = this.list(wrapper);
        List<Long> elderlyIds = orderList.stream().map(ServiceOrder::getElderlyId).distinct().collect(Collectors.toList());
        List<Long> volunteerIds = orderList.stream().map(ServiceOrder::getVolunteerId).distinct().collect(Collectors.toList());
        List<Long> adminIds = orderList.stream().map(ServiceOrder::getAdminId).filter(id -> id != null).distinct().collect(Collectors.toList());

        Map<Long, User> userMap = userMapper.selectBatchIds(elderlyIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Volunteer> volunteerMap = volunteerMapper.selectBatchIds(volunteerIds).stream()
                .collect(Collectors.toMap(Volunteer::getId, v -> v));
        Map<Long, Admin> adminMap = adminMapper.selectBatchIds(adminIds).stream()
                .collect(Collectors.toMap(Admin::getId, a -> a));

        return orderList.stream().map(order -> {
            OrderDetailVO vo = new OrderDetailVO();
            vo.setId(order.getId());
            vo.setElderlyId(order.getElderlyId());
            vo.setElderlyName(userMap.get(order.getElderlyId()).getUsername());
            vo.setVolunteerId(order.getVolunteerId());
            vo.setVolunteerName(volunteerMap.get(order.getVolunteerId()).getUsername());
            vo.setServiceArea(order.getServiceArea());
            vo.setServiceDay(order.getServiceDay());
            vo.setServiceDayStr(DAY_MAP.get(order.getServiceDay()));
            vo.setServiceTime(order.getServiceTime());
            vo.setServiceTimeStr(TIME_MAP.get(order.getServiceTime()));
            vo.setOrderStatus(order.getOrderStatus());
            vo.setOrderStatusStr(STATUS_MAP.get(order.getOrderStatus()));
            vo.setAdminId(order.getAdminId());
            if (order.getAdminId() != null) {
                vo.setAdminName(adminMap.get(order.getAdminId()).getName());
            }
            vo.setCreateTime(order.getCreateTime());
            vo.setServiceStartTime(order.getServiceStartTime());
            vo.setServiceEndTime(order.getServiceEndTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveOrder(OrderApproveDTO dto) {
        ServiceOrder order = this.getById(dto.getOrderId());
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (dto.getApproveStatus() == 1) {
            order.setAdminId(dto.getAdminId());
            order.setOrderStatus(2);
            Volunteer volunteer = volunteerMapper.selectById(order.getVolunteerId());
            volunteer.setCurrentStatus(2);
            volunteerMapper.updateById(volunteer);
        } else {
            order.setOrderStatus(4);
        }
        this.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderStatus(Long orderId, Integer status) {
        ServiceOrder order = this.getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        order.setOrderStatus(status);
        if (status == 3) {
            order.setServiceEndTime(LocalDateTime.now());
            Volunteer volunteer = volunteerMapper.selectById(order.getVolunteerId());
            volunteer.setCurrentStatus(1);
            volunteerMapper.updateById(volunteer);
        } else if (status == 2) {
            order.setServiceStartTime(LocalDateTime.now());
        }
        this.updateById(order);
    }
}