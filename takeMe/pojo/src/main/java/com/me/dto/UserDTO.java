package com.me.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String realName;
    private String username;
    private String phone;
    private String password;
    private String avatar;
    private Integer gender;
    private Integer age;
    private String emergencyName;
    private String emergencyPhone;
    private Integer status;
}