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
    // 当前登录人ID
    private Long loginId;
    // 身份类型：0=管理员 1=志愿者 2=普通用户（老人）
    private Integer userType;
    // 姓名/昵称
    private String realName;
    // 头像
    private String avatar;
}