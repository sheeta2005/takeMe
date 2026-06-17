package com.me.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.me.dto.LoginDTO;
import com.me.dto.PageResultDTO;
import com.me.dto.UserRegisterDTO;
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

    IPage<Volunteer> searchVolunteer(
            String username,
            Long id,
            PageResultDTO pageResultDTO
    );
    
    /**
     * 志愿者注册
     * @param registerDTO 注册参数
     * @return 注册是否成功
     */
    boolean register(UserRegisterDTO registerDTO);
    
    /**
     * 释放志愿者所有进行中的服务（itemStatus=1或2）
     * @param volunteerId 志愿者ID
     * @return 释放的服务数量
     */
    int releaseVolunteerServices(Long volunteerId);

    void updateAvatar(Long volunteerId, String avatarUrl);

    void deleteAvatar(Long volunteerId);
}