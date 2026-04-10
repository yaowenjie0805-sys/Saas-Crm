-- ============================================================
-- V14: 鍙鍖栧伐浣滄祦寮曟搸
-- 鏀寔鎷栨嫿寮忓伐浣滄祦璁捐銆佸鏉′欢鍒嗘敮銆佸浗鍐呭鎵圭壒鑹?
-- ============================================================

-- 宸ヤ綔娴佸畾涔夎〃
CREATE TABLE IF NOT EXISTS workflow_definitions (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    category VARCHAR(40) COMMENT 'MARKETING, SALES, APPROVAL, CUSTOM',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT, ACTIVE, PAUSED, ARCHIVED',
    version INT NOT NULL DEFAULT 1,
    owner VARCHAR(80) NOT NULL,
    department VARCHAR(80),
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    execution_count INT NOT NULL DEFAULT 0,
    last_executed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    activated_at TIMESTAMP,
    published_by VARCHAR(80),
    INDEX idx_tenant_status (tenant_id, status),
    INDEX idx_category (category),
    INDEX idx_owner (owner)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 宸ヤ綔娴佽妭鐐硅〃
CREATE TABLE IF NOT EXISTS workflow_nodes (
    id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    node_type VARCHAR(40) NOT NULL COMMENT 'TRIGGER, CONDITION, ACTION, NOTIFICATION, WAIT, APPROVAL, CC',
    node_subtype VARCHAR(80) COMMENT 'CREATE_TASK, SEND_EMAIL, UPDATE_FIELD, etc.',
    name VARCHAR(120) NOT NULL,
    description TEXT,
    position_x INT NOT NULL,
    position_y INT NOT NULL,
    config_json TEXT NOT NULL COMMENT '鑺傜偣閰嶇疆',
    input_mapping TEXT COMMENT '杈撳叆鏄犲皠',
    output_mapping TEXT COMMENT '杈撳嚭鏄犲皠',
    config_validation VARCHAR(20) NOT NULL DEFAULT 'VALID' COMMENT 'VALID, INVALID',
    validation_message TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_workflow (workflow_id),
    INDEX idx_node_type (node_type),
    FOREIGN KEY (workflow_id) REFERENCES workflow_definitions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 宸ヤ綔娴佽繛鎺ヨ〃
CREATE TABLE IF NOT EXISTS workflow_connections (
    id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    source_node_id VARCHAR(64) NOT NULL,
    target_node_id VARCHAR(64) NOT NULL,
    connection_type VARCHAR(20) NOT NULL DEFAULT 'DEFAULT' COMMENT 'DEFAULT, TRUE, FALSE',
    label VARCHAR(80),
    condition_expression TEXT COMMENT '条件表达式',
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    INDEX idx_workflow (workflow_id),
    INDEX idx_source (source_node_id),
    INDEX idx_target (target_node_id),
    FOREIGN KEY (workflow_id) REFERENCES workflow_definitions(id) ON DELETE CASCADE,
    FOREIGN KEY (source_node_id) REFERENCES workflow_nodes(id) ON DELETE CASCADE,
    FOREIGN KEY (target_node_id) REFERENCES workflow_nodes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 宸ヤ綔娴佹墽琛岃褰曡〃
CREATE TABLE IF NOT EXISTS workflow_executions (
    id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    workflow_version INT NOT NULL,
    trigger_type VARCHAR(40) NOT NULL,
    trigger_source VARCHAR(80) COMMENT 'entity ID that triggered',
    trigger_payload TEXT COMMENT 'JSON payload',
    status VARCHAR(20) NOT NULL COMMENT 'RUNNING, COMPLETED, FAILED, CANCELLED',
    current_node_id VARCHAR(64),
    execution_context TEXT COMMENT 'JSON context',
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    error_message TEXT,
    error_details TEXT,
    execution_duration_ms INT COMMENT '鎵ц鑰楁椂(姣)',
    INDEX idx_workflow_status (workflow_id, status),
    INDEX idx_trigger (trigger_type),
    INDEX idx_started (started_at),
    FOREIGN KEY (workflow_id) REFERENCES workflow_definitions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 宸ヤ綔娴佽妭鐐规墽琛岃褰曡〃
CREATE TABLE IF NOT EXISTS workflow_node_executions (
    id VARCHAR(64) PRIMARY KEY,
    execution_id VARCHAR(64) NOT NULL,
    node_id VARCHAR(64) NOT NULL,
    node_type VARCHAR(40) NOT NULL,
    node_name VARCHAR(120),
    status VARCHAR(20) NOT NULL COMMENT 'PENDING, RUNNING, COMPLETED, FAILED, SKIPPED',
    input_data TEXT,
    output_data TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    execution_order INT NOT NULL DEFAULT 0,
    INDEX idx_execution (execution_id),
    INDEX idx_node (node_id),
    FOREIGN KEY (execution_id) REFERENCES workflow_executions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 瀹℃壒鑺傜偣閰嶇疆琛紙鍥藉唴鐗硅壊锛?
CREATE TABLE IF NOT EXISTS approval_nodes (
    id VARCHAR(64) PRIMARY KEY,
    workflow_node_id VARCHAR(64) NOT NULL,
    approval_type VARCHAR(20) NOT NULL COMMENT 'SINGLE, SERIAL, PARALLEL, OR',
    approver_type VARCHAR(20) NOT NULL COMMENT 'USER, ROLE, DEPARTMENT, DYNAMIC',
    approver_config TEXT NOT NULL COMMENT '审批人配置',
    sla_hours INT COMMENT 'SLA鏃堕檺(灏忔椂)',
    allow_add_sign BOOLEAN NOT NULL DEFAULT TRUE COMMENT '鍏佽鍔犵',
    allow_transfer BOOLEAN NOT NULL DEFAULT TRUE COMMENT '鍏佽杞氦',
    allow_reject BOOLEAN NOT NULL DEFAULT TRUE COMMENT '鍏佽椹冲洖',
    reject_to VARCHAR(20) COMMENT 'REJECT_START, REJECT_PREV, SPECIFIC_NODE',
    reject_node_id VARCHAR(64),
    notify_on_create BOOLEAN NOT NULL DEFAULT TRUE,
    notify_on_complete BOOLEAN NOT NULL DEFAULT TRUE,
    notify_on_timeout BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_workflow_node (workflow_node_id),
    FOREIGN KEY (workflow_node_id) REFERENCES workflow_nodes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 瀹℃壒鍔犵璁板綍琛紙鍥藉唴鐗硅壊锛?
CREATE TABLE IF NOT EXISTS approval_delegations (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    workflow_execution_id VARCHAR(64) NOT NULL,
    workflow_node_id VARCHAR(64) NOT NULL,
    delegation_type VARCHAR(20) NOT NULL COMMENT 'ADD_SIGN, TRANSFER',
    from_user VARCHAR(80) NOT NULL,
    to_user VARCHAR(80) NOT NULL,
    reason TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, ACCEPTED, REJECTED, CANCELLED',
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    INDEX idx_execution (workflow_execution_id),
    INDEX idx_from_user (from_user),
    INDEX idx_to_user (to_user),
    FOREIGN KEY (workflow_execution_id) REFERENCES workflow_executions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 閫氱煡鑺傜偣閰嶇疆琛?
CREATE TABLE IF NOT EXISTS notification_nodes (
    id VARCHAR(64) PRIMARY KEY,
    workflow_node_id VARCHAR(64) NOT NULL,
    channel VARCHAR(20) NOT NULL COMMENT 'EMAIL, SMS, WECHAT_WORK, DINGTALK, IN_APP',
    template_type VARCHAR(40) NOT NULL COMMENT 'ALERT, REMINDER, NOTIFICATION',
    recipient_type VARCHAR(20) NOT NULL COMMENT 'FIXED, DYNAMIC, FIELD',
    recipient_config TEXT NOT NULL COMMENT '接收人配置',
    template_config TEXT NOT NULL COMMENT '娑堟伅妯℃澘閰嶇疆',
    priority VARCHAR(10) NOT NULL DEFAULT 'NORMAL' COMMENT 'LOW, NORMAL, HIGH, URGENT',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_workflow_node (workflow_node_id),
    FOREIGN KEY (workflow_node_id) REFERENCES workflow_nodes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 绛夊緟鑺傜偣閰嶇疆琛?
CREATE TABLE IF NOT EXISTS wait_nodes (
    id VARCHAR(64) PRIMARY KEY,
    workflow_node_id VARCHAR(64) NOT NULL,
    wait_type VARCHAR(20) NOT NULL COMMENT 'DELAY, CONDITION, WEBHOOK',
    duration_value INT COMMENT '等待时长值',
    duration_unit VARCHAR(10) COMMENT 'MINUTES, HOURS, DAYS',
    condition_expression TEXT COMMENT '绛夊緟鏉′欢',
    timeout_action VARCHAR(20) COMMENT 'CONTINUE, TERMINATE, NOTIFY',
    timeout_node_id VARCHAR(64),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_workflow_node (workflow_node_id),
    FOREIGN KEY (workflow_node_id) REFERENCES workflow_nodes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 宸ヤ綔娴佽Е鍙戝櫒绫诲瀷琛?
CREATE TABLE IF NOT EXISTS workflow_triggers (
    id VARCHAR(64) PRIMARY KEY,
    node_id VARCHAR(64) NOT NULL,
    trigger_type VARCHAR(40) NOT NULL COMMENT 'RECORD_CREATED, RECORD_UPDATED, FIELD_CHANGED, SCHEDULE, MANUAL',
    entity_type VARCHAR(40) COMMENT 'CUSTOMER, LEAD, OPPORTUNITY, etc.',
    field_name VARCHAR(80) COMMENT '瑙﹀彂瀛楁',
    condition_expression TEXT COMMENT '瑙﹀彂鏉′欢',
    config_json TEXT COMMENT '瑙﹀彂閰嶇疆',
    created_at TIMESTAMP NOT NULL,
    INDEX idx_node (node_id),
    FOREIGN KEY (node_id) REFERENCES workflow_nodes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
