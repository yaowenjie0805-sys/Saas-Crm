export const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
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

const normalize = (value) => String(value || '').trim().replace(/[-\s]+/g, '_').toUpperCase()

const ROLE_MAP = {
  ADMIN: 'roleAdmin',
  ROLEADMIN: 'roleAdmin',
  MANAGER: 'roleManager',
  ROLEMANAGER: 'roleManager',
  SALES: 'roleSales',
  ROLESALES: 'roleSales',
  ANALYST: 'roleAnalyst',
  ROLEANALYST: 'roleAnalyst',
}

const STATUS_MAP = {
  ACTIVE: 'statusActive',
  PENDING: 'statusPending',
  WAITING: 'statusWaiting',
  INACTIVE: 'statusInactive',
  NEW: 'statusNew',
  OPEN: 'statusOpen',
  WON: 'statusWon',
  LOST: 'statusLost',
  DRAFT: 'statusDraft',
  SUBMITTED: 'statusSubmitted',
  CONFIRMED: 'statusConfirmed',
  FULFILLING: 'statusFulfilling',
  APPROVED: 'statusApproved',
  REJECTED: 'statusRejected',
  ESCALATED: 'statusEscalated',
  CANCELED: 'statusCanceled',
  RETRY: 'statusRetry',
  SUCCESS: 'statusSuccess',
  PARTIAL_SUCCESS: 'statusPartialSuccess',
  SIGNED: 'statusSigned',
  RECEIVED: 'statusReceived',
  COMPLETED: 'statusCompleted',
  FAILED: 'statusFailed',
  RUNNING: 'statusRunning',
  DONE: 'statusDone',
  PAID: 'statusPaid',
  UNPAID: 'statusUnpaid',
  OVERDUE: 'statusOverdue',
}

const STAGE_MAP = {
  LEAD: 'stageLead',
  QUALIFIED: 'stageQualified',
  PROPOSAL: 'stageProposal',
  NEGOTIATION: 'stageNegotiation',
  CLOSED_WON: 'stageClosedWon',
  CLOSED_LOST: 'stageClosedLost',
  STAGELEAD: 'stageLead',
  STAGEQUALIFIED: 'stageQualified',
  STAGEPROPOSAL: 'stageProposal',
  STAGENEGOTIATION: 'stageNegotiation',
  STAGECLOSEDWON: 'stageClosedWon',
  STAGECLOSEDLOST: 'stageClosedLost',
}

const CHANNEL_MAP = {
  PHONE: 'channelPhone',
  EMAIL: 'channelEmail',
  WECHAT: 'channelWechat',
  VISIT: 'channelVisit',
  MEETING: 'channelMeeting',
}

const METHOD_MAP = {
  BANK: 'methodBank',
  CASH: 'methodCash',
  TRANSFER: 'methodTransfer',
  CARD: 'methodCard',
}

const LEVEL_MAP = {
  HIGH: 'levelHigh',
  MEDIUM: 'levelMedium',
  LOW: 'levelLow',
}

const TIME_MAP = {
  TODAY: 'today',
  TOMORROW: 'tomorrow',
  THIS_WEEK: 'thisWeek',
}

const STAT_LABEL_MAP = {
  TOTAL_CUSTOMERS: 'statTotalCustomers',
  PROJECTED_REVENUE: 'statProjectedRevenue',
  AVG_SALES_CYCLE: 'statAvgSalesCycle',
  RETENTION_RATE: 'statRetentionRate',
}

const DATASET_MAP = {
  CUSTOMERS: 'customers',
  OPPORTUNITIES: 'pipeline',
  CONTRACTS: 'contracts',
  PAYMENTS: 'payments',
  LEADS: 'leads',
}

const VISIBILITY_MAP = {
  PRIVATE: 'reportVisibilityPrivate',
  DEPARTMENT: 'reportVisibilityDepartment',
  TENANT: 'reportVisibilityTenant',
}

const OWNER_ALIAS_MAP = {
  SYSTEMADMIN: 'ownerSystemAdmin',
  ADMIN: 'ownerAdmin',
  MANAGER: 'ownerManager',
  SALES: 'ownerSales',
  ANALYST: 'ownerAnalyst',
}

const translateMapped = (t, mapping, value) => {
  if (value === null || value === undefined || value === '') return '-'
  const key = mapping[normalize(value)]
  return key ? t(key) : String(value)
}

export const formatDateTime = (v) => (v ? String(v).replace('T', ' ').slice(0, 19) : '-')
export const mapToBars = (obj) => Object.entries(obj || {}).map(([label, value]) => ({ label, value: Number(value || 0) }))
export const formatMoney = (v) => {
  const n = Number(v || 0)
  const cnySymbol = '\u00A5'
  if (Math.abs(n) >= 1e6) return `${cnySymbol}${(n / 1e6).toFixed(2)}M`
  if (Math.abs(n) >= 1e3) return `${cnySymbol}${(n / 1e3).toFixed(1)}K`
  return `${cnySymbol}${n}`
}
export const formatMoneyByCurrency = (v, currency = 'CNY') => {
  const n = Number(v || 0)
  if (!Number.isFinite(n)) return `${currency} 0`
  const symbol = currency === 'CNY' ? '\u00A5' : currency === 'USD' ? '$' : `${currency} `
  if (Math.abs(n) >= 1e6) return `${symbol}${(n / 1e6).toFixed(2)}M`
  if (Math.abs(n) >= 1e3) return `${symbol}${(n / 1e3).toFixed(1)}K`
  return `${symbol}${n}`
}
export const translateRole = (t, value) => translateMapped(t, ROLE_MAP, value)
export const translateStatus = (t, value) => translateMapped(t, STATUS_MAP, value)
export const translateStage = (t, value) => translateMapped(t, STAGE_MAP, value)
export const translateChannel = (t, value) => translateMapped(t, CHANNEL_MAP, value)
export const translateMethod = (t, value) => translateMapped(t, METHOD_MAP, value)
export const translateTaskLevel = (t, value) => translateMapped(t, LEVEL_MAP, value)
export const translateTimeLabel = (t, value) => translateMapped(t, TIME_MAP, value)
export const translateStatLabel = (t, value) => translateMapped(t, STAT_LABEL_MAP, value)
export const translateDataset = (t, value) => translateMapped(t, DATASET_MAP, value)
export const translateVisibility = (t, value) => translateMapped(t, VISIBILITY_MAP, value)
export const translateOwnerAlias = (t, value) => translateMapped(t, OWNER_ALIAS_MAP, value)
export const formatStatValue = (t, label, value) => {
  const normalizedLabel = normalize(label)
  const raw = String(value || '')
  if (normalizedLabel === 'PROJECTED_REVENUE') {
    const n = Number(raw.replace(/[^\d.-]/g, ''))
    return Number.isFinite(n) && raw ? formatMoney(n) : raw.replace(/^CNY\s+/i, '\u00A5')
  }
  if (normalizedLabel === 'AVG_SALES_CYCLE') {
    const days = raw.match(/(\d+)/)?.[1]
    return days ? `${days} ${t('dayUnit')}` : raw
  }
  return raw.replace(/^CNY\s+/i, '\u00A5')
}
export const readFilters = () => {
  try {
    return JSON.parse(localStorage.getItem(FILTERS_KEY) || '{}')
  } catch {
    return {}
  }
}
export const parseHashPage = () => {
  const raw = (window.location.hash || '').replace(/^#\/?/, '')
  return raw || 'dashboard'
}

export async function api(path, options = {}, token, lang = 'en') {
  const body = options.body
  const isFormData = typeof FormData !== 'undefined' && body instanceof FormData
  const headers = { 'Accept-Language': lang, ...(options.headers || {}) }
  if (!isFormData && !headers['Content-Type']) headers['Content-Type'] = 'application/json'
  if (isFormData && headers['Content-Type']) delete headers['Content-Type']
  if (token && token !== 'COOKIE_SESSION') headers.Authorization = `Bearer ${token}`
  if (!headers['X-Tenant-Id']) {
    try {
      const auth = JSON.parse(localStorage.getItem('crm_auth') || 'null')
      const tenantFromAuth = String(auth?.tenantId || '').trim()
      if (tenantFromAuth) headers['X-Tenant-Id'] = tenantFromAuth
    } catch {
      // ignore localStorage parse errors
    }
    if (!headers['X-Tenant-Id']) {
      const lastTenant = String(localStorage.getItem('crm_last_tenant') || '').trim()
      if (lastTenant) headers['X-Tenant-Id'] = lastTenant
    }
    if (!headers['X-Tenant-Id']) headers['X-Tenant-Id'] = 'tenant_default'
  }
  const res = await fetch(`${API_BASE}${path}`, { ...options, credentials: 'include', headers, body })
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    const fallback = lang === 'zh' ? '\u8bf7\u6c42\u5931\u8d25' : 'Request failed'
    const err = new Error(body.message || fallback)
    err.code = body.code || ''
    err.details = body.details || {}
    err.requestId = body.requestId || ''
    err.status = res.status
    err.validationErrors = body.validationErrors || null
    throw err
  }
  if (res.status === 204) return null
  return res.json()
}





