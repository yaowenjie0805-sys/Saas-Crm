CREATE INDEX idx_customers_tenant_status_updated
  ON customers(tenant_id, status, updated_at);

CREATE INDEX idx_customers_tenant_owner_created
  ON customers(tenant_id, owner, created_at);

CREATE INDEX idx_opportunities_tenant_stage_updated
  ON opportunities(tenant_id, stage, updated_at);

CREATE INDEX idx_opportunities_tenant_owner_created
  ON opportunities(tenant_id, owner, created_at);

CREATE INDEX idx_tasks_tenant_done_updated
  ON tasks(tenant_id, done, updated_at);

CREATE INDEX idx_tasks_tenant_owner_created
  ON tasks(tenant_id, owner, created_at);

CREATE INDEX idx_followups_tenant_customer_updated
  ON follow_ups(tenant_id, customer_id, updated_at);

CREATE INDEX idx_followups_tenant_author_updated
  ON follow_ups(tenant_id, author, updated_at);

CREATE INDEX idx_followups_tenant_author_created
  ON follow_ups(tenant_id, author, created_at);

CREATE INDEX idx_payments_tenant_status_updated
  ON payments(tenant_id, status, updated_at);

CREATE INDEX idx_payments_tenant_owner_created
  ON payments(tenant_id, owner, created_at);

CREATE INDEX idx_contracts_tenant_status_updated
  ON contracts(tenant_id, status, updated_at);

CREATE INDEX idx_contracts_tenant_owner_created
  ON contracts(tenant_id, owner, created_at);

CREATE INDEX idx_quotes_tenant_opp_status_owner_updated
  ON quotes(tenant_id, opportunity_id, status, owner, updated_at);

CREATE INDEX idx_orders_tenant_opp_status_owner_updated
  ON order_records(tenant_id, opportunity_id, status, owner, updated_at);

CREATE INDEX idx_lead_import_jobs_tenant_status_created
  ON lead_import_jobs(tenant_id, status, created_at);
