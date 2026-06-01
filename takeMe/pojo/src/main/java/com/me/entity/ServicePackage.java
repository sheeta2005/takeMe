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
@TableName("service_package")
public class ServicePackage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private Integer type;// 0shop 1clean 2meal 3medical 4 companion
    private Integer price;
    private String description;
    private String image;
    private Integer status;

    private LocalDateTime createTime;
}