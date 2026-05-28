package com.me.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerDTO {

    private Long id;
    private Integer status;

    private String realName;
    private String username;
    private String phone;
    private String password;
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