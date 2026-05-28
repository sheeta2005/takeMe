package com.me.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageQueryDTO {
    // 第几页
    private Integer pageNum;
    // 每页几条
    private Integer pageSize;
}