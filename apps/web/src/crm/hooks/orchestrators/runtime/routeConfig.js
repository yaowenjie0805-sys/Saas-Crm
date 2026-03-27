export const PAGE_TO_PATH = {
  dashboard: '/dashboard',
  leads: '/leads',
  products: '/products',
  priceBooks: '/price-books',
  quotes: '/quotes',
  orders: '/orders',
  customers: '/customers',
  contacts: '/contacts',
  pipeline: '/opportunities',
  contracts: '/contracts',
  payments: '/payments',
  followUps: '/follow-ups',
  tasks: '/tasks',
  reports: '/reports',
  reportDesigner: '/reports/designer',
  approvals: '/approvals',
  audit: '/audit',
  permissions: '/admin/permissions',
  usersAdmin: '/admin/users',
  salesAutomation: '/admin/sales-automation',
  adminTenants: '/admin/tenants',
}

export const PATH_TO_PAGE = Object.entries(PAGE_TO_PATH).reduce((acc, [page, path]) => ({ ...acc, [path]: page }), {})

export const PAGE_CHUNK_PRELOADERS = {
  dashboard: () => import('../../../components/pages/DashboardPanel'),
  pipeline: () => import('../../../components/pages/PipelinePanel'),
  reports: () => import('../../../components/pages/DashboardPanel'),
  reportDesigner: () => import('../../../components/pages/ReportDesignerPanel'),
  customers: () => import('../../../components/pages/customers'),
  quotes: () => import('../../../components/pages/QuotesPanel'),
  orders: () => import('../../../components/pages/OrdersPanel'),
  approvals: () => import('../../../components/pages/ApprovalsPageContainer'),
}

export const PAGE_DOMAIN_PRELOADERS = {
  dashboard: () => Promise.resolve(),
  customers: () => Promise.resolve(),
  pipeline: () => Promise.resolve(),
  reports: () => Promise.resolve(),
  reportDesigner: () => Promise.resolve(),
  approvals: () => Promise.resolve(),
  quotes: () => Promise.resolve(),
  orders: () => Promise.resolve(),
  permissions: () => Promise.resolve(),
  usersAdmin: () => Promise.resolve(),
  salesAutomation: () => Promise.resolve(),
}

export const REFRESH_REASONS = new Set(['topbar_refresh', 'panel_action', 'workbench_jump', 'sidebar_nav'])

export const PAGE_DOMAIN_MAP = {
  dashboard: 'workbench',
  leads: 'customer',
  customers: 'customer',
  pipeline: 'customer',
  contacts: 'customer',
  followUps: 'customer',
  tasks: 'workbench',
  products: 'commerce',
  priceBooks: 'commerce',
  quotes: 'commerce',
  orders: 'commerce',
  contracts: 'commerce',
  payments: 'commerce',
  approvals: 'approval',
  reports: 'reporting',
  reportDesigner: 'reporting',
  audit: 'reporting',
  permissions: 'governance',
  usersAdmin: 'governance',
  salesAutomation: 'governance',
  adminTenants: 'governance',
}

const SUPPORTED_DATE_FORMATS = ['yyyy-MM-dd', 'dd/MM/yyyy', 'MM-dd-yyyy']
const LEGACY_DATE_FORMAT = 'YYYY-MM-DD'

export const normalizeDateFormat = (raw) => {
  const text = String(raw || '').trim()
  if (!text || text === LEGACY_DATE_FORMAT) return 'yyyy-MM-dd'
  return SUPPORTED_DATE_FORMATS.includes(text) ? text : 'yyyy-MM-dd'
}

export const parseDateByFormat = (raw, format) => {
  const text = String(raw || '').trim()
  if (!text) return null
  const normalizedFormat = normalizeDateFormat(format)
  const matcher = {
    'yyyy-MM-dd': /^(\d{4})-(\d{2})-(\d{2})$/,
    'dd/MM/yyyy': /^(\d{2})\/(\d{2})\/(\d{4})$/,
    'MM-dd-yyyy': /^(\d{2})-(\d{2})-(\d{4})$/,
  }[normalizedFormat]
  const matched = text.match(matcher)
  if (!matched) return null
  let y = 0
  let m = 0
  let d = 0
  if (normalizedFormat === 'yyyy-MM-dd') {
    y = Number(matched[1]); m = Number(matched[2]); d = Number(matched[3])
  } else if (normalizedFormat === 'dd/MM/yyyy') {
    d = Number(matched[1]); m = Number(matched[2]); y = Number(matched[3])
  } else {
    m = Number(matched[1]); d = Number(matched[2]); y = Number(matched[3])
  }
  const date = new Date(Date.UTC(y, m - 1, d))
  if (Number.isNaN(date.getTime())) return null
  if (date.getUTCFullYear() !== y || date.getUTCMonth() + 1 !== m || date.getUTCDate() !== d) return null
  return date
}

export const isValidDateStringByTenantFormat = (value, tenantDateFormat) => {
  const raw = String(value || '').trim()
  if (!raw) return true
  const tenantFormat = normalizeDateFormat(tenantDateFormat)
  if (parseDateByFormat(raw, tenantFormat)) return true
  return SUPPORTED_DATE_FORMATS.some((fmt) => parseDateByFormat(raw, fmt))
}
