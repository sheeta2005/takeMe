package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.me.dto.LoginDTO;
import com.me.entity.Admin;
import com.me.mapper.AdminMapper;
import com.me.service.AdminService;
import com.me.vo.LoginVO;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getAccount, loginDTO.getAccount());
        Admin admin = this.getOne(wrapper);
        if (admin == null) {
            throw new RuntimeException("账号不存在");
        }
        String encryptPwd = DigestUtils.md5DigestAsHex(loginDTO.getPassword().getBytes());
        if (!admin.getPassword().equals(encryptPwd)) {
            throw new RuntimeException("密码错误");
        }
        if (admin.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }
        LoginVO loginVO = new LoginVO();
        loginVO.setToken("token_" + admin.getId());
        loginVO.setRole(3);
        loginVO.setId(admin.getId());
        return loginVO;
    }

    @Override
    public Admin getById(Long id) {
        return getById(id);
    }
}