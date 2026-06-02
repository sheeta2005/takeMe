package com.me.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("approval")
public class Approval {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String type;           // register=注册, leave=请假, service_change=服务变更, points_appeal=积分申诉
    private Long applicantId;      // 申请人ID
    private String applicantName;  // 申请人姓名
    private String content;        // 申请内容
    private String status;         // pending=待审核, approved=已通过, rejected=已驳回

    private String remark;         // 审批意见
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
