package com.me.service.Impl;

import com.me.dto.LoginDTO;
import com.me.entity.User;
import com.me.mapper.UserMapper;
import com.me.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
}