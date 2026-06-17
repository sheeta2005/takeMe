package com.me.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalResultMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long approvalId;

    private String type;

    private Long applicantId;

    private String applicantName;

    private String result;

    private String remark;

    private LocalDateTime approveTime;
}

