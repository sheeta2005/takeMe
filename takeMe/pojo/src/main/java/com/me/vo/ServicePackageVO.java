package com.me.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServicePackageVO {

    private Long id;
    private String name;
    private Integer type;
    private Integer price;
    private String description;
    private String image;
    private Integer status;

    private LocalDateTime createTime;
}