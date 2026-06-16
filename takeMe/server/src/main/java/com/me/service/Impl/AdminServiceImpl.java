package com.me.service.Impl;

import com.me.dto.LoginDTO;
import com.me.entity.Admin;
import com.me.mapper.AdminMapper;
import com.me.redis.annotation.RedisCache;
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
        LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Admin::getUsername, loginDTO.getUsername());
        Admin admin = this.getOne(queryWrapper);

        if (admin == null || !passwordEncoder.matches(loginDTO.getPassword(), admin.getPassword())) {
            return null;
        }

        return admin;
    }

    @Override
    @RedisCache(prefix = "admin:info", expire = 120, nullExpire = 2)
    public Admin getAdminInfo(Long adminId) {
        Admin admin = this.getById(adminId);
        if (admin != null) {
            admin.setPassword(null);
        }
        return admin;
    }

    @Override
    public boolean updateAdminInfo(Admin admin) {
        admin.setUsername(null);
        admin.setPassword(null);
        admin.setCreateTime(null);
        admin.setLastLoginTime(null);
        return this.updateById(admin);
    }
}