package com.me.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String realName;
    private String username;
    private String phone;
    private String avatar;

    private Integer gender;
    private Integer age;

    private String emergencyName;
    private String emergencyPhone;

    private Integer status;
    private String createTime;     // 格式化后字符串，如 yyyy-MM-dd HH:mm:ss
    private String lastLoginTime;
}