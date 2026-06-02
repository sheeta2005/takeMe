package com.me.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.dto.LoginDTO;
import com.me.entity.Volunteer;
import com.baomidou.mybatisplus.extension.service.IService;

public interface VolunteerService extends IService<Volunteer> {
    /**
     * 志愿者登录
     * @param loginDTO 登录参数
     * @return 志愿者信息（登录成功），null（登录失败/账号禁用）
     */
    Volunteer login(LoginDTO loginDTO);
    
    /**
     * 根据用户名查询志愿者
     * @param username 用户名
     * @return 志愿者信息
     */
    Volunteer getByUsername(String username);
    
    /**
     * 分页搜索志愿者
     * @param page 页码
     * @param pageSize 每页数量
     * @param username 用户名
     * @param id ID
     * @param serviceType 服务类型
     * @return 分页结果
     */
    Page<Volunteer> searchVolunteer(
        Integer page,
        Integer pageSize,
        String username,
        Long id,
        Integer serviceType
    );
}