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
@TableName("volunteer_leave")
public class VolunteerLeave {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long volunteerId;
    private Byte type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private Byte status;
    private LocalDateTime createTime;
}