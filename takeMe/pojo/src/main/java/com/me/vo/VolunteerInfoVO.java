package com.me.vo;

import lombok.Data;

@Data
public class VolunteerInfoVO {
    private Long id;
    private String username;
    private String account;
    private String phone;
    private Integer age;
    private Integer gender;
    private String address;
    private String availableRange;
    private Integer freeTime;
    private Integer workDay;
    private Integer currentStatus;
    private String avatar;
    private Integer status;
}