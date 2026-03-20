CREATE TABLE IF NOT EXISTS quote_versions (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  quote_id VARCHAR(64) NOT NULL,
  version_no INT NOT NULL,
  status VARCHAR(32) NOT NULL,
  total_amount BIGINT NOT NULL,
  created_by VARCHAR(120) NOT NULL,
  snapshot_json VARCHAR(2000) NOT NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_quote_versions_tenant_quote_version
  ON quote_versions(tenant_id, quote_id, version_no);

CREATE INDEX idx_quote_versions_tenant_quote_created
  ON quote_versions(tenant_id, quote_id, created_at DESC);

