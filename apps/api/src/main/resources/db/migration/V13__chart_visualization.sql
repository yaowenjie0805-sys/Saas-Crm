-- ============================================================
-- V13: 鍥捐〃鍙鍖栧姛鑳?
-- 鏀寔鏌辩姸鍥俱€佹姌绾垮浘銆侀ゼ鍥俱€佹紡鏂楀浘绛夊浘琛ㄧ被鍨?
-- ============================================================

-- 鍥捐〃妯℃澘琛?
CREATE TABLE IF NOT EXISTS chart_templates (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    chart_type VARCHAR(40) NOT NULL COMMENT 'BAR, LINE, PIE, FUNNEL, RADAR, GAUGE',
    dataset_type VARCHAR(40) NOT NULL COMMENT 'CUSTOMERS, OPPORTUNITIES, QUOTES, ORDERS, REVENUE, etc.',
    config_json TEXT NOT NULL COMMENT '鍥捐〃閰嶇疆锛氶鑹层€佹爣绛俱€佸浘渚嬬瓑',
    layout_config TEXT COMMENT '甯冨眬閰嶇疆锛氬楂樸€佷綅缃瓑',
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE' COMMENT 'PRIVATE, DEPARTMENT, TENANT',
    owner VARCHAR(80) NOT NULL,
    department VARCHAR(80),
    version INT NOT NULL DEFAULT 1,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_tenant_type (tenant_id, chart_type),
    INDEX idx_tenant_dataset (tenant_id, dataset_type),
    INDEX idx_owner (owner)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 鍥捐〃鏁版嵁闆嗚〃锛堢紦瀛樿绠楃粨鏋滐級
CREATE TABLE IF NOT EXISTS chart_datasets (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    chart_id VARCHAR(64) NOT NULL,
    dataset_type VARCHAR(40) NOT NULL,
    filter_config TEXT COMMENT '筛选条件配置',
    data_json TEXT NOT NULL COMMENT '鏁版嵁闆咼SON',
    computed_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    INDEX idx_chart (chart_id),
    INDEX idx_tenant_computed (tenant_id, computed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 浠〃鐩橀厤缃〃
CREATE TABLE IF NOT EXISTS dashboard_configs (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    layout_json TEXT NOT NULL COMMENT '浠〃鐩樺竷灞€閰嶇疆',
    widgets_json TEXT NOT NULL COMMENT '仪表盘组件配置',
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    owner VARCHAR(80) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_tenant_owner (tenant_id, owner),
    INDEX idx_is_default (is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 閿€鍞娴嬭〃
CREATE TABLE IF NOT EXISTS sales_forecasts (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    owner VARCHAR(80),
    forecast_period VARCHAR(20) NOT NULL COMMENT 'MONTH, QUARTER, YEAR',
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    predicted_amount BIGINT NOT NULL DEFAULT 0,
    predicted_count INT NOT NULL DEFAULT 0,
    confirmed_amount BIGINT NOT NULL DEFAULT 0,
    confidence_level DECIMAL(5,2) COMMENT '0.00-100.00',
    pipeline_amount BIGINT NOT NULL DEFAULT 0,
    model_version VARCHAR(40),
    computed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_tenant_period (tenant_id, forecast_period, period_start),
    INDEX idx_owner_period (owner, forecast_period, period_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 棰勬祴璋冩暣璁板綍琛?
CREATE TABLE IF NOT EXISTS forecast_adjustments (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    forecast_id VARCHAR(64) NOT NULL,
    adjusted_by VARCHAR(80) NOT NULL,
    adjustment_type VARCHAR(20) NOT NULL COMMENT 'UPWARD, DOWNWARD, COMMIT',
    adjustment_amount BIGINT NOT NULL DEFAULT 0,
    previous_amount BIGINT NOT NULL DEFAULT 0,
    new_amount BIGINT NOT NULL DEFAULT 0,
    adjustment_reason TEXT,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (forecast_id) REFERENCES sales_forecasts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
