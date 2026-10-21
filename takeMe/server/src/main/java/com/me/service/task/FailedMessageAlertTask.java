package com.me.service.task;

import com.me.service.FailedMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FailedMessageAlertTask {
    private final FailedMessageService failedMessageService;

    @Scheduled(fixedDelay = 60000)
    public void reportRecentFailures() {
        for (var failure : failedMessageService.recentFailures()) {
            log.error("最近一分钟消息失败汇总: sourceQueue={}, reason={}, count={}",
                    failure.get("sourceQueue"), failure.get("reason"), failure.get("messageCount"));
        }
    }
}
