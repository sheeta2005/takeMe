-- 先选择目标库。迁移前列出历史重复项；如有重复，添加唯一约束会失败，不自动删改评价。
SET NAMES utf8mb4;
SELECT order_item_id, COUNT(*) AS duplicate_count
FROM review WHERE order_item_id IS NOT NULL
GROUP BY order_item_id HAVING COUNT(*) > 1;

-- 检查是否已有等价的单列唯一约束，支持重复执行。
SET @ddl = IF(EXISTS(
    SELECT index_name FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'review' AND non_unique = 0
    GROUP BY index_name
    HAVING COUNT(*) = 1 AND MAX(column_name) = 'order_item_id'),
    'SELECT 1', 'ALTER TABLE review ADD UNIQUE KEY uk_review_order_item (order_item_id)');
PREPARE migration FROM @ddl;
EXECUTE migration;
DEALLOCATE PREPARE migration;
