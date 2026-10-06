-- 先选择目标库；重复执行不会覆盖历史订单，历史 request_id 保持 NULL。
SET NAMES utf8mb4;
SET @ddl = IF(EXISTS(
    SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
    AND table_name = 'order' AND column_name = 'request_id'),
    'SELECT 1', 'ALTER TABLE `order` ADD COLUMN request_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT ''提交幂等标识''');
PREPARE migration FROM @ddl;
EXECUTE migration;
DEALLOCATE PREPARE migration;

SET @ddl = IF(EXISTS(
    SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
    AND table_name = 'order' AND index_name = 'uk_order_request'),
    'SELECT 1', 'ALTER TABLE `order` ADD UNIQUE KEY uk_order_request (user_id, request_id)');
PREPARE migration FROM @ddl;
EXECUTE migration;
DEALLOCATE PREPARE migration;

-- 如历史订单号重复，本语句失败并要求人工核对，禁止自动删除订单。
SET @ddl = IF(EXISTS(
    SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
    AND table_name = 'order' AND index_name = 'uk_order_no'),
    'SELECT 1', 'ALTER TABLE `order` ADD UNIQUE KEY uk_order_no (order_no)');
PREPARE migration FROM @ddl;
EXECUTE migration;
DEALLOCATE PREPARE migration;
