package com.me.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 包含：老人、志愿者、管理员（通用用户表）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户姓名
     */
    private String username;

    /**
     * 登录账号
     */
    private String account;

    /**
     * 登录密码（加密存储）
     */
    private String password;

    /**
     * 手机号码
     */
    private String phone;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 性别 0-女 1-男
     */
    private Integer gender;

    /**
     * 居住地址
     */
    private String address;


    /**
     * 头像地址
     */
    private String avatar;

    /**
     * 账号状态
     * 0 禁用
     * 1 启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}