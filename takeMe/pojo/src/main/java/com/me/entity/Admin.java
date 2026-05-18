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
 * 管理员实体类
 * 对应数据库表 admin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("admin")
public class Admin {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 登录账号
     */
    private String account;

    /**
     * 登录密码
     */
    private String password;

    /**
     * 管理员姓名
     */
    private String name;

    /**
     * 手机号码
     */
    private String phone;

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