package com.me.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单详细信息")
public class OrderVO {

    @Schema(description = "订单ID", example = "1001")
    private Long id;
    
    @Schema(description = "订单号", example = "ORD202606170001")
    private String orderNo;
    
    @Schema(description = "用户ID", example = "100")
    private Long userId;
    
    @Schema(description = "志愿者ID列表（逗号分隔）", example = "201,202")
    private String volunteerIds;

    @Schema(description = "订单总价（分）", example = "5000")
    private Integer totalPrice;
    
    @Schema(description = "服务日期", example = "2026-06-20")
    private String serviceDate;
    
    @Schema(description = "服务时间段", example = "09:00-11:00")
    private String serviceTime;
    
    @Schema(description = "服务地址", example = "北京市朝阳区xxx小区")
    private String address;
    
    @Schema(description = "备注信息", example = "请提前联系")
    private String remark;

    @Schema(description = "订单状态（0-待支付 1-待审批 2-进行中 3-已完成 4-已取消 5-已拒绝）", example = "2")
    private Integer status;
    
    @Schema(description = "创建时间", example = "2026-06-17 10:30:00")
    private LocalDateTime createTime;
    
    @Schema(description = "完成时间", example = "2026-06-20 11:00:00")
    private LocalDateTime completeTime;
    
    @Schema(description = "是否已评价（0-否 1-是）", example = "0")
    private Integer isReviewed;

    @Schema(description = "下单用户姓名", example = "张三")
    private String userName;
    
    @Schema(description = "下单用户电话", example = "13800138000")
    private String userPhone;

    @Schema(description = "订单项列表")
    private List<OrderItemVO> items;
}