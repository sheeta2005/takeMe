-- 第 2 至 10 项修复的增量迁移；先选择目标数据库，再执行本文件。
-- 重复执行不会删除表、清空消息或覆盖已有业务关联。
SET NAMES utf8mb4;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'approval' AND column_name = 'business_id'),
    'SELECT 1',
    'ALTER TABLE approval ADD COLUMN business_id BIGINT NULL COMMENT ''审批对应的具体业务记录 ID''');
PREPARE migration FROM @ddl;
EXECUTE migration;
DEALLOCATE PREPARE migration;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'message' AND column_name = 'event_id'),
    'SELECT 1',
    'ALTER TABLE message ADD COLUMN event_id VARCHAR(64) NULL COMMENT ''消息事件去重标识''');
PREPARE migration FROM @ddl;
EXECUTE migration;
DEALLOCATE PREPARE migration;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
        AND table_name = 'message' AND index_name = 'uk_message_event_receiver'),
    'SELECT 1',
    'ALTER TABLE message ADD UNIQUE KEY uk_message_event_receiver (event_id, receiver_type, receiver_id)');
PREPARE migration FROM @ddl;
EXECUTE migration;
DEALLOCATE PREPARE migration;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
        AND table_name = 'order_item' AND index_name = 'idx_volunteer_status'),
    'SELECT 1',
    'ALTER TABLE order_item ADD KEY idx_volunteer_status (volunteer_id, item_status, id)');
PREPARE migration FROM @ddl;
EXECUTE migration;
DEALLOCATE PREPARE migration;

CREATE TABLE IF NOT EXISTS mq_outbox (
    id VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '稳定的消息事件 ID',
    exchange_name VARCHAR(255) NOT NULL,
    routing_key VARCHAR(255) NOT NULL,
    payload_type VARCHAR(255) NOT NULL,
    payload LONGTEXT NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=待发送，1=已确认',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_time DATETIME NOT NULL,
    last_error VARCHAR(2000) NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    KEY idx_outbox_due (status, next_attempt_time, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='与业务事务一起提交的待发送消息';

-- 注册及日期变更的业务对象就是申请人，不需要猜测其他记录。
UPDATE approval SET business_id = applicant_id
WHERE business_id IS NULL AND type IN ('register', 'service_days_change');

-- 历史请假只在完整申请内容唯一匹配时关联，存在歧义的记录保持未关联。
UPDATE approval a
JOIN (
    SELECT a2.id AS approval_id, MAX(l.id) AS leave_id
    FROM approval a2
    JOIN volunteer_leave l ON l.volunteer_id = a2.applicant_id
        AND a2.content = CONCAT('请假类型：', IF(l.type = 0, '事假', '病假'),
            '，时间：', DATE_FORMAT(l.start_time, IF(SECOND(l.start_time) = 0, '%Y-%m-%dT%H:%i', '%Y-%m-%dT%H:%i:%s')),
            ' 至 ', DATE_FORMAT(l.end_time, IF(SECOND(l.end_time) = 0, '%Y-%m-%dT%H:%i', '%Y-%m-%dT%H:%i:%s')),
            '，原因：', l.reason)
    WHERE a2.type = 'leave' AND a2.business_id IS NULL
    GROUP BY a2.id
    HAVING COUNT(*) = 1
) exact_match ON exact_match.approval_id = a.id
SET a.business_id = exact_match.leave_id;

-- 未关联的历史请假必须人工核对，应用不会自动审批“最新一条”。
SELECT id, applicant_id, create_time FROM approval
WHERE type = 'leave' AND status = 'pending' AND business_id IS NULL;
