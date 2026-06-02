package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.dto.LoginDTO;
import com.me.entity.User;
import com.me.mapper.UserMapper;
import com.me.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class  UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User login(LoginDTO loginDTO) {
        // 1. 根据账号查询普通用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = this.getOne(queryWrapper);

        // 2. 账号不存在、密码错误或账号禁用，返回null
        if (user == null
                || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())
                || user.getStatus() != 1) { // status=1表示正常，0表示禁用
            return null;
        }

        // 3. 登录成功，返回用户信息
        return user;
    }
    
    @Override
    public Page<User> searchUser(
            Integer page,
            Integer pageSize,
            String keyword,
            Integer gender,
            String startDate,
            String endDate
    ) {
        Page<User> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or()
                    .like(User::getRealName, keyword)
                    .or()
                    .like(User::getPhone, keyword));
        }
        if (gender != null) {
            wrapper.eq(User::getGender, gender);
        }
        if (startDate != null && !startDate.trim().isEmpty()) {
            wrapper.ge(User::getCreateTime, startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            wrapper.le(User::getCreateTime, endDate + " 23:59:59");
        }
        
        wrapper.orderByDesc(User::getCreateTime);
        return this.page(pageParam, wrapper);
    }
}