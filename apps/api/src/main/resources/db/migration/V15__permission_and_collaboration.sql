-- ============================================================
-- V15: 鏉冮檺鎺у埗澧炲己鍜屽崗浣滃姛鑳?
-- 鏀寔瀛楁绾ф潈闄愩€佹暟鎹寖鍥磋鍒欍€佸洟闃熺鐞嗐€佹晱鎰熷瓧娈佃劚鏁?
-- ============================================================

-- 瀛楁鏉冮檺琛?
CREATE TABLE IF NOT EXISTS field_permissions (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    entity_type VARCHAR(40) NOT NULL COMMENT 'CUSTOMER, LEAD, OPPORTUNITY, etc.',
    field_name VARCHAR(80) NOT NULL,
    role VARCHAR(30) NOT NULL,
    can_view BOOLEAN NOT NULL DEFAULT TRUE,
    can_edit BOOLEAN NOT NULL DEFAULT FALSE,
    can_delete BOOLEAN NOT NULL DEFAULT FALSE,
    is_hidden BOOLEAN NOT NULL DEFAULT FALSE COMMENT '瀹屽叏闅愯棌瀛楁',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_tenant_entity_field_role (tenant_id, entity_type, field_name, role),
    INDEX idx_tenant_role (tenant_id, role),
    INDEX idx_entity (entity_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 鏁版嵁鑼冨洿瑙勫垯琛?
CREATE TABLE IF NOT EXISTS data_scope_rules (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    role VARCHAR(30) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    scope_type VARCHAR(20) NOT NULL COMMENT 'OWNER, DEPARTMENT, TEAM, CUSTOM',
    scope_config TEXT COMMENT '自定义范围配置',
    filter_expression TEXT COMMENT '过滤表达式',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_tenant_role_entity (tenant_id, role, entity_type),
    INDEX idx_tenant_role (tenant_id, role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 鍥㈤槦琛?
CREATE TABLE IF NOT EXISTS teams (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    leader_id VARCHAR(64),
    parent_team_id VARCHAR(64),
    team_type VARCHAR(40) COMMENT 'SALES, MARKETING, SUPPORT',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_tenant (tenant_id),
    INDEX idx_parent (parent_team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 鍥㈤槦鎴愬憳琛?
CREATE TABLE IF NOT EXISTS team_members (
    id VARCHAR(64) PRIMARY KEY,
    team_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    role VARCHAR(40) NOT NULL COMMENT 'LEADER, MEMBER',
    joined_at TIMESTAMP NOT NULL,
    INDEX idx_team (team_id),
    INDEX idx_user (user_id),
    UNIQUE KEY uk_team_user (team_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 璇勮琛?
CREATE TABLE IF NOT EXISTS comments (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    entity_type VARCHAR(40) NOT NULL COMMENT 'CUSTOMER, LEAD, OPPORTUNITY, etc.',
    entity_id VARCHAR(64) NOT NULL,
    author VARCHAR(80) NOT NULL,
    author_name VARCHAR(120),
    content TEXT NOT NULL,
    parent_id VARCHAR(64) COMMENT '鍥炲鏌愭潯璇勮',
    mentions TEXT COMMENT 'JSON鏁扮粍: ["user1", "user2"]',
    mentions_users TEXT COMMENT '提及用户信息',
    likes_count INT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_tenant (tenant_id),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_author (author),
    INDEX idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 娲诲姩鍒嗕韩琛?
CREATE TABLE IF NOT EXISTS activity_shares (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    share_type VARCHAR(20) NOT NULL COMMENT 'USER, TEAM, DEPARTMENT, ROLE',
    share_target VARCHAR(80) NOT NULL COMMENT 'user_id, team_id, department, role_name',
    activity_type VARCHAR(40) NOT NULL COMMENT 'CREATE, UPDATE, DELETE, STATUS_CHANGE',
    entity_type VARCHAR(40) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    shared_by VARCHAR(80) NOT NULL,
    message TEXT COMMENT '闄勮█',
    created_at TIMESTAMP NOT NULL,
    INDEX idx_tenant (tenant_id),
    INDEX idx_share_target (share_type, share_target),
    INDEX idx_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 鏁忔劅瀛楁閰嶇疆琛紙鍥藉唴鐗硅壊锛?
CREATE TABLE IF NOT EXISTS sensitive_field_configs (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    field_name VARCHAR(80) NOT NULL,
    mask_type VARCHAR(20) NOT NULL DEFAULT 'PARTIAL' COMMENT 'PARTIAL, FULL, CUSTOM',
    mask_pattern VARCHAR(100) COMMENT '自定义脱敏模式',
    visible_chars_start INT DEFAULT 3,
    visible_chars_end INT DEFAULT 4,
    mask_char VARCHAR(10) DEFAULT '*',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_tenant_entity_field (tenant_id, entity_type, field_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 娑堟伅鎺ㄩ€佽褰曡〃锛堝浗鍐呯壒鑹诧級
CREATE TABLE IF NOT EXISTS push_messages (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(80) NOT NULL,
    channel VARCHAR(20) NOT NULL COMMENT 'WECHAT_WORK, DINGTALK, SMS, EMAIL, IN_APP',
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    content_type VARCHAR(20) DEFAULT 'TEXT' COMMENT 'TEXT, HTML, TEMPLATE',
    related_type VARCHAR(40) COMMENT '鍏宠仈绫诲瀷',
    related_id VARCHAR(64) COMMENT '鍏宠仈ID',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, SENT, FAILED, READ',
    sent_at TIMESTAMP,
    read_at TIMESTAMP,
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    INDEX idx_user_channel (user_id, channel),
    INDEX idx_status (status),
    INDEX idx_related (related_type, related_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 閫氱煡娓犻亾閰嶇疆琛紙鍥藉唴鐗硅壊锛?
CREATE TABLE IF NOT EXISTS notification_channels (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    channel_type VARCHAR(20) NOT NULL COMMENT 'WECHAT_WORK, DINGTALK, SMS, EMAIL',
    channel_name VARCHAR(120) NOT NULL,
    config_json TEXT NOT NULL COMMENT '娓犻亾閰嶇疆',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    priority INT NOT NULL DEFAULT 0 COMMENT '优先级',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_tenant (tenant_id),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 鐢ㄦ埛鎵╁睍瀛楁锛堝叧鑱斿洟闃熴€佷富绠★級
SET @team_id_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_accounts'
      AND COLUMN_NAME = 'team_id'
);
SET @team_id_sql := IF(@team_id_exists = 0,
    'ALTER TABLE user_accounts ADD COLUMN team_id VARCHAR(64)',
    'SELECT 1');
PREPARE stmt_team_id FROM @team_id_sql;
EXECUTE stmt_team_id;
DEALLOCATE PREPARE stmt_team_id;

SET @manager_id_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_accounts'
      AND COLUMN_NAME = 'manager_id'
);
SET @manager_id_sql := IF(@manager_id_exists = 0,
    'ALTER TABLE user_accounts ADD COLUMN manager_id VARCHAR(64)',
    'SELECT 1');
PREPARE stmt_manager_id FROM @manager_id_sql;
EXECUTE stmt_manager_id;
DEALLOCATE PREPARE stmt_manager_id;

SET @department_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_accounts'
      AND COLUMN_NAME = 'department'
);
SET @department_sql := IF(@department_exists = 0,
    'ALTER TABLE user_accounts ADD COLUMN department VARCHAR(120)',
    'SELECT 1');
PREPARE stmt_department FROM @department_sql;
EXECUTE stmt_department;
DEALLOCATE PREPARE stmt_department;
