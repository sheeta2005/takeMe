package com.me.service.Impl;

import com.me.dto.LoginDTO;
import com.me.entity.Volunteer;
import com.me.mapper.VolunteerMapper;
import com.me.service.VolunteerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
}