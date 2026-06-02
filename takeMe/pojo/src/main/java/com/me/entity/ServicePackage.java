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
    private Integer type;// 0shop 1clean 2meal 3medical 4 companion//0 = 代购 1 = 助洁 2 = 助餐 3 = 助医 4 = 陪伴
    private Integer price;
    private String description;
    private String image;
    private Integer status;

    private LocalDateTime createTime;
}