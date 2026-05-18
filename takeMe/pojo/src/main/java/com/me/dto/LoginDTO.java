package com.me.dto;

import lombok.Data;

@Data
public class LoginDTO {
    private String account;
    private String password;
    // 身份：admin1/user2/volunteer3
    private Integer role;
}