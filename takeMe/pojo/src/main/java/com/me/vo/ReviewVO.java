package com.me.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewVO {
    private Long id;
    private Long orderId;
    private Long userId;
    private Long volunteerId;
    private Integer rating;
    private String comment;
    private LocalDateTime createTime;
}