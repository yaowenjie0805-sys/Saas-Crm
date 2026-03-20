ALTER TABLE payments ADD COLUMN order_id VARCHAR(64);
CREATE INDEX idx_payments_tenant_order ON payments(tenant_id, order_id);
