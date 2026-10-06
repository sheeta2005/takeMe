package com.me.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mq_outbox")
public class OutboxMessage {
    @TableId(type = IdType.INPUT)
    private String id;
    private String exchangeName;
    private String routingKey;
    private String payloadType;
    private String payload;
    private Integer status; // 0=待发送，1=已确认；失败仍保留为待发送
    private Integer attempts;
    private LocalDateTime nextAttemptTime;
    private String lastError;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
