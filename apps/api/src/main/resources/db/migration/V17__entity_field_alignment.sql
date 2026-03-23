-- ============================================================
-- V17: 对齐实体新增字段（客户/联系人/审批事件/审批任务）
-- 仅补充缺失字段，不改变既有语义
-- ============================================================

-- customers
SET @customers_industry_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'industry'
);
SET @customers_industry_sql := IF(@customers_industry_exists = 0,
    'ALTER TABLE customers ADD COLUMN industry VARCHAR(120)',
    'SELECT 1');
PREPARE stmt_customers_industry FROM @customers_industry_sql;
EXECUTE stmt_customers_industry;
DEALLOCATE PREPARE stmt_customers_industry;

SET @customers_scale_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'scale'
);
SET @customers_scale_sql := IF(@customers_scale_exists = 0,
    'ALTER TABLE customers ADD COLUMN scale VARCHAR(40)',
    'SELECT 1');
PREPARE stmt_customers_scale FROM @customers_scale_sql;
EXECUTE stmt_customers_scale;
DEALLOCATE PREPARE stmt_customers_scale;

SET @customers_phone_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'phone'
);
SET @customers_phone_sql := IF(@customers_phone_exists = 0,
    'ALTER TABLE customers ADD COLUMN phone VARCHAR(40)',
    'SELECT 1');
PREPARE stmt_customers_phone FROM @customers_phone_sql;
EXECUTE stmt_customers_phone;
DEALLOCATE PREPARE stmt_customers_phone;

SET @customers_website_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'website'
);
SET @customers_website_sql := IF(@customers_website_exists = 0,
    'ALTER TABLE customers ADD COLUMN website VARCHAR(255)',
    'SELECT 1');
PREPARE stmt_customers_website FROM @customers_website_sql;
EXECUTE stmt_customers_website;
DEALLOCATE PREPARE stmt_customers_website;

SET @customers_address_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'address'
);
SET @customers_address_sql := IF(@customers_address_exists = 0,
    'ALTER TABLE customers ADD COLUMN address VARCHAR(500)',
    'SELECT 1');
PREPARE stmt_customers_address FROM @customers_address_sql;
EXECUTE stmt_customers_address;
DEALLOCATE PREPARE stmt_customers_address;

SET @customers_description_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'description'
);
SET @customers_description_sql := IF(@customers_description_exists = 0,
    'ALTER TABLE customers ADD COLUMN description TEXT',
    'SELECT 1');
PREPARE stmt_customers_description FROM @customers_description_sql;
EXECUTE stmt_customers_description;
DEALLOCATE PREPARE stmt_customers_description;

-- contacts
SET @contacts_mobile_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contacts' AND COLUMN_NAME = 'mobile'
);
SET @contacts_mobile_sql := IF(@contacts_mobile_exists = 0,
    'ALTER TABLE contacts ADD COLUMN mobile VARCHAR(40)',
    'SELECT 1');
PREPARE stmt_contacts_mobile FROM @contacts_mobile_sql;
EXECUTE stmt_contacts_mobile;
DEALLOCATE PREPARE stmt_contacts_mobile;

SET @contacts_position_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contacts' AND COLUMN_NAME = 'position'
);
SET @contacts_position_sql := IF(@contacts_position_exists = 0,
    'ALTER TABLE contacts ADD COLUMN position VARCHAR(120)',
    'SELECT 1');
PREPARE stmt_contacts_position FROM @contacts_position_sql;
EXECUTE stmt_contacts_position;
DEALLOCATE PREPARE stmt_contacts_position;

SET @contacts_company_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contacts' AND COLUMN_NAME = 'company'
);
SET @contacts_company_sql := IF(@contacts_company_exists = 0,
    'ALTER TABLE contacts ADD COLUMN company VARCHAR(255)',
    'SELECT 1');
PREPARE stmt_contacts_company FROM @contacts_company_sql;
EXECUTE stmt_contacts_company;
DEALLOCATE PREPARE stmt_contacts_company;

-- approval_events
SET @approval_events_actor_id_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'approval_events' AND COLUMN_NAME = 'actor_id'
);
SET @approval_events_actor_id_sql := IF(@approval_events_actor_id_exists = 0,
    'ALTER TABLE approval_events ADD COLUMN actor_id VARCHAR(64)',
    'SELECT 1');
PREPARE stmt_approval_events_actor_id FROM @approval_events_actor_id_sql;
EXECUTE stmt_approval_events_actor_id;
DEALLOCATE PREPARE stmt_approval_events_actor_id;

SET @approval_events_target_id_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'approval_events' AND COLUMN_NAME = 'target_id'
);
SET @approval_events_target_id_sql := IF(@approval_events_target_id_exists = 0,
    'ALTER TABLE approval_events ADD COLUMN target_id VARCHAR(64)',
    'SELECT 1');
PREPARE stmt_approval_events_target_id FROM @approval_events_target_id_sql;
EXECUTE stmt_approval_events_target_id;
DEALLOCATE PREPARE stmt_approval_events_target_id;

SET @approval_events_description_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'approval_events' AND COLUMN_NAME = 'description'
);
SET @approval_events_description_sql := IF(@approval_events_description_exists = 0,
    'ALTER TABLE approval_events ADD COLUMN description VARCHAR(500)',
    'SELECT 1');
PREPARE stmt_approval_events_description FROM @approval_events_description_sql;
EXECUTE stmt_approval_events_description;
DEALLOCATE PREPARE stmt_approval_events_description;

-- approval_tasks
SET @approval_tasks_template_id_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'approval_tasks' AND COLUMN_NAME = 'template_id'
);
SET @approval_tasks_template_id_sql := IF(@approval_tasks_template_id_exists = 0,
    'ALTER TABLE approval_tasks ADD COLUMN template_id VARCHAR(64)',
    'SELECT 1');
PREPARE stmt_approval_tasks_template_id FROM @approval_tasks_template_id_sql;
EXECUTE stmt_approval_tasks_template_id;
DEALLOCATE PREPARE stmt_approval_tasks_template_id;

SET @approval_tasks_sla_deadline_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'approval_tasks' AND COLUMN_NAME = 'sla_deadline'
);
SET @approval_tasks_sla_deadline_sql := IF(@approval_tasks_sla_deadline_exists = 0,
    'ALTER TABLE approval_tasks ADD COLUMN sla_deadline TIMESTAMP',
    'SELECT 1');
PREPARE stmt_approval_tasks_sla_deadline FROM @approval_tasks_sla_deadline_sql;
EXECUTE stmt_approval_tasks_sla_deadline;
DEALLOCATE PREPARE stmt_approval_tasks_sla_deadline;

SET @approval_tasks_priority_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'approval_tasks' AND COLUMN_NAME = 'priority'
);
SET @approval_tasks_priority_sql := IF(@approval_tasks_priority_exists = 0,
    'ALTER TABLE approval_tasks ADD COLUMN priority INT',
    'SELECT 1');
PREPARE stmt_approval_tasks_priority FROM @approval_tasks_priority_sql;
EXECUTE stmt_approval_tasks_priority;
DEALLOCATE PREPARE stmt_approval_tasks_priority;

SET @approval_tasks_parent_task_id_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'approval_tasks' AND COLUMN_NAME = 'parent_task_id'
);
SET @approval_tasks_parent_task_id_sql := IF(@approval_tasks_parent_task_id_exists = 0,
    'ALTER TABLE approval_tasks ADD COLUMN parent_task_id VARCHAR(64)',
    'SELECT 1');
PREPARE stmt_approval_tasks_parent_task_id FROM @approval_tasks_parent_task_id_sql;
EXECUTE stmt_approval_tasks_parent_task_id;
DEALLOCATE PREPARE stmt_approval_tasks_parent_task_id;

SET @approval_tasks_add_sign_type_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'approval_tasks' AND COLUMN_NAME = 'add_sign_type'
);
SET @approval_tasks_add_sign_type_sql := IF(@approval_tasks_add_sign_type_exists = 0,
    'ALTER TABLE approval_tasks ADD COLUMN add_sign_type VARCHAR(20)',
    'SELECT 1');
PREPARE stmt_approval_tasks_add_sign_type FROM @approval_tasks_add_sign_type_sql;
EXECUTE stmt_approval_tasks_add_sign_type;
DEALLOCATE PREPARE stmt_approval_tasks_add_sign_type;

SET @approval_tasks_assignee_id_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'approval_tasks' AND COLUMN_NAME = 'assignee_id'
);
SET @approval_tasks_assignee_id_sql := IF(@approval_tasks_assignee_id_exists = 0,
    'ALTER TABLE approval_tasks ADD COLUMN assignee_id VARCHAR(64)',
    'SELECT 1');
PREPARE stmt_approval_tasks_assignee_id FROM @approval_tasks_assignee_id_sql;
EXECUTE stmt_approval_tasks_assignee_id;
DEALLOCATE PREPARE stmt_approval_tasks_assignee_id;

SET @approval_tasks_delegated_from_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'approval_tasks' AND COLUMN_NAME = 'delegated_from'
);
SET @approval_tasks_delegated_from_sql := IF(@approval_tasks_delegated_from_exists = 0,
    'ALTER TABLE approval_tasks ADD COLUMN delegated_from VARCHAR(64)',
    'SELECT 1');
PREPARE stmt_approval_tasks_delegated_from FROM @approval_tasks_delegated_from_sql;
EXECUTE stmt_approval_tasks_delegated_from;
DEALLOCATE PREPARE stmt_approval_tasks_delegated_from;

SET @approval_tasks_transferred_at_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'approval_tasks' AND COLUMN_NAME = 'transferred_at'
);
SET @approval_tasks_transferred_at_sql := IF(@approval_tasks_transferred_at_exists = 0,
    'ALTER TABLE approval_tasks ADD COLUMN transferred_at TIMESTAMP',
    'SELECT 1');
PREPARE stmt_approval_tasks_transferred_at FROM @approval_tasks_transferred_at_sql;
EXECUTE stmt_approval_tasks_transferred_at;
DEALLOCATE PREPARE stmt_approval_tasks_transferred_at;

SET @approval_tasks_delegated_at_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'approval_tasks' AND COLUMN_NAME = 'delegated_at'
);
SET @approval_tasks_delegated_at_sql := IF(@approval_tasks_delegated_at_exists = 0,
    'ALTER TABLE approval_tasks ADD COLUMN delegated_at TIMESTAMP',
    'SELECT 1');
PREPARE stmt_approval_tasks_delegated_at FROM @approval_tasks_delegated_at_sql;
EXECUTE stmt_approval_tasks_delegated_at;
DEALLOCATE PREPARE stmt_approval_tasks_delegated_at;

SET @approval_tasks_transfer_history_exists := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'approval_tasks' AND COLUMN_NAME = 'transfer_history'
);
SET @approval_tasks_transfer_history_sql := IF(@approval_tasks_transfer_history_exists = 0,
    'ALTER TABLE approval_tasks ADD COLUMN transfer_history TEXT',
    'SELECT 1');
PREPARE stmt_approval_tasks_transfer_history FROM @approval_tasks_transfer_history_sql;
EXECUTE stmt_approval_tasks_transfer_history;
DEALLOCATE PREPARE stmt_approval_tasks_transfer_history;
