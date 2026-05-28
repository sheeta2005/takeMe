package com.me.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerPointsRecordVO {

    private Long id;
    private Long volunteerId;
    private Long orderId;
    private Integer points;
    private Integer type;
    private String description;
    private LocalDateTime createTime;
}