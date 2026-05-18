package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.me.dto.LoginDTO;
import com.me.entity.User;
import com.me.mapper.UserMapper;
import com.me.service.UserService;
import com.me.vo.LoginVO;
import com.me.vo.UserInfoVO;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getAccount, loginDTO.getAccount());
        User user = this.getOne(wrapper);
        if (user == null) {
            throw new RuntimeException("账号不存在");
        }
        String encryptPwd = DigestUtils.md5DigestAsHex(loginDTO.getPassword().getBytes());
        if (!user.getPassword().equals(encryptPwd)) {
            throw new RuntimeException("密码错误");
        }
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }
        LoginVO loginVO = new LoginVO();
        loginVO.setToken("token_" + user.getId());
        loginVO.setRole(1);
        loginVO.setId(user.getId());
        return loginVO;
    }

    @Override
    public User getById(Long id) {
        return getById(id);
    }

    @Override
    public UserInfoVO getUserInfo(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setAccount(user.getAccount());
        vo.setPhone(user.getPhone());
        vo.setAge(user.getAge());
        vo.setGender(user.getGender());
        vo.setAddress(user.getAddress());
        vo.setWalkingRange(user.getWalkingRange());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        return vo;
    }
}