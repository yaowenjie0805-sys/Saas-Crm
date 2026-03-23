-- ============================================================
-- V16: 补充缺失字段和表结构优化
-- 增加评论增强字段、团队成员字段、工作流执行租户字段
-- ============================================================

-- 1. teams 表补充成员相关字段
SET @teams_member_ids_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'teams'
      AND COLUMN_NAME = 'member_ids'
);
SET @teams_member_ids_sql := IF(@teams_member_ids_exists = 0,
    'ALTER TABLE teams ADD COLUMN member_ids TEXT COMMENT ''成员ID列表，逗号分隔''',
    'SELECT 1');
PREPARE stmt_teams_member_ids FROM @teams_member_ids_sql;
EXECUTE stmt_teams_member_ids;
DEALLOCATE PREPARE stmt_teams_member_ids;

SET @teams_member_count_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'teams'
      AND COLUMN_NAME = 'member_count'
);
SET @teams_member_count_sql := IF(@teams_member_count_exists = 0,
    'ALTER TABLE teams ADD COLUMN member_count INT DEFAULT 0 COMMENT ''成员数量''',
    'SELECT 1');
PREPARE stmt_teams_member_count FROM @teams_member_count_sql;
EXECUTE stmt_teams_member_count;
DEALLOCATE PREPARE stmt_teams_member_count;

-- 2. comments 表补充增强字段
SET @comments_like_count_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comments'
      AND COLUMN_NAME = 'like_count'
);
SET @comments_like_count_sql := IF(@comments_like_count_exists = 0,
    'ALTER TABLE comments ADD COLUMN like_count INT DEFAULT 0 COMMENT ''点赞数''',
    'SELECT 1');
PREPARE stmt_comments_like_count FROM @comments_like_count_sql;
EXECUTE stmt_comments_like_count;
DEALLOCATE PREPARE stmt_comments_like_count;

SET @comments_reply_count_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comments'
      AND COLUMN_NAME = 'reply_count'
);
SET @comments_reply_count_sql := IF(@comments_reply_count_exists = 0,
    'ALTER TABLE comments ADD COLUMN reply_count INT DEFAULT 0 COMMENT ''回复数''',
    'SELECT 1');
PREPARE stmt_comments_reply_count FROM @comments_reply_count_sql;
EXECUTE stmt_comments_reply_count;
DEALLOCATE PREPARE stmt_comments_reply_count;

SET @comments_tags_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comments'
      AND COLUMN_NAME = 'tags'
);
SET @comments_tags_sql := IF(@comments_tags_exists = 0,
    'ALTER TABLE comments ADD COLUMN tags TEXT COMMENT ''标签列表，JSON数组''',
    'SELECT 1');
PREPARE stmt_comments_tags FROM @comments_tags_sql;
EXECUTE stmt_comments_tags;
DEALLOCATE PREPARE stmt_comments_tags;

SET @comments_liked_users_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comments'
      AND COLUMN_NAME = 'liked_users'
);
SET @comments_liked_users_sql := IF(@comments_liked_users_exists = 0,
    'ALTER TABLE comments ADD COLUMN liked_users TEXT COMMENT ''点赞用户ID列表''',
    'SELECT 1');
PREPARE stmt_comments_liked_users FROM @comments_liked_users_sql;
EXECUTE stmt_comments_liked_users;
DEALLOCATE PREPARE stmt_comments_liked_users;

SET @comments_metadata_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comments'
      AND COLUMN_NAME = 'metadata'
);
SET @comments_metadata_sql := IF(@comments_metadata_exists = 0,
    'ALTER TABLE comments ADD COLUMN metadata TEXT COMMENT ''元数据，JSON格式''',
    'SELECT 1');
PREPARE stmt_comments_metadata FROM @comments_metadata_sql;
EXECUTE stmt_comments_metadata;
DEALLOCATE PREPARE stmt_comments_metadata;

SET @comments_edited_at_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comments'
      AND COLUMN_NAME = 'edited_at'
);
SET @comments_edited_at_sql := IF(@comments_edited_at_exists = 0,
    'ALTER TABLE comments ADD COLUMN edited_at TIMESTAMP COMMENT ''编辑时间''',
    'SELECT 1');
PREPARE stmt_comments_edited_at FROM @comments_edited_at_sql;
EXECUTE stmt_comments_edited_at;
DEALLOCATE PREPARE stmt_comments_edited_at;

SET @comments_parent_comment_id_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comments'
      AND COLUMN_NAME = 'parent_comment_id'
);
SET @comments_parent_comment_id_sql := IF(@comments_parent_comment_id_exists = 0,
    'ALTER TABLE comments ADD COLUMN parent_comment_id VARCHAR(64) COMMENT ''父评论ID''',
    'SELECT 1');
PREPARE stmt_comments_parent_comment_id FROM @comments_parent_comment_id_sql;
EXECUTE stmt_comments_parent_comment_id;
DEALLOCATE PREPARE stmt_comments_parent_comment_id;

-- 删除旧的 likes_count 列（如果存在）
SET @comments_likes_count_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comments'
      AND COLUMN_NAME = 'likes_count'
);
SET @comments_likes_count_sql := IF(@comments_likes_count_exists = 1,
    'ALTER TABLE comments DROP COLUMN likes_count',
    'SELECT 1');
PREPARE stmt_comments_likes_count FROM @comments_likes_count_sql;
EXECUTE stmt_comments_likes_count;
DEALLOCATE PREPARE stmt_comments_likes_count;

-- 3. workflow_executions 补充租户字段
SET @workflow_exec_tenant_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'workflow_executions'
      AND COLUMN_NAME = 'tenant_id'
);
SET @workflow_exec_tenant_sql := IF(@workflow_exec_tenant_exists = 0,
    'ALTER TABLE workflow_executions ADD COLUMN tenant_id VARCHAR(64) NOT NULL COMMENT ''租户ID''',
    'SELECT 1');
PREPARE stmt_workflow_exec_tenant FROM @workflow_exec_tenant_sql;
EXECUTE stmt_workflow_exec_tenant;
DEALLOCATE PREPARE stmt_workflow_exec_tenant;

SET @workflow_exec_tenant_idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'workflow_executions'
      AND INDEX_NAME = 'idx_workflow_executions_tenant'
);
SET @workflow_exec_tenant_idx_sql := IF(@workflow_exec_tenant_idx_exists = 0,
    'CREATE INDEX idx_workflow_executions_tenant ON workflow_executions(tenant_id)',
    'SELECT 1');
PREPARE stmt_workflow_exec_tenant_idx FROM @workflow_exec_tenant_idx_sql;
EXECUTE stmt_workflow_exec_tenant_idx;
DEALLOCATE PREPARE stmt_workflow_exec_tenant_idx;

-- 4. approval_nodes 补充审批人字段
SET @approval_nodes_approver_ids_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'approval_nodes'
      AND COLUMN_NAME = 'approver_ids'
);
SET @approval_nodes_approver_ids_sql := IF(@approval_nodes_approver_ids_exists = 0,
    'ALTER TABLE approval_nodes ADD COLUMN approver_ids VARCHAR(512) COMMENT ''审批人ID列表，逗号分隔''',
    'SELECT 1');
PREPARE stmt_approval_nodes_approver_ids FROM @approval_nodes_approver_ids_sql;
EXECUTE stmt_approval_nodes_approver_ids;
DEALLOCATE PREPARE stmt_approval_nodes_approver_ids;

-- 5. approval_tasks 补充委托和转交字段
SET @approval_tasks_delegated_from_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'approval_tasks'
      AND COLUMN_NAME = 'delegated_from'
);
SET @approval_tasks_delegated_from_sql := IF(@approval_tasks_delegated_from_exists = 0,
    'ALTER TABLE approval_tasks ADD COLUMN delegated_from VARCHAR(64) COMMENT ''委托来源用户ID''',
    'SELECT 1');
PREPARE stmt_approval_tasks_delegated_from FROM @approval_tasks_delegated_from_sql;
EXECUTE stmt_approval_tasks_delegated_from;
DEALLOCATE PREPARE stmt_approval_tasks_delegated_from;

SET @approval_tasks_delegated_at_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'approval_tasks'
      AND COLUMN_NAME = 'delegated_at'
);
SET @approval_tasks_delegated_at_sql := IF(@approval_tasks_delegated_at_exists = 0,
    'ALTER TABLE approval_tasks ADD COLUMN delegated_at TIMESTAMP COMMENT ''委托时间''',
    'SELECT 1');
PREPARE stmt_approval_tasks_delegated_at FROM @approval_tasks_delegated_at_sql;
EXECUTE stmt_approval_tasks_delegated_at;
DEALLOCATE PREPARE stmt_approval_tasks_delegated_at;

SET @approval_tasks_transfer_history_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'approval_tasks'
      AND COLUMN_NAME = 'transfer_history'
);
SET @approval_tasks_transfer_history_sql := IF(@approval_tasks_transfer_history_exists = 0,
    'ALTER TABLE approval_tasks ADD COLUMN transfer_history TEXT COMMENT ''转交历史''',
    'SELECT 1');
PREPARE stmt_approval_tasks_transfer_history FROM @approval_tasks_transfer_history_sql;
EXECUTE stmt_approval_tasks_transfer_history;
DEALLOCATE PREPARE stmt_approval_tasks_transfer_history;

SET @approval_tasks_transferred_at_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'approval_tasks'
      AND COLUMN_NAME = 'transferred_at'
);
SET @approval_tasks_transferred_at_sql := IF(@approval_tasks_transferred_at_exists = 0,
    'ALTER TABLE approval_tasks ADD COLUMN transferred_at TIMESTAMP COMMENT ''最后转交时间''',
    'SELECT 1');
PREPARE stmt_approval_tasks_transferred_at FROM @approval_tasks_transferred_at_sql;
EXECUTE stmt_approval_tasks_transferred_at;
DEALLOCATE PREPARE stmt_approval_tasks_transferred_at;

SET @approval_tasks_add_sign_type_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'approval_tasks'
      AND COLUMN_NAME = 'add_sign_type'
);
SET @approval_tasks_add_sign_type_sql := IF(@approval_tasks_add_sign_type_exists = 0,
    'ALTER TABLE approval_tasks ADD COLUMN add_sign_type VARCHAR(20) COMMENT ''加签类型: BEFORE, AFTER''',
    'SELECT 1');
PREPARE stmt_approval_tasks_add_sign_type FROM @approval_tasks_add_sign_type_sql;
EXECUTE stmt_approval_tasks_add_sign_type;
DEALLOCATE PREPARE stmt_approval_tasks_add_sign_type;

SET @approval_tasks_parent_task_id_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'approval_tasks'
      AND COLUMN_NAME = 'parent_task_id'
);
SET @approval_tasks_parent_task_id_sql := IF(@approval_tasks_parent_task_id_exists = 0,
    'ALTER TABLE approval_tasks ADD COLUMN parent_task_id VARCHAR(64) COMMENT ''父任务ID（加签时）''',
    'SELECT 1');
PREPARE stmt_approval_tasks_parent_task_id FROM @approval_tasks_parent_task_id_sql;
EXECUTE stmt_approval_tasks_parent_task_id;
DEALLOCATE PREPARE stmt_approval_tasks_parent_task_id;

-- 6. 创建审批事件表（如果不存在）
CREATE TABLE IF NOT EXISTS approval_events (
    id VARCHAR(64) PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL,
    instance_id VARCHAR(64),
    event_type VARCHAR(30) NOT NULL COMMENT 'CREATED, APPROVED, REJECTED, DELEGATED, ADD_SIGN, TRANSFERRED',
    actor_id VARCHAR(80) NOT NULL,
    target_id VARCHAR(80) COMMENT '目标用户ID',
    description TEXT COMMENT '事件描述',
    created_at TIMESTAMP NOT NULL,
    INDEX idx_task (task_id),
    INDEX idx_instance (instance_id),
    INDEX idx_actor (actor_id),
    INDEX idx_event_type (event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. 拼音字段补充
SET @contacts_name_pinyin_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'contacts'
      AND COLUMN_NAME = 'name_pinyin'
);
SET @contacts_name_pinyin_sql := IF(@contacts_name_pinyin_exists = 0,
    'ALTER TABLE contacts ADD COLUMN name_pinyin VARCHAR(200) COMMENT ''姓名拼音''',
    'SELECT 1');
PREPARE stmt_contacts_name_pinyin FROM @contacts_name_pinyin_sql;
EXECUTE stmt_contacts_name_pinyin;
DEALLOCATE PREPARE stmt_contacts_name_pinyin;

SET @customers_name_pinyin_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'customers'
      AND COLUMN_NAME = 'name_pinyin'
);
SET @customers_name_pinyin_sql := IF(@customers_name_pinyin_exists = 0,
    'ALTER TABLE customers ADD COLUMN name_pinyin VARCHAR(200) COMMENT ''公司名称拼音''',
    'SELECT 1');
PREPARE stmt_customers_name_pinyin FROM @customers_name_pinyin_sql;
EXECUTE stmt_customers_name_pinyin;
DEALLOCATE PREPARE stmt_customers_name_pinyin;

SET @leads_name_pinyin_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'leads'
      AND COLUMN_NAME = 'name_pinyin'
);
SET @leads_name_pinyin_sql := IF(@leads_name_pinyin_exists = 0,
    'ALTER TABLE leads ADD COLUMN name_pinyin VARCHAR(200) COMMENT ''姓名拼音''',
    'SELECT 1');
PREPARE stmt_leads_name_pinyin FROM @leads_name_pinyin_sql;
EXECUTE stmt_leads_name_pinyin;
DEALLOCATE PREPARE stmt_leads_name_pinyin;

SET @leads_company_pinyin_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'leads'
      AND COLUMN_NAME = 'company_pinyin'
);
SET @leads_company_pinyin_sql := IF(@leads_company_pinyin_exists = 0,
    'ALTER TABLE leads ADD COLUMN company_pinyin VARCHAR(200) COMMENT ''公司名称拼音''',
    'SELECT 1');
PREPARE stmt_leads_company_pinyin FROM @leads_company_pinyin_sql;
EXECUTE stmt_leads_company_pinyin;
DEALLOCATE PREPARE stmt_leads_company_pinyin;

-- 8. 创建操作日志归档表（用于历史数据查询）
CREATE TABLE IF NOT EXISTS audit_logs_archive (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action_id BIGINT,
    tenant_id VARCHAR(64),
    actor_id VARCHAR(80),
    actor_type VARCHAR(40),
    action_type VARCHAR(40),
    entity_type VARCHAR(40),
    entity_id VARCHAR(64),
    details TEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    created_at TIMESTAMP,
    archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant_actor (tenant_id, actor_id),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_created (created_at),
    INDEX idx_archived (archived_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
