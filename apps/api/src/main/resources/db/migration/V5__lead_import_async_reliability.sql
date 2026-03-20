ALTER TABLE lead_import_jobs ADD COLUMN processed_rows INT NOT NULL DEFAULT 0;
ALTER TABLE lead_import_jobs ADD COLUMN percent INT NOT NULL DEFAULT 0;
ALTER TABLE lead_import_jobs ADD COLUMN last_heartbeat_at TIMESTAMP NULL;
ALTER TABLE lead_import_jobs ADD COLUMN cancel_requested BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE lead_import_jobs ADD COLUMN error_message VARCHAR(500) NULL;

CREATE TABLE IF NOT EXISTS lead_import_job_items (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  job_id VARCHAR(64) NOT NULL,
  line_no INT NOT NULL,
  status VARCHAR(20) NOT NULL,
  raw_line TEXT,
  error_code VARCHAR(80),
  error_message VARCHAR(500),
  created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_lead_import_items_job_status ON lead_import_job_items(tenant_id, job_id, status, line_no);

CREATE TABLE IF NOT EXISTS lead_import_job_chunks (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  job_id VARCHAR(64) NOT NULL,
  chunk_no INT NOT NULL,
  status VARCHAR(20) NOT NULL,
  payload_json TEXT,
  retry_count INT NOT NULL,
  last_error VARCHAR(500),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_lead_import_chunk_no ON lead_import_job_chunks(tenant_id, job_id, chunk_no);
CREATE INDEX idx_lead_import_chunks_status ON lead_import_job_chunks(tenant_id, job_id, status, chunk_no);
