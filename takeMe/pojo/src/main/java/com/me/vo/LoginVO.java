package com.me.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {
    // 登录凭证token
    private String token;
    // 用户ID（普通用户登录时返回）
    private Long userId;
    // 志愿者ID（志愿者登录时返回）
    private Long volunteerId;
    // 管理员ID（管理员登录时返回）
    private Long adminId;
    // 姓名/昵称
    private String realName;
    // 身份类型：0=用户 1=志愿者 2=管理员
    private Integer userType;
    // 头像
    private String avatar;
}