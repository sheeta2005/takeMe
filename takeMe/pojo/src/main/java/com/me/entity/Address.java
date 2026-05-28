package com.me.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("address")
public class Address {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId; // 新增：关联用户ID
    private String address;
    private Integer isDefault;
    private LocalDateTime createTime;
}