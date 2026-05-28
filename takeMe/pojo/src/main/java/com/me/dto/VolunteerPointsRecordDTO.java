package com.me.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerPointsRecordDTO {

    private Long id;
    private Long volunteerId;
    private Long orderId;
    private Integer points;
    private Integer type;
    private String description;
}