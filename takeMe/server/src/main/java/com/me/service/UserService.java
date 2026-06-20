package com.me.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.me.dto.LoginDTO;
import com.me.dto.PageResultDTO;
import com.me.dto.UserRegisterDTO;
import com.me.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {
    User login(LoginDTO loginDTO);

    boolean register(UserRegisterDTO registerDTO);

    boolean updatePassword(Long userId, String oldPassword, String newPassword);

    IPage<User> searchUser(
            String keyword,
            Integer gender,
            Long id,
            String startDate,
            String endDate,
            PageResultDTO pageResultDTO,
            String sortBy,
            String sortOrder
    );

    void updateAvatar(Long userId, String avatarUrl);

    void deleteAvatar(Long userId);

    java.util.List<Long> getAllUserIds(int pageNum, int pageSize);

    boolean logicalDeleteUser(Long userId);
}