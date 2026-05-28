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
@TableName("message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long receiverId;          // 接收者ID
    private Integer receiverType;     // 0=老人 1=志愿者
    private Integer type;            // 0=系统 1=订单 2=服务
    private String title;            // 标题
    private String content;          // 内容
    private Integer isRead;          // 0=未读 1=已读

    private Long relatedOrderId;
    private Long relatedUserId;
    private Long relatedVolunteerId;
    private String relatedUrl;

    private LocalDateTime createTime;
}