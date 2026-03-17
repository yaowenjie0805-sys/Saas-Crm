CREATE TABLE IF NOT EXISTS lead_import_jobs (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  file_name VARCHAR(255),
  status VARCHAR(32) NOT NULL,
  total_rows INT NOT NULL,
  success_count INT NOT NULL,
  fail_count INT NOT NULL,
  result_json TEXT,
  created_by VARCHAR(80),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_lead_import_jobs_tenant_created ON lead_import_jobs(tenant_id, created_at);

CREATE TABLE IF NOT EXISTS lead_assignment_rules (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  name VARCHAR(120) NOT NULL,
  enabled BOOLEAN NOT NULL,
  members_json TEXT,
  rr_cursor INT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_lead_assign_rules_tenant_enabled ON lead_assignment_rules(tenant_id, enabled, updated_at);
