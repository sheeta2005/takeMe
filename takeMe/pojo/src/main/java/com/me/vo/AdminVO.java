package com.me.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminVO {

    private Long id;
    private String username;
    private String realName;
    private LocalDateTime createTime;
    private LocalDateTime lastLoginTime;
}