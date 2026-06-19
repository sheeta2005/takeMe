package com.me.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerStartTimeoutMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderItemId;

    private Long volunteerId;

    private Long orderId;

    private String orderNo;
}
