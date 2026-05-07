-- Generated columns for case-insensitive query indexes.
-- MySQL can use indexes on generated columns for matching LOWER/UPPER expressions.

-- SavedSearch: support lower(name) LIKE queries
ALTER TABLE saved_searches
  ADD COLUMN name_ci VARCHAR(120) GENERATED ALWAYS AS (LOWER(name)) STORED;

CREATE INDEX idx_saved_search_tenant_name_ci ON saved_searches(tenant_id, name_ci);

-- UserAccount: support lower(department) equality queries
ALTER TABLE user_accounts
  ADD COLUMN department_ci VARCHAR(80) GENERATED ALWAYS AS (LOWER(department)) STORED;

CREATE INDEX idx_user_account_tenant_dept_ci ON user_accounts(tenant_id, department_ci);

-- UserAccount: support upper(role) equality queries
ALTER TABLE user_accounts
  ADD COLUMN role_ci VARCHAR(30) GENERATED ALWAYS AS (UPPER(role)) STORED;

CREATE INDEX idx_user_account_tenant_role_ci ON user_accounts(tenant_id, role_ci);
