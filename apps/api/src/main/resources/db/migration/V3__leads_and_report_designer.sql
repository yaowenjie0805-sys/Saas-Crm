CREATE TABLE IF NOT EXISTS leads (
  id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  company VARCHAR(120),
  phone VARCHAR(120),
  email VARCHAR(120),
  status VARCHAR(40) NOT NULL,
  owner VARCHAR(120) NOT NULL,
  source VARCHAR(255),
  tenant_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_leads_tenant_status ON leads(tenant_id, status);
CREATE INDEX idx_leads_tenant_owner ON leads(tenant_id, owner);

CREATE TABLE IF NOT EXISTS report_designer_templates (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  name VARCHAR(120) NOT NULL,
  visibility VARCHAR(32) NOT NULL,
  owner VARCHAR(80) NOT NULL,
  department VARCHAR(80),
  dataset VARCHAR(40) NOT NULL,
  config_json TEXT,
  version INT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_report_tpl_tenant_updated ON report_designer_templates(tenant_id, updated_at);
CREATE UNIQUE INDEX uk_report_tpl_tenant_name ON report_designer_templates(tenant_id, name);
