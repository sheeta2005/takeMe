package com.me.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
    
    /**
     * 分页搜索用户
     * @param page 页码
     * @param pageSize 每页数量
     * @param keyword 关键词（姓名/账号/手机号）
     * @param gender 性别
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分页结果
     */
    Page<User> searchUser(
        Integer page,
        Integer pageSize,
        String keyword,
        Integer gender,
        String startDate,
        String endDate
    );
}