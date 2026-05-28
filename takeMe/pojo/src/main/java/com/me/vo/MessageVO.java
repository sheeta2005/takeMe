package com.me.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageVO {

    private Long id;
    private Long receiverId;
    private Integer receiverType;
    private Integer type;
    private String title;
    private String content;
    private Integer isRead;

    private Long relatedOrderId;
    private Long relatedUserId;
    private Long relatedVolunteerId;
    private String relatedUrl;

    private LocalDateTime createTime;
}