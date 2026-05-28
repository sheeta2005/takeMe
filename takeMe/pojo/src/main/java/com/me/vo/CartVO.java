package com.me.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartVO {

    private Long id;
    private Long userId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}