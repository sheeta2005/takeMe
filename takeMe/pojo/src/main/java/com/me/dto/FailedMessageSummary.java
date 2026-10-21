package com.me.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FailedMessageSummary {
    private String id;
    private String messageId;
    private String sourceQueue;
    private String exchangeName;
    private String routingKey;
    private String reason;
    private String lastError;
    private Integer occurrences;
    private Integer status;
    private Integer replayCount;
    private String lastReplayError;
    private Long replayedBy;
    private LocalDateTime createTime;
    private LocalDateTime lastFailureTime;
    private LocalDateTime lastReplayTime;
    private Long closedBy;
    private LocalDateTime closedTime;
    private String closeReason;
}
