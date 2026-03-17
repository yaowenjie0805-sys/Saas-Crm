-- Idempotent dual-market demo seed for local development.
-- Usage:
--   mysql -h127.0.0.1 -P3306 -uroot -proot crm_local < scripts/seed-dual-market-demo.sql

INSERT INTO tenants (
  id, name, status, quota_users, timezone, currency, date_format,
  market_profile, tax_rule, approval_mode, channels_json, data_residency, mask_level,
  created_at, updated_at
) VALUES
  ('tenant_cn_demo', 'China Demo Tenant', 'ACTIVE', 120, 'Asia/Shanghai', 'CNY', 'yyyy-MM-dd',
   'CN', 'VAT_CN', 'STRICT', '["WECOM","DINGTALK"]', 'CN', 'STANDARD', NOW(), NOW()),
  ('tenant_global_demo', 'Global Demo Tenant', 'ACTIVE', 120, 'UTC', 'USD', 'yyyy-MM-dd',
   'GLOBAL', 'VAT_GLOBAL', 'STAGE_GATE', '["EMAIL","SLACK"]', 'GLOBAL', 'STRICT', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  status = VALUES(status),
  quota_users = VALUES(quota_users),
  timezone = VALUES(timezone),
  currency = VALUES(currency),
  date_format = VALUES(date_format),
  market_profile = VALUES(market_profile),
  tax_rule = VALUES(tax_rule),
  approval_mode = VALUES(approval_mode),
  channels_json = VALUES(channels_json),
  data_residency = VALUES(data_residency),
  mask_level = VALUES(mask_level),
  updated_at = NOW();

INSERT INTO user_accounts (
  id, username, password, role, display_name, owner_scope, enabled, tenant_id, department, data_scope
)
SELECT CONCAT('u_cn_demo_', ua.username), CONCAT('cn_', ua.username), ua.password, ua.role,
       CONCAT('CN Demo ', ua.display_name), ua.owner_scope, ua.enabled, 'tenant_cn_demo',
       COALESCE(ua.department, 'DEFAULT'), COALESCE(ua.data_scope, 'SELF')
FROM user_accounts ua
WHERE ua.tenant_id = 'tenant_default' AND ua.username IN ('admin','manager','sales','analyst')
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  role = VALUES(role),
  display_name = VALUES(display_name),
  owner_scope = VALUES(owner_scope),
  enabled = VALUES(enabled),
  department = VALUES(department),
  data_scope = VALUES(data_scope);

INSERT INTO user_accounts (
  id, username, password, role, display_name, owner_scope, enabled, tenant_id, department, data_scope
)
SELECT CONCAT('u_gl_demo_', ua.username), CONCAT('gl_', ua.username), ua.password, ua.role,
       CONCAT('GLOBAL Demo ', ua.display_name), ua.owner_scope, ua.enabled, 'tenant_global_demo',
       COALESCE(ua.department, 'DEFAULT'), COALESCE(ua.data_scope, 'SELF')
FROM user_accounts ua
WHERE ua.tenant_id = 'tenant_default' AND ua.username IN ('admin','manager','sales','analyst')
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  role = VALUES(role),
  display_name = VALUES(display_name),
  owner_scope = VALUES(owner_scope),
  enabled = VALUES(enabled),
  department = VALUES(department),
  data_scope = VALUES(data_scope);

INSERT INTO customers (id, name, owner, tag, value, status, tenant_id, created_at, updated_at) VALUES
  ('c_cn_01', 'Huajing Manufacturing', 'sales', 'A', 260000, 'ACTIVE', 'tenant_cn_demo', NOW(), NOW()),
  ('c_cn_02', 'Lingfeng Medical', 'manager', 'NEW', 220000, 'PENDING', 'tenant_cn_demo', NOW(), NOW()),
  ('c_gl_01', 'Northwind Labs', 'sales', 'A', 420000, 'ACTIVE', 'tenant_global_demo', NOW(), NOW()),
  ('c_gl_02', 'Borealis Retail', 'manager', 'NEW', 360000, 'PENDING', 'tenant_global_demo', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  name = VALUES(name), owner = VALUES(owner), tag = VALUES(tag), value = VALUES(value),
  status = VALUES(status), tenant_id = VALUES(tenant_id), updated_at = NOW();

INSERT INTO leads (id, name, company, phone, email, status, owner, source, tenant_id, created_at, updated_at) VALUES
  ('l_cn_01', 'CN Seed Lead 1', 'Huajing Manufacturing', '13900000001', 'cn.seed1@example.com', 'NEW', 'sales', 'WECOM', 'tenant_cn_demo', NOW(), NOW()),
  ('l_cn_02', 'CN Seed Lead 2', 'Lingfeng Medical', '13900000002', 'cn.seed2@example.com', 'QUALIFIED', 'manager', 'REFERRAL', 'tenant_cn_demo', NOW(), NOW()),
  ('l_gl_01', 'GLOBAL Seed Lead 1', 'Northwind Labs', '13900000011', 'gl.seed1@example.com', 'NEW', 'sales', 'WEBSITE', 'tenant_global_demo', NOW(), NOW()),
  ('l_gl_02', 'GLOBAL Seed Lead 2', 'Borealis Retail', '13900000012', 'gl.seed2@example.com', 'NURTURING', 'manager', 'EMAIL', 'tenant_global_demo', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  name = VALUES(name), company = VALUES(company), phone = VALUES(phone), email = VALUES(email),
  status = VALUES(status), owner = VALUES(owner), source = VALUES(source), tenant_id = VALUES(tenant_id), updated_at = NOW();

INSERT INTO lead_assignment_rules (id, tenant_id, name, enabled, members_json, rr_cursor, created_at, updated_at) VALUES
  ('lar_cn_01', 'tenant_cn_demo', 'CN demo rule', 1, '[{"username":"sales","weight":2,"enabled":true},{"username":"manager","weight":1,"enabled":true}]', 0, NOW(), NOW()),
  ('lar_gl_01', 'tenant_global_demo', 'GLOBAL demo rule', 1, '[{"username":"sales","weight":2,"enabled":true},{"username":"manager","weight":1,"enabled":true}]', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  name = VALUES(name), enabled = VALUES(enabled), members_json = VALUES(members_json), rr_cursor = VALUES(rr_cursor), updated_at = NOW();

INSERT INTO lead_import_jobs (
  id, tenant_id, file_name, status, total_rows, success_count, fail_count, result_json,
  created_by, created_at, updated_at, processed_rows, percent, last_heartbeat_at, cancel_requested, error_message
) VALUES
  ('lij_cn_demo', 'tenant_cn_demo', 'lead-import-cn.csv', 'PARTIAL_SUCCESS', 6, 4, 2, NULL, 'admin', NOW(), NOW(), 6, 100, NOW(), 0, 'lead_import_partial_failure'),
  ('lij_gl_demo', 'tenant_global_demo', 'lead-import-global.csv', 'PARTIAL_SUCCESS', 6, 4, 2, NULL, 'admin', NOW(), NOW(), 6, 100, NOW(), 0, 'lead_import_partial_failure')
ON DUPLICATE KEY UPDATE
  file_name = VALUES(file_name), status = VALUES(status), total_rows = VALUES(total_rows), success_count = VALUES(success_count),
  fail_count = VALUES(fail_count), processed_rows = VALUES(processed_rows), percent = VALUES(percent),
  last_heartbeat_at = VALUES(last_heartbeat_at), cancel_requested = VALUES(cancel_requested), error_message = VALUES(error_message), updated_at = NOW();

INSERT INTO lead_import_job_chunks (
  id, tenant_id, job_id, chunk_no, status, payload_json, retry_count, last_error, created_at, updated_at
) VALUES
  ('ljc_cn_01', 'tenant_cn_demo', 'lij_cn_demo', 1, 'PROCESSED', '[]', 0, NULL, NOW(), NOW()),
  ('ljc_cn_02', 'tenant_cn_demo', 'lij_cn_demo', 2, 'PROCESSED', '[]', 0, NULL, NOW(), NOW()),
  ('ljc_gl_01', 'tenant_global_demo', 'lij_gl_demo', 1, 'PROCESSED', '[]', 0, NULL, NOW(), NOW()),
  ('ljc_gl_02', 'tenant_global_demo', 'lij_gl_demo', 2, 'PROCESSED', '[]', 0, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  status = VALUES(status), payload_json = VALUES(payload_json), retry_count = VALUES(retry_count),
  last_error = VALUES(last_error), updated_at = NOW();

INSERT INTO lead_import_job_items (
  id, tenant_id, job_id, line_no, status, raw_line, error_code, error_message, created_at
) VALUES
  ('lji_cn_01', 'tenant_cn_demo', 'lij_cn_demo', 3, 'FAILED', 'CN duplicate row', 'lead_import_duplicate', 'duplicate lead', NOW()),
  ('lji_cn_02', 'tenant_cn_demo', 'lij_cn_demo', 5, 'FAILED', 'CN invalid phone', 'contact_phone_invalid', 'invalid phone', NOW()),
  ('lji_gl_01', 'tenant_global_demo', 'lij_gl_demo', 4, 'FAILED', 'GLOBAL duplicate row', 'lead_import_duplicate', 'duplicate lead', NOW()),
  ('lji_gl_02', 'tenant_global_demo', 'lij_gl_demo', 6, 'FAILED', 'GLOBAL invalid phone', 'contact_phone_invalid', 'invalid phone', NOW())
ON DUPLICATE KEY UPDATE
  line_no = VALUES(line_no), status = VALUES(status), raw_line = VALUES(raw_line),
  error_code = VALUES(error_code), error_message = VALUES(error_message), created_at = VALUES(created_at);

INSERT INTO opportunities (
  id, stage, count, amount, owner, progress, tenant_id, settlement_currency, exchange_rate_snapshot, tax_display_mode, compliance_tag, created_at, updated_at
) VALUES
  ('o_cn_01', 'QUALIFIED', 12, 320000, 'sales', 50, 'tenant_cn_demo', 'CNY', '1.000000@PBOC', 'TAX_INCLUSIVE', 'CN_LOCAL', NOW(), NOW()),
  ('o_cn_02', 'PROPOSAL', 8, 280000, 'manager', 65, 'tenant_cn_demo', 'CNY', '1.000000@PBOC', 'TAX_INCLUSIVE', 'CN_LOCAL', NOW(), NOW()),
  ('o_gl_01', 'QUALIFIED', 10, 500000, 'sales', 45, 'tenant_global_demo', 'USD', '1.080000@ECB', 'TAX_EXCLUSIVE', 'GDPR_SAFE', NOW(), NOW()),
  ('o_gl_02', 'NEGOTIATION', 6, 460000, 'manager', 72, 'tenant_global_demo', 'USD', '1.080000@ECB', 'TAX_EXCLUSIVE', 'GDPR_SAFE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  stage = VALUES(stage), count = VALUES(count), amount = VALUES(amount), owner = VALUES(owner), progress = VALUES(progress),
  tenant_id = VALUES(tenant_id), settlement_currency = VALUES(settlement_currency), exchange_rate_snapshot = VALUES(exchange_rate_snapshot),
  tax_display_mode = VALUES(tax_display_mode), compliance_tag = VALUES(compliance_tag), updated_at = NOW();

INSERT INTO products (
  id, tenant_id, code, name, category, status, standard_price, tax_rate, currency, unit, sale_region, created_at, updated_at
) VALUES
  ('prd_cn_01', 'tenant_cn_demo', 'SKU-CN-CRM', 'CRM License CN', 'SOFTWARE', 'ACTIVE', 180000, 0.0600, 'CNY', 'item', 'CN', NOW(), NOW()),
  ('prd_cn_02', 'tenant_cn_demo', 'SKU-CN-PS', 'Implementation Service CN', 'SERVICE', 'ACTIVE', 120000, 0.0600, 'CNY', 'item', 'CN', NOW(), NOW()),
  ('prd_gl_01', 'tenant_global_demo', 'SKU-GL-CRM', 'CRM Subscription', 'SOFTWARE', 'ACTIVE', 260000, 0.0600, 'USD', 'item', 'GLOBAL', NOW(), NOW()),
  ('prd_gl_02', 'tenant_global_demo', 'SKU-GL-PS', 'Professional Service', 'SERVICE', 'ACTIVE', 140000, 0.0600, 'USD', 'item', 'GLOBAL', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id), code = VALUES(code), name = VALUES(name), category = VALUES(category), status = VALUES(status),
  standard_price = VALUES(standard_price), tax_rate = VALUES(tax_rate), currency = VALUES(currency), unit = VALUES(unit),
  sale_region = VALUES(sale_region), updated_at = NOW();

INSERT INTO quotes (
  id, tenant_id, quote_no, customer_id, opportunity_id, owner, status, subtotal_amount, tax_amount, total_amount,
  settlement_currency, exchange_rate_snapshot, invoice_status, tax_display_mode, compliance_tag, version, valid_until, notes, created_at, updated_at
) VALUES
  ('q_cn_01', 'tenant_cn_demo', 'QT-CN-001', 'c_cn_01', 'o_cn_01', 'sales', 'APPROVED', 300000, 18000, 318000,
   'CNY', '1.000000@PBOC', 'NOT_REQUIRED', 'TAX_INCLUSIVE', 'CN_LOCAL', 1, DATE_ADD(CURDATE(), INTERVAL 30 DAY), 'CN demo quote', NOW(), NOW()),
  ('q_gl_01', 'tenant_global_demo', 'QT-GL-001', 'c_gl_01', 'o_gl_01', 'sales', 'SUBMITTED', 400000, 24000, 424000,
   'USD', '1.080000@ECB', 'PENDING', 'TAX_EXCLUSIVE', 'GDPR_SAFE', 1, DATE_ADD(CURDATE(), INTERVAL 30 DAY), 'GLOBAL demo quote', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id), quote_no = VALUES(quote_no), customer_id = VALUES(customer_id), opportunity_id = VALUES(opportunity_id),
  owner = VALUES(owner), status = VALUES(status), subtotal_amount = VALUES(subtotal_amount), tax_amount = VALUES(tax_amount),
  total_amount = VALUES(total_amount), settlement_currency = VALUES(settlement_currency), exchange_rate_snapshot = VALUES(exchange_rate_snapshot),
  invoice_status = VALUES(invoice_status), tax_display_mode = VALUES(tax_display_mode), compliance_tag = VALUES(compliance_tag),
  version = VALUES(version), valid_until = VALUES(valid_until), notes = VALUES(notes), updated_at = NOW();

INSERT INTO quote_items (
  id, tenant_id, quote_id, product_id, product_name, quantity, unit_price, discount_rate, tax_rate, subtotal_amount, tax_amount, total_amount, created_at, updated_at
) VALUES
  ('qi_cn_01', 'tenant_cn_demo', 'q_cn_01', 'prd_cn_01', 'CRM License CN', 1, 180000, 0, 0.0600, 180000, 10800, 190800, NOW(), NOW()),
  ('qi_cn_02', 'tenant_cn_demo', 'q_cn_01', 'prd_cn_02', 'Implementation Service CN', 1, 120000, 0, 0.0600, 120000, 7200, 127200, NOW(), NOW()),
  ('qi_gl_01', 'tenant_global_demo', 'q_gl_01', 'prd_gl_01', 'CRM Subscription', 1, 260000, 0, 0.0600, 260000, 15600, 275600, NOW(), NOW()),
  ('qi_gl_02', 'tenant_global_demo', 'q_gl_01', 'prd_gl_02', 'Professional Service', 1, 140000, 0, 0.0600, 140000, 8400, 148400, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id), quote_id = VALUES(quote_id), product_id = VALUES(product_id), product_name = VALUES(product_name),
  quantity = VALUES(quantity), unit_price = VALUES(unit_price), discount_rate = VALUES(discount_rate), tax_rate = VALUES(tax_rate),
  subtotal_amount = VALUES(subtotal_amount), tax_amount = VALUES(tax_amount), total_amount = VALUES(total_amount), updated_at = NOW();

INSERT INTO order_records (
  id, tenant_id, order_no, customer_id, opportunity_id, quote_id, owner, status, amount,
  settlement_currency, exchange_rate_snapshot, invoice_status, tax_display_mode, compliance_tag, sign_date, notes, created_at, updated_at
) VALUES
  ('ord_cn_01', 'tenant_cn_demo', 'ORD-CN-001', 'c_cn_01', 'o_cn_01', 'q_cn_01', 'sales', 'FULFILLING', 318000,
   'CNY', '1.000000@PBOC', 'NOT_REQUIRED', 'TAX_INCLUSIVE', 'CN_LOCAL', DATE_SUB(CURDATE(), INTERVAL 3 DAY), 'CN demo order', NOW(), NOW()),
  ('ord_gl_01', 'tenant_global_demo', 'ORD-GL-001', 'c_gl_01', 'o_gl_01', 'q_gl_01', 'sales', 'CONFIRMED', 424000,
   'USD', '1.080000@ECB', 'PENDING', 'TAX_EXCLUSIVE', 'GDPR_SAFE', DATE_SUB(CURDATE(), INTERVAL 3 DAY), 'GLOBAL demo order', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id), order_no = VALUES(order_no), customer_id = VALUES(customer_id), opportunity_id = VALUES(opportunity_id),
  quote_id = VALUES(quote_id), owner = VALUES(owner), status = VALUES(status), amount = VALUES(amount),
  settlement_currency = VALUES(settlement_currency), exchange_rate_snapshot = VALUES(exchange_rate_snapshot),
  invoice_status = VALUES(invoice_status), tax_display_mode = VALUES(tax_display_mode), compliance_tag = VALUES(compliance_tag),
  sign_date = VALUES(sign_date), notes = VALUES(notes), updated_at = NOW();

INSERT INTO contracts (
  id, customer_id, contract_no, title, amount, status, sign_date, owner, tenant_id,
  settlement_currency, exchange_rate_snapshot, invoice_status, tax_display_mode, compliance_tag, created_at, updated_at
) VALUES
  ('cr_cn_01', 'c_cn_01', 'CT-CN-001', 'CN Annual CRM Contract', 318000, 'SIGNED', DATE_SUB(CURDATE(), INTERVAL 8 DAY), 'manager', 'tenant_cn_demo',
   'CNY', '1.000000@PBOC', 'NOT_REQUIRED', 'TAX_INCLUSIVE', 'CN_LOCAL', NOW(), NOW()),
  ('cr_gl_01', 'c_gl_01', 'CT-GL-001', 'Annual CRM Contract', 424000, 'SIGNED', DATE_SUB(CURDATE(), INTERVAL 8 DAY), 'manager', 'tenant_global_demo',
   'USD', '1.080000@ECB', 'PENDING', 'TAX_EXCLUSIVE', 'GDPR_SAFE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  customer_id = VALUES(customer_id), contract_no = VALUES(contract_no), title = VALUES(title), amount = VALUES(amount),
  status = VALUES(status), sign_date = VALUES(sign_date), owner = VALUES(owner), tenant_id = VALUES(tenant_id),
  settlement_currency = VALUES(settlement_currency), exchange_rate_snapshot = VALUES(exchange_rate_snapshot),
  invoice_status = VALUES(invoice_status), tax_display_mode = VALUES(tax_display_mode), compliance_tag = VALUES(compliance_tag), updated_at = NOW();

INSERT INTO payments (
  id, customer_id, contract_id, order_id, amount, received_date, method, status, remark, owner, tenant_id,
  settlement_currency, exchange_rate_snapshot, invoice_status, tax_display_mode, compliance_tag, created_at, updated_at
) VALUES
  ('pm_cn_01', 'c_cn_01', 'cr_cn_01', 'ord_cn_01', 80000, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'BANK_TRANSFER', 'RECEIVED', 'CN phase-1 payment', 'sales', 'tenant_cn_demo',
   'CNY', '1.000000@PBOC', 'NOT_REQUIRED', 'TAX_INCLUSIVE', 'CN_LOCAL', NOW(), NOW()),
  ('pm_gl_01', 'c_gl_01', 'cr_gl_01', 'ord_gl_01', 120000, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'BANK_TRANSFER', 'RECEIVED', 'GLOBAL phase-1 payment', 'sales', 'tenant_global_demo',
   'USD', '1.080000@ECB', 'PENDING', 'TAX_EXCLUSIVE', 'GDPR_SAFE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  customer_id = VALUES(customer_id), contract_id = VALUES(contract_id), order_id = VALUES(order_id), amount = VALUES(amount),
  received_date = VALUES(received_date), method = VALUES(method), status = VALUES(status), remark = VALUES(remark),
  owner = VALUES(owner), tenant_id = VALUES(tenant_id), settlement_currency = VALUES(settlement_currency),
  exchange_rate_snapshot = VALUES(exchange_rate_snapshot), invoice_status = VALUES(invoice_status),
  tax_display_mode = VALUES(tax_display_mode), compliance_tag = VALUES(compliance_tag), updated_at = NOW();

INSERT INTO approval_templates (
  id, tenant_id, biz_type, name, amount_min, amount_max, role, department, approver_roles, flow_definition, version, status, enabled, created_at, updated_at
) VALUES
  ('apt_cn_01', 'tenant_cn_demo', 'QUOTE', 'CN quote approval flow', 200000, NULL, 'SALES', 'DEFAULT', '["MANAGER","ADMIN"]',
   '{"nodes":[{"key":"n1","role":"MANAGER"},{"key":"n2","role":"ADMIN"}]}', 1, 'PUBLISHED', 1, NOW(), NOW()),
  ('apt_gl_01', 'tenant_global_demo', 'QUOTE', 'Quote approval flow', 200000, NULL, 'SALES', 'DEFAULT', '["MANAGER","ADMIN"]',
   '{"nodes":[{"key":"n1","role":"MANAGER"},{"key":"n2","role":"ADMIN"}]}', 1, 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  tenant_id = VALUES(tenant_id), biz_type = VALUES(biz_type), name = VALUES(name), amount_min = VALUES(amount_min),
  amount_max = VALUES(amount_max), role = VALUES(role), department = VALUES(department), approver_roles = VALUES(approver_roles),
  flow_definition = VALUES(flow_definition), version = VALUES(version), status = VALUES(status), enabled = VALUES(enabled), updated_at = NOW();
