package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.dto.LoginDTO;
import com.me.dto.UserRegisterDTO;
import com.me.entity.Volunteer;
import com.me.mapper.VolunteerMapper;
import com.me.service.VolunteerService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class VolunteerServiceImpl extends ServiceImpl<VolunteerMapper, Volunteer> implements VolunteerService {

    private final PasswordEncoder passwordEncoder;

    public VolunteerServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

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
    public Page<Volunteer> searchVolunteer(
            Integer page,
            Integer pageSize,
            String username,
            Long id
    ) {
        Page<Volunteer> pageParam = new Page<>(page, pageSize);
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
        return this.save(volunteer);
    }
}