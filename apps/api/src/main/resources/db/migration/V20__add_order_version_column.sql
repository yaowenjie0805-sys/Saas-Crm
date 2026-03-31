-- 添加乐观锁版本号到 order_records 表
-- 用于并发控制，防止数据覆盖

ALTER TABLE order_records
ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- 添加注释
COMMENT ON COLUMN order_records.version IS '乐观锁版本号，用于并发控制';
