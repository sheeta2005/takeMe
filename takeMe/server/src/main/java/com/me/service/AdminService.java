package com.me.service;

import com.me.dto.LoginDTO;
import com.me.entity.Admin;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AdminService extends IService<Admin> {
    /**
     * 管理员登录
     * @param loginDTO 登录参数
     * @return 管理员信息（登录成功），null（登录失败）
     */
    Admin login(LoginDTO loginDTO);
}