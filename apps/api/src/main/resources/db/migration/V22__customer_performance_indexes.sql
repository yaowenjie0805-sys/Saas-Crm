-- Customer表性能索引优化

-- 优化 findByIdAndTenantId 查询
CREATE INDEX idx_customers_id_tenant ON customers(id, tenant_id);

-- 优化 findByTenantIdAndIdIn 查询
CREATE INDEX idx_customers_tenant_id ON customers(tenant_id, id);

-- 优化 findByTenantIdAndOwnerIn 查询
CREATE INDEX idx_customers_tenant_owner ON customers(tenant_id, owner);

-- 优化 findByTenantIdAndOwnerInAndCreatedAtBetween 查询
CREATE INDEX idx_customers_tenant_owner_created ON customers(tenant_id, owner, created_at);

-- 优化 findTop8ByTenantIdOrderByUpdatedAtDesc 查询
CREATE INDEX idx_customers_tenant_updated ON customers(tenant_id, updated_at DESC);

-- 优化按状态分组的查询
CREATE INDEX idx_customers_tenant_status ON customers(tenant_id, status);

-- 优化按状态和值的查询
CREATE INDEX idx_customers_tenant_status_value ON customers(tenant_id, status, value);
