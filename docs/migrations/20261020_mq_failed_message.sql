-- 消息队列修缮：在启用失败归档消费者之前执行；可重复执行，不清理已有消息。
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS mq_failed_message (
    id CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY COMMENT '来源队列和事件标识的摘要',
    message_id VARCHAR(255) NOT NULL COMMENT '重投时保留的原消息标识',
    source_queue VARCHAR(255) NOT NULL COMMENT '无法确定来源时留空，禁止自动猜测重放',
    exchange_name VARCHAR(255) NOT NULL,
    routing_key VARCHAR(255) NOT NULL,
    body LONGBLOB NOT NULL COMMENT '原始消息字节，包含格式错误的消息',
    headers_json LONGTEXT NOT NULL,
    content_type VARCHAR(255) NULL,
    content_encoding VARCHAR(255) NULL,
    correlation_id VARCHAR(255) NULL,
    reason VARCHAR(64) NOT NULL,
    last_error TEXT NULL,
    occurrences INT NOT NULL DEFAULT 1,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=已归档待核查，1=Broker已确认重投，不代表业务成功',
    replay_count INT NOT NULL DEFAULT 0,
    last_replay_error VARCHAR(2000) NULL,
    replayed_by BIGINT NULL,
    last_replay_time DATETIME NULL,
    create_time DATETIME NOT NULL,
    last_failure_time DATETIME NOT NULL,
    KEY idx_failed_status_time (status, last_failure_time),
    KEY idx_failed_time (last_failure_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='失败消息归档及人工重放记录';
