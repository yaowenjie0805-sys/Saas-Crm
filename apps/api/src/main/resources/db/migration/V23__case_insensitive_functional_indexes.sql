-- Functional indexes for case-insensitive queries (lower() index support)
-- Avoids full table scan when JPQL uses lower(column) comparisons

-- SavedSearch: support lower(name) LIKE queries
CREATE INDEX idx_saved_search_tenant_name_ci ON saved_search(tenant_id, LOWER(name));

-- UserAccount: support lower(department) equality queries
CREATE INDEX idx_user_account_tenant_dept_ci ON user_accounts(tenant_id, LOWER(department));

-- UserAccount: support upper(role) equality queries
CREATE INDEX idx_user_account_tenant_role_ci ON user_accounts(tenant_id, UPPER(role));
