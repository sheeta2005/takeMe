package com.me.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String realName;      // 真实姓名
    private String username;      // 登录账号
    private String phone;         // 手机号
    private String password;      // 密码
    private String avatar;        // 头像

    private Integer gender;       // 0男 1女
    private Integer age;          // 年龄

    private String emergencyName; // 紧急联系人
    private String emergencyPhone;// 紧急电话

    private Integer status;       // 0禁用 1正常
    private LocalDateTime createTime;
    private LocalDateTime lastLoginTime;
}