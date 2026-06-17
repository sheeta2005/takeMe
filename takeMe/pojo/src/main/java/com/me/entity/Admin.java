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
@TableName("admin")
public class Admin {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String avatar;
    private String username;
    private String password;
    private String realName;

    private LocalDateTime createTime;
    private LocalDateTime lastLoginTime;
}