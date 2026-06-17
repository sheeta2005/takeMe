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

    /**
     * 获取当前管理员信息（脱敏处理）
     * @param adminId 管理员ID
     * @return 脱敏后的管理员信息
     */
    Admin getAdminInfo(Long adminId);

    /**
     * 更新管理员信息（仅允许修改realName）
     * @param admin 待更新的管理员对象
     * @return 是否更新成功
     */
    boolean updateAdminInfo(Admin admin);

    void updateAvatar(Long adminId, String avatarUrl);

    void deleteAvatar(Long adminId);
}