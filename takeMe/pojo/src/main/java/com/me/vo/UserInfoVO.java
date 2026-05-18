package com.me.vo;

import lombok.Data;

@Data
public class UserInfoVO {
    private Long id;
    private String username;
    private String account;
    private String phone;
    private Integer age;
    private Integer gender;
    private String address;
    private String walkingRange;
    private String avatar;
    private Integer status;
}