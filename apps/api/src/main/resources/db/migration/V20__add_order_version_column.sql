-- Add optimistic lock version column for order records (MySQL-compatible syntax).
ALTER TABLE order_records
ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version for concurrency control';