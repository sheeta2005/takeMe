package com.me.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {
    private Long id;
    private String username;
    private int role; // 1.admin 2.user 3.volunteer
    private String token;
}