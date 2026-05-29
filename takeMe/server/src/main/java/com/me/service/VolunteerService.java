package com.me.service;

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
}