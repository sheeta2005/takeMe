package com.me.service.task;

import com.me.mapper.FailedMessageMapper;
import com.me.mapper.OutboxMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.function.IntSupplier;

@Component
@Slf4j
public class MqCleanupTask {
    private final OutboxMapper outboxMapper;
    private final FailedMessageMapper failedMessageMapper;
    private final TransactionTemplate transaction;
    private final int outboxRetentionDays;
    private final int failedRetentionDays;
    private final int batchSize;
    private final int maxBatches;

    public MqCleanupTask(OutboxMapper outboxMapper, FailedMessageMapper failedMessageMapper,
                         PlatformTransactionManager manager,
                         @Value("${mq.cleanup.outbox-retention-days:7}") int outboxRetentionDays,
                         @Value("${mq.cleanup.failed-retention-days:30}") int failedRetentionDays,
                         @Value("${mq.cleanup.batch-size:500}") int batchSize,
                         @Value("${mq.cleanup.max-batches:10}") int maxBatches) {
        if (outboxRetentionDays < 1 || failedRetentionDays < 1 || batchSize < 1 || maxBatches < 1) {
            throw new IllegalArgumentException("消息清理保留天数、批大小及批次数必须为正数");
        }
        this.outboxMapper = outboxMapper;
        this.failedMessageMapper = failedMessageMapper;
        this.outboxRetentionDays = outboxRetentionDays;
        this.failedRetentionDays = failedRetentionDays;
        this.batchSize = batchSize;
        this.maxBatches = maxBatches;
        this.transaction = new TransactionTemplate(manager);
        // 每批独立提交，避免清扫历史积压时持有大事务和大量行锁。
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.transaction.setTimeout(5);
    }

    // 不依赖 MQ 开关；停用 MQ 时仍可清扫以前已确认的消息。
    @Scheduled(fixedDelay = 3600000, initialDelay = 60000)
    public int cleanupOutbox() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(outboxRetentionDays);
        return cleanup("已确认发件箱", () -> outboxMapper.deleteConfirmedBefore(cutoff, batchSize));
    }

    // 已重投不等于已完成，仅人工结案后才开始计算保留期。
    @Scheduled(fixedDelay = 3600000, initialDelay = 60000)
    public int cleanupFailedMessages() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(failedRetentionDays);
        return cleanup("已结案失败消息", () -> failedMessageMapper.deleteClosedBefore(cutoff, batchSize));
    }

    private int cleanup(String category, IntSupplier deleteBatch) {
        int total = 0;
        try {
            for (int batch = 0; batch < maxBatches; batch++) {
                int deleted = transaction.execute(status -> deleteBatch.getAsInt());
                total += deleted;
                if (deleted < batchSize) break;
            }
            log.info("消息清理完成: category={}, deleted={}", category, total);
        } catch (Exception e) {
            // 已提交批次不回滚，失败批次交由下一轮重新处理。
            log.error("消息清理中断: category={}, committed={}", category, total, e);
        }
        return total;
    }
}
