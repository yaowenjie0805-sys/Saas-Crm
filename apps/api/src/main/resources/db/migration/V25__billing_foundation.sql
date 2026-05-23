CREATE TABLE IF NOT EXISTS subscription_plans (
    id VARCHAR(64) PRIMARY KEY,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    price_cents_monthly BIGINT NOT NULL DEFAULT 0,
    currency VARCHAR(16) NOT NULL DEFAULT 'CNY',
    max_users INT NOT NULL DEFAULT 1,
    max_customers INT NOT NULL DEFAULT 100,
    max_storage_mb INT NOT NULL DEFAULT 512,
    features_json TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_subscription_plans_code (code),
    INDEX idx_subscription_plans_enabled (enabled, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tenant_subscriptions (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    plan_code VARCHAR(32) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    started_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NULL,
    trial_ends_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_tenant_subscriptions_tenant (tenant_id),
    INDEX idx_tenant_subscriptions_plan (plan_code),
    INDEX idx_tenant_subscriptions_status (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tenant_usage_daily (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    usage_date DATE NOT NULL,
    metric_key VARCHAR(64) NOT NULL,
    metric_value BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_tenant_usage_daily_metric (tenant_id, usage_date, metric_key),
    INDEX idx_tenant_usage_daily_tenant_date (tenant_id, usage_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tenant_feature_flags (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    flag_key VARCHAR(80) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    source VARCHAR(24) NOT NULL DEFAULT 'PLAN',
    updated_by VARCHAR(80),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_tenant_feature_flags_key (tenant_id, flag_key),
    INDEX idx_tenant_feature_flags_tenant (tenant_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO subscription_plans (
    id, code, name, price_cents_monthly, currency, max_users, max_customers, max_storage_mb, features_json, enabled, created_at, updated_at
) VALUES
    ('plan_free', 'FREE', 'Free', 0, 'CNY', 3, 200, 512, '["crm.core","dashboard.basic"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('plan_team', 'TEAM', 'Team', 9900, 'CNY', 15, 5000, 5120, '["crm.core","dashboard.basic","collaboration","import_export.basic"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('plan_business', 'BUSINESS', 'Business', 29900, 'CNY', 80, 50000, 51200, '["crm.core","dashboard.basic","collaboration","import_export.basic","approval","reports.advanced","automation","integrations"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('plan_enterprise', 'ENTERPRISE', 'Enterprise', 0, 'CNY', 500, 500000, 512000, '["crm.core","dashboard.basic","collaboration","import_export.basic","approval","reports.advanced","automation","integrations","sso","audit.advanced","permissions.advanced"]', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    price_cents_monthly = VALUES(price_cents_monthly),
    currency = VALUES(currency),
    max_users = VALUES(max_users),
    max_customers = VALUES(max_customers),
    max_storage_mb = VALUES(max_storage_mb),
    features_json = VALUES(features_json),
    enabled = VALUES(enabled),
    updated_at = CURRENT_TIMESTAMP;
