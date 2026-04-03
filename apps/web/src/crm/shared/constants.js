export const FILTERS_KEY = 'crm_filters_v2'
export const LANG_KEY = 'crm_lang'
export const OIDC_STATE_KEY = 'crm_oidc_state'

export const ROLES = ['ADMIN', 'MANAGER', 'SALES', 'ANALYST']
export const READ_OPS = ['opViewDashboard', 'opViewReports']
export const WRITE_OPS = ['opManageCustomers', 'opDeleteCustomers', 'opManageTasks', 'opManageFollowUps', 'opCreateOpportunity', 'opEditOpportunityAmount']

export const CUSTOMER_STATUS_OPTIONS = ['Active', 'Pending', 'Inactive']
export const OPPORTUNITY_STAGE_OPTIONS = ['Lead', 'Qualified', 'Proposal', 'Negotiation', 'Closed Won', 'Closed Lost']
export const CONTRACT_STATUS_OPTIONS = ['Draft', 'Signed']
export const PAYMENT_STATUS_OPTIONS = ['Pending', 'Received', 'Overdue']
export const PAYMENT_METHOD_OPTIONS = ['Bank', 'Transfer', 'Cash', 'Card']
export const FOLLOW_UP_CHANNEL_OPTIONS = ['Phone', 'Email', 'WeChat', 'Visit', 'Meeting']
export const MARKET_PROFILE_OPTIONS = ['CN', 'GLOBAL']
export const TENANT_APPROVAL_MODE_OPTIONS = ['STRICT', 'STAGE_GATE']

export const STORAGE_KEYS = {
  AUTH: 'crm_auth',
  LAST_TENANT: 'crm_last_tenant',
  FILTERS: FILTERS_KEY,
  LANG: LANG_KEY,
  OIDC_STATE: OIDC_STATE_KEY,
  TOKEN: 'token',
  TENANT_ID: 'tenantId',
}

export const TENANT_OPTIONAL_PATH_PREFIXES = ['/auth/', '/v1/auth/']
