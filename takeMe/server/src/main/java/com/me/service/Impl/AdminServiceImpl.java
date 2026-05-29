package com.me.service.Impl;

import com.me.dto.LoginDTO;
import com.me.entity.Admin;
import com.me.mapper.AdminMapper;
import com.me.service.AdminService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    private final PasswordEncoder passwordEncoder;

    public AdminServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Admin login(LoginDTO loginDTO) {
        // 1. 根据账号查询管理员
        LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Admin::getUsername, loginDTO.getUsername());
        Admin admin = this.getOne(queryWrapper);

        // 2. 账号不存在或密码错误，返回null
        if (admin == null || !passwordEncoder.matches(loginDTO.getPassword(), admin.getPassword())) {
            return null;
        }

        // 3. 登录成功，返回管理员信息
        return admin;
    }
}