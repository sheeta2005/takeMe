package com.me.service.task;

import com.me.service.FailedMessageService;
import com.me.mapper.OutboxMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FailedMessageAlertTask {
    private final FailedMessageService failedMessageService;
    private final OutboxMapper outboxMapper;

    @Value("${mq.alert.outbox-count:10000}")
    private long outboxCount;
    @Value("${mq.alert.outbox-age-minutes:10}")
    private long outboxAgeMinutes;
    @Value("${mq.alert.failed-count:1000}")
    private long failedCount;
    @Value("${mq.alert.failed-age-hours:24}")
    private long failedAgeHours;

    @Scheduled(fixedDelay = 60000)
    public void reportRecentFailures() {
        for (var failure : failedMessageService.recentFailures()) {
            log.error("最近一分钟消息失败汇总: sourceQueue={}, reason={}, count={}",
                    failure.get("sourceQueue"), failure.get("reason"), failure.get("messageCount"));
        }
    }

    // 与新增失败告警分开调度，持续发现长期滞留而没有新异常的积压。
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void reportBacklog() {
        report("发件箱待发送", outboxMapper.pendingSummary(LocalDateTime.now().minusMinutes(outboxAgeMinutes)), outboxCount);
        for (var summary : failedMessageService.outstandingSummary(LocalDateTime.now().minusHours(failedAgeHours))) {
            report("失败消息状态" + summary.get("status"), summary, failedCount);
        }
    }

    private void report(String category, Map<String, Object> summary, long threshold) {
        long count = ((Number) summary.get("messageCount")).longValue();
        long overdue = ((Number) summary.get("overdueCount")).longValue();
        if (count >= threshold || overdue > 0) {
            log.error("消息积压告警: category={}, count={}, overdue={}", category, count, overdue);
        }
    }
}
