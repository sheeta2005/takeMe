package com.me.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("volunteer_points_record")
public class VolunteerPointsRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long volunteerId;
    private Long orderId;
    private Integer points;
    private Integer type;
    private String description;

    private LocalDateTime createTime;
}