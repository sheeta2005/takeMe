package com.me.dto;
import lombok.Data;
@Data
public class OrderApproveDTO {
    private Long orderId;
    private Long adminId;
    private Integer approveStatus;
}
