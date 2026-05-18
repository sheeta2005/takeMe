package com.me.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 服务订单实体类
 * 对应数据库表 service_order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("service_order")
public class ServiceOrder {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单编号（展示用）
     */
    private String orderNo;

    /**
     * 服务类型
     * 1=助餐服务
     * 2=助洁服务
     * 3=助医服务
     * 4=代购服务
     */
    private Integer category;

    /**
     * 老人用户ID
     */
    private Long elderlyId;

    /**
     * 志愿者用户ID
     */
    private Long volunteerId;

    /**
     * 服务区域
     */
    private Integer serviceArea;

    /**
     * 服务日期（例如：1=周一，2=周二...）
     */
    private Integer serviceDay;

    /**
     * 服务时段
     * 1=上午（8:00-12:00）
     * 2=下午（14:00-18:00）
     */
    private Integer serviceTime;

    /**
     * 服务备注/特殊需求
     */
    private String remark;

    /**
     * 订单金额（单位：元）
     */
    private Integer amount;

    /**
     * 订单状态
     * 0=待接单
     * 1=已接单
     * 2=已完成
     * 3=已取消
     */
    private Integer orderStatus;

    /**
     * 管理员ID（派单/审核人）
     */
    private Long adminId;

    /**
     * 订单创建时间（下单时间）
     */
    private LocalDateTime createTime;

    /**
     * 服务开始时间
     */
    private LocalDateTime serviceStartTime;

    /**
     * 服务结束时间
     */
    private LocalDateTime serviceEndTime;
}