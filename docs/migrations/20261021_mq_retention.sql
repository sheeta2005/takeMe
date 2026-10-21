-- 消息生命周期修复：先执行迁移，再发布结案、归档和清理功能；可重复执行。
SET NAMES utf8mb4;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'mq_failed_message' AND column_name = 'closed_by'),
    'SELECT 1',
    'ALTER TABLE mq_failed_message ADD COLUMN closed_by BIGINT NULL COMMENT ''结案管理员''');
PREPARE migration FROM @ddl;
EXECUTE migration;
DEALLOCATE PREPARE migration;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'mq_failed_message' AND column_name = 'closed_time'),
    'SELECT 1',
    'ALTER TABLE mq_failed_message ADD COLUMN closed_time DATETIME NULL COMMENT ''人工核查结案时间''');
PREPARE migration FROM @ddl;
EXECUTE migration;
DEALLOCATE PREPARE migration;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'mq_failed_message' AND column_name = 'close_reason'),
    'SELECT 1',
    'ALTER TABLE mq_failed_message ADD COLUMN close_reason VARCHAR(1000) NULL COMMENT ''业务恢复或无需处理的结案依据''');
PREPARE migration FROM @ddl;
EXECUTE migration;
DEALLOCATE PREPARE migration;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
        AND table_name = 'mq_outbox' AND index_name = 'idx_outbox_cleanup'),
    'SELECT 1',
    'ALTER TABLE mq_outbox ADD KEY idx_outbox_cleanup (status, update_time, id)');
PREPARE migration FROM @ddl;
EXECUTE migration;
DEALLOCATE PREPARE migration;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
        AND table_name = 'mq_failed_message' AND index_name = 'idx_failed_cleanup'),
    'SELECT 1',
    'ALTER TABLE mq_failed_message ADD KEY idx_failed_cleanup (status, closed_time, id)');
PREPARE migration FROM @ddl;
EXECUTE migration;
DEALLOCATE PREPARE migration;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'mq_failed_message' AND column_name = 'status'
        AND column_comment = '0=待核查，1=Broker已确认重投，2=人工结案；重投不代表业务成功'),
    'SELECT 1',
    'ALTER TABLE mq_failed_message MODIFY COLUMN status TINYINT NOT NULL DEFAULT 0 COMMENT ''0=待核查，1=Broker已确认重投，2=人工结案；重投不代表业务成功''');
PREPARE migration FROM @ddl;
EXECUTE migration;
DEALLOCATE PREPARE migration;
