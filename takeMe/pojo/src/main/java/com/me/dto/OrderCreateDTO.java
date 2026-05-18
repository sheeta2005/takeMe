package com.me.dto;
import lombok.Data;
@Data
public class OrderCreateDTO {
    private Long elderlyId;
    private Long volunteerId;
    private String serviceArea;
    private Integer serviceDay;
    private Integer serviceTime;
}