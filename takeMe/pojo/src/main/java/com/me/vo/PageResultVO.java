package com.me.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResultVO<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<T> records;  // 将data字段改为records，以匹配前端期望
    private Long total;
    private Integer pageNum;
    private Integer pageSize;

    //供mybatis_plus分页软件使用
    public static <T> PageResultVO<T> from(IPage<T> page) {
        return new PageResultVO<>(
                page.getRecords(),  // 对应records字段
                page.getTotal(),    // 对应total字段
                Math.toIntExact(page.getCurrent()),  // 对应pageNum字段
                Math.toIntExact(page.getSize())     // 对应pageSize字段
        );
    }
}
