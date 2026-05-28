package com.me.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressVO {
    private Long id;
    private String address;
    private Long userId; // 新增：关联用户ID
    private Integer isDefault;
    private LocalDateTime createTime;
}