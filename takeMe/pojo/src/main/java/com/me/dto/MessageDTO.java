package com.me.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {

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
}