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
 * 志愿者实体
 * 对应数据库表 volunteer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("volunteer")
public class Volunteer {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 志愿者姓名
     */
    private String username;

    /**
     * 登录账号
     */
    private String account;

    /**
     * 登录密码
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
     * 可服务区域
     */
    private Integer canServiceArea;

    /**
     * 空闲时间段
     * 1=上午8:00-12:00
     * 2=下午14:00-18:00
     */
    private Integer freeTime;

    /**
     * 可服务日期
     * 1234567
     * 对应
     * 周一至周天
     */
    private Integer workDay;

    /**
     * 当前服务状态
     */
    private Integer currentStatus;

    /**
     * 头像地址
     */
    private String avatar;

    /**
     * 账号状态
     * 0-禁用
     * 1-启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}