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
@TableName("cart")
public class Cart {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;       // 老人ID
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}