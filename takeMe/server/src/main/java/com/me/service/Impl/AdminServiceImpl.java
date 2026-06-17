package com.me.service.Impl;

import com.me.dto.LoginDTO;
import com.me.entity.Admin;
import com.me.mapper.AdminMapper;
import com.me.redis.annotation.RedisCache;
import com.me.service.AdminService;
import com.me.util.OssUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    private final PasswordEncoder passwordEncoder;
    private final OssUtil ossUtil;

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

    @Override
    public void updateAvatar(Long adminId, String avatarUrl) {
        Admin admin = this.getById(adminId);
        if (admin == null) {
            throw new IllegalArgumentException("管理员不存在");
        }

        String oldAvatar = admin.getAvatar();
        if (oldAvatar != null && !oldAvatar.isEmpty()) {
            ossUtil.deleteFile(oldAvatar);
        }

        admin.setAvatar(avatarUrl);
        this.updateById(admin);
        
        log.info("Admin {} avatar updated: {}", adminId, avatarUrl);
    }

    @Override
    public void deleteAvatar(Long adminId) {
        Admin admin = this.getById(adminId);
        if (admin == null) {
            throw new IllegalArgumentException("管理员不存在");
        }

        String oldAvatar = admin.getAvatar();
        if (oldAvatar != null && !oldAvatar.isEmpty()) {
            ossUtil.deleteFile(oldAvatar);
        }

        admin.setAvatar(null);
        this.updateById(admin);
        
        log.info("Admin {} avatar deleted", adminId);
    }
}