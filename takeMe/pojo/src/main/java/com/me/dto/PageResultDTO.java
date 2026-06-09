package com.me.dto;

import lombok.Data;

@Data
public class PageResultDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
