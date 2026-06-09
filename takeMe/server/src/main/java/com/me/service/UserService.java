package com.me.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.dto.LoginDTO;
import com.me.dto.UserRegisterDTO;
import com.me.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {
    User login(LoginDTO loginDTO);

    boolean register(UserRegisterDTO registerDTO);

    boolean updatePassword(Long userId, String oldPassword, String newPassword);

    Page<User> searchUser(
            Integer page,
            Integer pageSize,
            String keyword,
            Integer gender,
            String startDate,
            String endDate
    );
}