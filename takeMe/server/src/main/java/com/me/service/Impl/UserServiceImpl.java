package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.dto.LoginDTO;
import com.me.dto.PageResultDTO;
import com.me.dto.UserRegisterDTO;
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
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = this.getOne(queryWrapper);

        if (user == null
                || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())
                || user.getStatus() != 1) {
            return null;
        }

        return user;
    }

    @Override
    public boolean register(UserRegisterDTO registerDTO) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, registerDTO.getUsername());
        Long count = this.count(wrapper);
        if (count > 0) {
            return false;
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setRealName(registerDTO.getRealName());
        user.setPhone(registerDTO.getPhone());
        user.setStatus(1);

        return this.save(user);
    }

    @Override
    public boolean updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = this.getById(userId);
        if (user == null) {
            return false;
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        return this.updateById(user);
    }

    @Override
    public IPage<User> searchUser(
            String keyword,
            Integer gender,
            String startDate,
            String endDate,
            PageResultDTO pageResultDTO
    ) {
        Page<User> pageParam = new Page<>(pageResultDTO.getPageNum(), pageResultDTO.getPageSize());
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