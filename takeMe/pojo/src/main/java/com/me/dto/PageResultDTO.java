package com.me.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "分页查询参数")
public class PageResultDTO {
    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;
    
    @Schema(description = "每页数量", example = "10")
    private Integer pageSize = 10;

    // 负数分页会绕过 LIMIT；统一限制小区列表的单次查询规模。
    public void setPageNum(Integer pageNum) {
        if (pageNum == null || pageNum < 1) {
            throw new IllegalArgumentException("页码必须大于等于1");
        }
        this.pageNum = pageNum;
    }

    public void setPageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("每页数量必须在1到100之间");
        }
        this.pageSize = pageSize;
    }
}
