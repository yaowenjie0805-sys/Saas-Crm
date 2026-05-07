ALTER TABLE user_accounts
  ADD CONSTRAINT uk_user_accounts_tenant_username UNIQUE (tenant_id, username);

ALTER TABLE user_invitations
  ADD CONSTRAINT uk_user_invitations_token UNIQUE (token);
