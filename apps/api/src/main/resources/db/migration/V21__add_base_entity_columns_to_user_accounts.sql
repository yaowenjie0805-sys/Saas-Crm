-- ============================================================
-- V21: Add missing BaseEntity columns to user_accounts table
-- The UserAccount entity extends BaseEntity which includes:
-- version (optimistic locking), created_at, updated_at,
-- created_by, last_modified_by, deleted. The V1 baseline
-- missed these columns for user_accounts.
-- Idempotent: only adds columns if they don't exist.
-- ============================================================

SET @version_exists := (SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_accounts' AND COLUMN_NAME = 'version');
SET @add_version_sql := IF(@version_exists = 0,
    'ALTER TABLE `user_accounts` ADD COLUMN version BIGINT DEFAULT 0 NOT NULL',
    'SELECT ''version column already exists'' as result');
PREPARE stmt_version FROM @add_version_sql; EXECUTE stmt_version; DEALLOCATE PREPARE stmt_version;

SET @created_at_exists := (SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_accounts' AND COLUMN_NAME = 'created_at');
SET @add_created_at_sql := IF(@created_at_exists = 0,
    'ALTER TABLE `user_accounts` ADD COLUMN created_at TIMESTAMP NULL',
    'SELECT ''created_at column already exists'' as result');
PREPARE stmt_created_at FROM @add_created_at_sql; EXECUTE stmt_created_at; DEALLOCATE PREPARE stmt_created_at;

SET @updated_at_exists := (SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_accounts' AND COLUMN_NAME = 'updated_at');
SET @add_updated_at_sql := IF(@updated_at_exists = 0,
    'ALTER TABLE `user_accounts` ADD COLUMN updated_at TIMESTAMP NULL',
    'SELECT ''updated_at column already exists'' as result');
PREPARE stmt_updated_at FROM @add_updated_at_sql; EXECUTE stmt_updated_at; DEALLOCATE PREPARE stmt_updated_at;

SET @created_by_exists := (SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_accounts' AND COLUMN_NAME = 'created_by');
SET @add_created_by_sql := IF(@created_by_exists = 0,
    'ALTER TABLE `user_accounts` ADD COLUMN created_by VARCHAR(80) NULL',
    'SELECT ''created_by column already exists'' as result');
PREPARE stmt_created_by FROM @add_created_by_sql; EXECUTE stmt_created_by; DEALLOCATE PREPARE stmt_created_by;

SET @last_modified_by_exists := (SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_accounts' AND COLUMN_NAME = 'last_modified_by');
SET @add_last_modified_by_sql := IF(@last_modified_by_exists = 0,
    'ALTER TABLE `user_accounts` ADD COLUMN last_modified_by VARCHAR(80) NULL',
    'SELECT ''last_modified_by column already exists'' as result');
PREPARE stmt_last_modified_by FROM @add_last_modified_by_sql; EXECUTE stmt_last_modified_by; DEALLOCATE PREPARE stmt_last_modified_by;

SET @deleted_exists := (SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_accounts' AND COLUMN_NAME = 'deleted');
SET @add_deleted_sql := IF(@deleted_exists = 0,
    'ALTER TABLE `user_accounts` ADD COLUMN deleted BOOLEAN DEFAULT FALSE NOT NULL',
    'SELECT ''deleted column already exists'' as result');
PREPARE stmt_deleted FROM @add_deleted_sql; EXECUTE stmt_deleted; DEALLOCATE PREPARE stmt_deleted;
