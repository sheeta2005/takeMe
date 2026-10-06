-- 先选择业务库；本次自动执行只允许隔离压测库，正式库部署前备份并审核。
SET NAMES utf8mb4;

-- 老人列表先按所属人过滤，再按创建时间及主键稳定分页。
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'order' AND index_name = 'idx_order_user_created'),
    'SELECT 1', 'ALTER TABLE `order` ADD KEY idx_order_user_created (user_id, create_time DESC, id DESC)');
PREPARE migration FROM @ddl;
EXECUTE migration;
DEALLOCATE PREPARE migration;

-- 可接服务的 COUNT 可覆盖日期/时间及父订单关联；页数据仍按创建时间排序。
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'order_item' AND index_name = 'idx_item_available_time'),
    'SELECT 1', 'ALTER TABLE order_item ADD KEY idx_item_available_time (volunteer_id, item_status, service_date, service_time, order_id)');
PREPARE migration FROM @ddl;
EXECUTE migration;
DEALLOCATE PREPARE migration;

-- 支付与退款时间分开建覆盖索引，避免跨两列 OR 导致的整表访问。
-- 实测优化器会交叉选择两个索引，补齐另一时间列，保证任一索引都无需回表读取统计条件。
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'payment_transaction' AND index_name = 'idx_payment_mock_time'),
    'SELECT 1', 'ALTER TABLE payment_transaction ADD KEY idx_payment_mock_time (payment_method, payment_time, payment_status, amount, refund_time)');
PREPARE migration FROM @ddl;
EXECUTE migration;
DEALLOCATE PREPARE migration;

SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'payment_transaction' AND index_name = 'idx_refund_mock_time'),
    'SELECT 1', 'ALTER TABLE payment_transaction ADD KEY idx_refund_mock_time (payment_method, refund_time, payment_status, amount, payment_time)');
PREPARE migration FROM @ddl;
EXECUTE migration;
DEALLOCATE PREPARE migration;
