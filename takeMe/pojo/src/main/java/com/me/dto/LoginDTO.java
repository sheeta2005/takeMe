package com.me.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginDTO {
    // 账号
    private String username;
    // 密码
    private String password;
    // 用户类型：0=管理员 1=志愿者 2=普通用户（老人）
    private Integer userType;
    // 手机号（可选，短信登录用）
    private String phone;
    // 验证码（可选）
    private String code;
}