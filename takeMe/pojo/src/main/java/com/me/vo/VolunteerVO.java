package com.me.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerVO {

    private Long id;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime lastLoginTime;

    private String realName;
    private String username;
    private String phone;
    private String avatar;

    private Integer gender;
    private Integer age;
    private String address;

    private Integer serviceDays;
    private Integer serviceType;
    private Integer workStatus;
    private Integer totalServiceHours;

    private String emergencyName;
    private String emergencyPhone;
}