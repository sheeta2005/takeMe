package com.me.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerLeaveDTO {

    private Long id;
    private Long volunteerId;
    private Integer type;
    private String startTime;
    private String endTime;
    private String reason;
    private Integer status;
}