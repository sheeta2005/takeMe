package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.me.dto.LoginDTO;
import com.me.dto.PageResultDTO;
import com.me.dto.UserRegisterDTO;
import com.me.entity.Approval;
import com.me.entity.OrderItem;
import com.me.entity.Volunteer;
import com.me.mapper.ApprovalMapper;
import com.me.mapper.OrderItemMapper;
import com.me.mapper.VolunteerMapper;
import com.me.service.VolunteerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VolunteerServiceImpl extends ServiceImpl<VolunteerMapper, Volunteer> implements VolunteerService {

    private final PasswordEncoder passwordEncoder;
    private final ApprovalMapper approvalMapper;
    private final OrderItemMapper orderItemMapper;

    @Override
    public Volunteer login(LoginDTO loginDTO) {
        // 1. 根据账号查询志愿者
        LambdaQueryWrapper<Volunteer> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Volunteer::getUsername, loginDTO.getUsername());
        Volunteer volunteer = this.getOne(queryWrapper);

        // 2. 账号不存在、密码错误或账号禁用，返回null
        if (volunteer == null
                || !passwordEncoder.matches(loginDTO.getPassword(), volunteer.getPassword())
                || volunteer.getStatus() != 1) { // status=1表示正常，0表示禁用
            return null;
        }

        // 3. 登录成功，返回志愿者信息
        return volunteer;
    }

    @Override
    public Volunteer getByUsername(String username) {
        return lambdaQuery()
                .eq(Volunteer::getUsername, username)
                .one();
    }

    @Override
    public IPage<Volunteer> searchVolunteer(
            String username,
            Long id,
            PageResultDTO pageResultDTO
    ) {
        Page<Volunteer> pageParam = new Page<>(pageResultDTO.getPageNum(), pageResultDTO.getPageSize());
        LambdaQueryWrapper<Volunteer> wrapper = new LambdaQueryWrapper<>();

        if (username != null && !username.trim().isEmpty()) {
            wrapper.like(Volunteer::getUsername, username);
        }
        if (id != null) {
            wrapper.eq(Volunteer::getId, id);
        }

        wrapper.orderByDesc(Volunteer::getCreateTime);
        return this.page(pageParam, wrapper);
    }
    
    @Override
    public boolean register(UserRegisterDTO registerDTO) {
        // 检查用户名是否已存在
        Volunteer existingVolunteer = this.getByUsername(registerDTO.getUsername());
        if (existingVolunteer != null) {
            return false; // 用户名已存在，注册失败
        }
        
        // 创建新志愿者对象
        Volunteer volunteer = new Volunteer();
        volunteer.setUsername(registerDTO.getUsername());
        volunteer.setPassword(passwordEncoder.encode(registerDTO.getPassword())); // 加密密码
        volunteer.setRealName(registerDTO.getRealName());
        volunteer.setPhone(registerDTO.getPhone());
        volunteer.setStatus(0); // 新注册用户默认为待审批状态（0=停用，1=启用）
        volunteer.setCreateTime(LocalDateTime.now());
        volunteer.setLastLoginTime(null);
        
        // 设置默认值
        volunteer.setGender(null);
        volunteer.setAge(null);
        volunteer.setAddress(null);
        volunteer.setServiceDays(null);
        volunteer.setWorkStatus(0); // 默认休息中
        volunteer.setTotalServiceHours(0);
        volunteer.setEmergencyName(null);
        volunteer.setEmergencyPhone(null);
        volunteer.setAvatar("http://localhost:8080/uploads/default-volunteer-avatar.png"); // 设置默认头像
        
        // 保存到数据库
        boolean saved = this.save(volunteer);
        
        if (saved) {
            // 创建审批记录
            Approval approval = new Approval();
            approval.setType("register");
            approval.setApplicantId(volunteer.getId());
            approval.setApplicantName(volunteer.getRealName());
            approval.setContent("志愿者注册申请");
            approval.setStatus("pending");
            approval.setCreateTime(LocalDateTime.now());
            
            approvalMapper.insert(approval);
        }
        
        return saved;
    }
    
    @Override
    public int releaseVolunteerServices(Long volunteerId) {
        if (volunteerId == null) {
            return 0;
        }
        
        // 查询该志愿者所有 itemStatus=1（已接单）或 2（服务中）的订单项
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getVolunteerId, volunteerId)
               .in(OrderItem::getItemStatus, Arrays.asList(1, 2));
        
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        
        if (items == null || items.isEmpty()) {
            return 0;
        }
        
        int count = 0;
        for (OrderItem item : items) {
            // 清空志愿者ID，设置为已放弃状态
            item.setVolunteerId(null);
            item.setItemStatus( 5); // 5=已放弃
            orderItemMapper.updateById(item);
            count++;
        }
        
        return count;
    }
}