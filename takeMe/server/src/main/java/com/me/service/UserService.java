package com.me.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.me.dto.LoginDTO;
import com.me.dto.PageResultDTO;
import com.me.dto.UserRegisterDTO;
import com.me.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {
    //校验用户登录
    User login(LoginDTO loginDTO);

    //注册用户账号
    boolean register(UserRegisterDTO registerDTO);

    //修改用户密码
    boolean updatePassword(Long userId, String oldPassword, String newPassword);

    //分页查询用户
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

    //更新用户头像
    void updateAvatar(Long userId, String avatarUrl);

    //删除用户头像
    void deleteAvatar(Long userId);

    //分页查询启用用户编号
    java.util.List<Long> getAllUserIds(int pageNum, int pageSize);

    //用户逻辑删除
    boolean logicalDeleteUser(Long userId);
}