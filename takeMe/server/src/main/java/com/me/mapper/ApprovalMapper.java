package com.me.mapper;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.me.entity.Approval;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ApprovalMapper extends BaseMapper<Approval> {
    //查询并锁定审批记录
    Approval selectForUpdate(@Param("id") Long id);

    // 审批列表仅使用单表条件与分页。
    default Page<Approval> selectSearchPage(Page<Approval> page, String type, String status, String keyword, String startDate, String endDate) {
        LambdaQueryWrapper<Approval> wrapper = new LambdaQueryWrapper<>();

        if (type != null && !type.trim().isEmpty()) {
            wrapper.eq(Approval::getType, type);
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(Approval::getStatus, status);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(Approval::getApplicantName, keyword)
                    .or()
                    .like(Approval::getApplicantId, keyword));
        }
        if (startDate != null && !startDate.trim().isEmpty()) {
            wrapper.ge(Approval::getCreateTime, startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            wrapper.le(Approval::getCreateTime, endDate + " 23:59:59");
        }

        wrapper.orderByDesc(Approval::getCreateTime);
        return selectPage(page, wrapper);
    }

    // 审批仅能从待审批状态迁移一次，备注允许清空。
    int decidePending(@Param("id") Long id, @Param("result") String result, @Param("remark") String remark, @Param("updateTime") LocalDateTime updateTime);

    // 统计待审批数量。
    Long countPending();
}
