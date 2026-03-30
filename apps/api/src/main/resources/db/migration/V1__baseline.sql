CREATE TABLE IF NOT EXISTS tenants (
  id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  status VARCHAR(20) NOT NULL,
  quota_users INT NOT NULL,
  timezone VARCHAR(40) NOT NULL,
  currency VARCHAR(16) NOT NULL,
  date_format VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS user_accounts (
  id VARCHAR(64) PRIMARY KEY,
  username VARCHAR(80) NOT NULL,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(30) NOT NULL,
  display_name VARCHAR(80) NOT NULL,
  owner_scope VARCHAR(120),
  enabled BOOLEAN NOT NULL,
  tenant_id VARCHAR(64) NOT NULL,
  department VARCHAR(80),
  data_scope VARCHAR(30),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  created_by VARCHAR(80),
  last_modified_by VARCHAR(80),
  deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS user_invitations (
  id VARCHAR(64) PRIMARY KEY,
  token VARCHAR(120) NOT NULL,
  tenant_id VARCHAR(64) NOT NULL,
  username VARCHAR(80) NOT NULL,
  role VARCHAR(30) NOT NULL,
  owner_scope VARCHAR(120),
  department VARCHAR(80),
  data_scope VARCHAR(30),
  display_name VARCHAR(80),
  invited_by VARCHAR(80),
  expires_at TIMESTAMP,
  used BOOLEAN NOT NULL,
  used_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS customers (
  id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  owner VARCHAR(120) NOT NULL,
  tag VARCHAR(120) NOT NULL,
  value BIGINT NOT NULL,
  status VARCHAR(120) NOT NULL,
  tenant_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS contacts (
  id VARCHAR(64) PRIMARY KEY,
  customer_id VARCHAR(64) NOT NULL,
  name VARCHAR(120) NOT NULL,
  title VARCHAR(120),
  phone VARCHAR(40),
  email VARCHAR(120),
  owner VARCHAR(120),
  tenant_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS opportunities (
  id VARCHAR(64) PRIMARY KEY,
  stage VARCHAR(80) NOT NULL,
  count INT NOT NULL,
  amount BIGINT NOT NULL,
  owner VARCHAR(120) NOT NULL,
  progress INT NOT NULL,
  tenant_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS contracts (
  id VARCHAR(64) PRIMARY KEY,
  customer_id VARCHAR(64) NOT NULL,
  contract_no VARCHAR(80),
  title VARCHAR(255) NOT NULL,
  amount BIGINT NOT NULL,
  status VARCHAR(40) NOT NULL,
  sign_date DATE,
  owner VARCHAR(120),
  tenant_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS payments (
  id VARCHAR(64) PRIMARY KEY,
  customer_id VARCHAR(64),
  contract_id VARCHAR(64) NOT NULL,
  amount BIGINT NOT NULL,
  received_date DATE,
  method VARCHAR(40),
  status VARCHAR(40) NOT NULL,
  remark VARCHAR(255),
  owner VARCHAR(120),
  tenant_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS follow_ups (
  id VARCHAR(64) PRIMARY KEY,
  customer_id VARCHAR(64) NOT NULL,
  author VARCHAR(120),
  summary VARCHAR(255) NOT NULL,
  channel VARCHAR(80),
  result VARCHAR(255),
  tenant_id VARCHAR(64) NOT NULL,
  next_action_date DATE,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS tasks (
  id VARCHAR(64) PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  time VARCHAR(120),
  level VARCHAR(40),
  done BOOLEAN NOT NULL,
  owner VARCHAR(120),
  tenant_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_logs (
  id VARCHAR(64) PRIMARY KEY,
  username VARCHAR(80) NOT NULL,
  role VARCHAR(30) NOT NULL,
  action VARCHAR(80) NOT NULL,
  resource VARCHAR(80) NOT NULL,
  resource_id VARCHAR(80),
  details TEXT,
  tenant_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS automation_rules (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  name VARCHAR(120) NOT NULL,
  trigger_type VARCHAR(60) NOT NULL,
  trigger_expr VARCHAR(255) NOT NULL,
  action_type VARCHAR(60) NOT NULL,
  action_payload TEXT,
  enabled BOOLEAN NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS approval_templates (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  biz_type VARCHAR(40) NOT NULL,
  name VARCHAR(80) NOT NULL,
  amount_min BIGINT,
  amount_max BIGINT,
  role VARCHAR(40),
  department VARCHAR(80),
  approver_roles VARCHAR(400) NOT NULL,
  flow_definition TEXT,
  version INT NOT NULL,
  status VARCHAR(20) NOT NULL,
  enabled BOOLEAN NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS approval_template_versions (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  template_id VARCHAR(64) NOT NULL,
  version INT NOT NULL,
  biz_type VARCHAR(40) NOT NULL,
  name VARCHAR(80) NOT NULL,
  role VARCHAR(40),
  department VARCHAR(80),
  approver_roles VARCHAR(400) NOT NULL,
  flow_definition TEXT,
  status VARCHAR(20) NOT NULL,
  published_by VARCHAR(80) NOT NULL,
  published_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS approval_instances (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  template_id VARCHAR(64) NOT NULL,
  template_version INT,
  biz_type VARCHAR(40) NOT NULL,
  biz_id VARCHAR(64) NOT NULL,
  status VARCHAR(20) NOT NULL,
  submitter VARCHAR(80) NOT NULL,
  comment VARCHAR(255),
  current_seq INT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS approval_tasks (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  instance_id VARCHAR(64) NOT NULL,
  approver_role VARCHAR(40) NOT NULL,
  approver_user VARCHAR(80),
  status VARCHAR(20) NOT NULL,
  comment VARCHAR(255),
  seq INT NOT NULL,
  node_key VARCHAR(64),
  sla_minutes INT,
  deadline_at TIMESTAMP,
  escalate_to_roles VARCHAR(255),
  escalation_level INT,
  escalation_source_task_id VARCHAR(64),
  notified_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS approval_events (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  instance_id VARCHAR(64) NOT NULL,
  task_id VARCHAR(64),
  event_type VARCHAR(60) NOT NULL,
  operator_user VARCHAR(80),
  detail VARCHAR(255),
  request_id VARCHAR(64),
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS notification_jobs (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  event_type VARCHAR(80) NOT NULL,
  target VARCHAR(40) NOT NULL,
  payload TEXT,
  status VARCHAR(20) NOT NULL,
  retry_count INT NOT NULL,
  max_retries INT NOT NULL,
  next_retry_at TIMESTAMP,
  last_error VARCHAR(500),
  dedupe_key VARCHAR(200) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

