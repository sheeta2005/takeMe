package com.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.me.dto.LoginDTO;
import com.me.entity.User;
import com.me.vo.LoginVO;
import com.me.vo.UserInfoVO;

public interface UserService extends IService<User> {
    LoginVO login(LoginDTO loginDTO);
    UserInfoVO getUserInfo(Long userId);

    User getById(Long id);

    User getByAccount(String account);
}