-- DROP INDEX idx_tasks_tenant_done_updated ON tasks;
-- DROP INDEX idx_tasks_tenant_owner_created ON tasks;

CREATE INDEX idx_tasks_tenant_done_updated_id ON tasks(tenant_id, done, updated_at, id);
CREATE INDEX idx_tasks_tenant_owner_done_updated_id ON tasks(tenant_id, owner, done, updated_at, id);
