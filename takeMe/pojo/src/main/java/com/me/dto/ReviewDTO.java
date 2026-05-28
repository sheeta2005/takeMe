package com.me.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {
    private Long id;
    private Long orderId;
    private Long userId;
    private Long volunteerId;
    private Integer rating;
    private String comment;
}