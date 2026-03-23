-- ============================================================
-- V12: 鎼滅储绱㈠紩鍜屽揩鎹风瓫閫夊姛鑳?
-- 鏀寔鍏ㄥ眬鎼滅储锛堝惈鎷奸煶鎼滅储锛夊拰蹇嵎绛涢€夋爣绛?
-- ============================================================

-- 鎼滅储绱㈠紩琛?
CREATE TABLE IF NOT EXISTS search_index (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    search_content TEXT NOT NULL,
    pinyin_content VARCHAR(500),
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_search_index_tenant_entity (tenant_id, entity_type),
    INDEX idx_search_index_entity_id (entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 添加兼容 MySQL/H2 的搜索索引
CREATE INDEX IF NOT EXISTS idx_search_content ON search_index(search_content, pinyin_content);

-- 淇濆瓨鐨勬悳绱㈠巻鍙?
CREATE TABLE IF NOT EXISTS saved_searches (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    owner VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    search_type VARCHAR(40) NOT NULL,
    query_json TEXT NOT NULL,
    is_shared BOOLEAN NOT NULL DEFAULT FALSE,
    share_with_roles VARCHAR(255),
    usage_count INT NOT NULL DEFAULT 0,
    last_used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_saved_searches_tenant_owner (tenant_id, owner),
    INDEX idx_saved_searches_type (search_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 蹇嵎绛涢€夐璁撅紙鍥藉唴鐗硅壊锛?
CREATE TABLE IF NOT EXISTS quick_filters (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    owner VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    icon VARCHAR(40),
    entity_type VARCHAR(40) NOT NULL,
    filter_config TEXT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_quick_filters_tenant_entity_order (tenant_id, entity_type, display_order),
    INDEX idx_quick_filters_owner_default (owner, is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 淇濆瓨鐨勯珮绾х瓫閫?
CREATE TABLE IF NOT EXISTS saved_filters (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    owner VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    filter_config TEXT NOT NULL,
    is_shared BOOLEAN NOT NULL DEFAULT FALSE,
    share_with_roles VARCHAR(255),
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_saved_filters_tenant_entity (tenant_id, entity_type),
    INDEX idx_saved_filters_owner_shared (owner, is_shared)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
