package com.me.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.me.entity.Approval;

public interface ApprovalService extends IService<Approval> {

    /**
     * 分页查询审批列表
     * @param page 页码
     * @param pageSize 每页数量
     * @param type 申请类型
     * @param status 审批状态
     * @param keyword 关键词（申请人姓名或ID）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分页结果
     */
    Page<Approval> getApprovalPage(
            Integer page,
            Integer pageSize,
            String type,
            String status,
            String keyword,
            String startDate,
            String endDate
    );

    /**
     * 获取审批详情
     * @param id 审批ID
     * @return 审批信息
     */
    Approval getApprovalDetail(Long id);

    /**
     * 通过申请
     * @param id 审批ID
     * @param remark 审批意见
     * @return 是否成功
     */
    boolean approveApplication(Long id, String remark);

    /**
     * 驳回申请
     * @param id 审批ID
     * @param remark 驳回原因
     * @return 是否成功
     */
    boolean rejectApplication(Long id, String remark);
}
