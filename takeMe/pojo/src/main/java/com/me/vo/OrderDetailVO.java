package com.me.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OrderDetailVO {
    private Long id;
    private Long elderlyId;
    private String elderlyName;
    private Long volunteerId;
    private String volunteerName;
    private String serviceArea;
    private Integer serviceDay;
    private String serviceDayStr;
    private Integer serviceTime;
    private String serviceTimeStr;
    private Integer orderStatus;
    private String orderStatusStr;
    private Long adminId;
    private String adminName;
    private LocalDateTime createTime;
    private LocalDateTime serviceStartTime;
    private LocalDateTime serviceEndTime;
}