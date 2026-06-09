package com.me.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.me.dto.PageResultDTO;
import com.me.entity.Approval;

public interface ApprovalService extends IService<Approval> {
    IPage<Approval> getApprovalPage(
            String type,
            String status,
            String keyword,
            String startDate,
            String endDate,
            PageResultDTO pageResultDTO
    );

    Approval getApprovalDetail(Long id);

    boolean approveApplication(Long id, String remark);

    boolean rejectApplication(Long id, String remark);
}
