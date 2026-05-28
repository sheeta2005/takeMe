package com.me.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerLeaveVO {

    private Long id;
    private Long volunteerId;
    private Integer type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private Integer status;
    private LocalDateTime createTime;
}