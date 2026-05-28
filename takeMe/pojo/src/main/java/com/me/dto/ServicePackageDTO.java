package com.me.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServicePackageDTO {

    private Long id;
    private String name;
    private Integer type;
    private Integer price;
    private String description;
    private String image;
    private Integer status;
}