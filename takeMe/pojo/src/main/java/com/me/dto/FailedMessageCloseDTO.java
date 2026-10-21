package com.me.dto;

import lombok.Data;

// 结案原因应说明业务已恢复，或确认无需继续处理的依据。
@Data
public class FailedMessageCloseDTO {
    private String reason;
}
