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
@TableName("volunteer")
public class Volunteer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime lastLoginTime;

    private String realName;
    private String username;
    private String phone;
    private String password;
    private String avatar;

    private Integer gender;
    private Integer age;
    private String address;

    private String serviceDays;//0 周天 1周一....6 周六
    
    private Integer workStatus;
    private Integer totalServiceHours;

    private String emergencyName;
    private String emergencyPhone;
}