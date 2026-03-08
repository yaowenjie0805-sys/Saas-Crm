CREATE UNIQUE INDEX IF NOT EXISTS uk_user_accounts_tenant_username ON user_accounts(tenant_id, username);
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_invitations_token ON user_invitations(token);
CREATE UNIQUE INDEX IF NOT EXISTS uk_notification_jobs_dedupe ON notification_jobs(dedupe_key);

CREATE INDEX IF NOT EXISTS idx_customers_tenant_created ON customers(tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_contacts_tenant_created ON contacts(tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_opportunities_tenant_created ON opportunities(tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_contracts_tenant_created ON contracts(tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_payments_tenant_created ON payments(tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_followups_tenant_created ON follow_ups(tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_tasks_tenant_created ON tasks(tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_tenant_created ON audit_logs(tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_approval_templates_tenant ON approval_templates(tenant_id, biz_type, status);
CREATE INDEX IF NOT EXISTS idx_approval_instances_tenant ON approval_instances(tenant_id, status, created_at);
CREATE INDEX IF NOT EXISTS idx_approval_tasks_tenant ON approval_tasks(tenant_id, status, created_at);
CREATE INDEX IF NOT EXISTS idx_notification_jobs_tenant ON notification_jobs(tenant_id, status, created_at);
