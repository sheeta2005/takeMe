package com.me.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.me.dto.PageResultDTO;
import com.me.entity.Approval;

public interface ApprovalService extends IService<Approval> {
    //分页查询审批记录
    IPage<Approval> getApprovalPage(
            String type,
            String status,
            String keyword,
            String startDate,
            String endDate,
            PageResultDTO pageResultDTO
    );

    //查询审批详情
    Approval getApprovalDetail(Long id);

    //通过审批申请
    boolean approveApplication(Long id, String remark);

    //拒绝审批申请
    boolean rejectApplication(Long id, String remark);
}
