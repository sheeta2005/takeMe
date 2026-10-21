package com.me.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mq_failed_message")
public class FailedMessage {
    @TableId(type = IdType.INPUT)
    private String id;
    private String messageId;
    private String sourceQueue;
    private String exchangeName;
    private String routingKey;
    private byte[] body;
    private String headersJson;
    private String contentType;
    private String contentEncoding;
    private String correlationId;
    private String reason;
    private String lastError;
    private Integer occurrences;
    private Integer status; // 0=已归档待核查，1=Broker 已确认重投，不代表业务成功。
    private Integer replayCount;
    private String lastReplayError;
    private Long replayedBy;
    private LocalDateTime lastReplayTime;
    private LocalDateTime createTime;
    private LocalDateTime lastFailureTime;
}
