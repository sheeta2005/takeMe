package com.me.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResultVO<T> {
    // 总条数
    private Long total;
    // 当前页数据
    private List<T> records;
}