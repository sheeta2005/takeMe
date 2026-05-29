package com.me.service;

import com.me.dto.LoginDTO;
import com.me.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {
    /**
     * 普通用户登录
     * @param loginDTO 登录参数
     * @return 用户信息（登录成功），null（登录失败/账号禁用）
     */
    User login(LoginDTO loginDTO);
}