import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { API_BASE, FILTERS_KEY, LANG_KEY, OIDC_STATE_KEY, CUSTOMER_STATUS_OPTIONS, CONTRACT_STATUS_OPTIONS, PAYMENT_METHOD_OPTIONS, PAYMENT_STATUS_OPTIONS, api } from './crm/shared'
import AuthShell from './crm/components/shell/AuthShell'
import AppShell from './crm/components/shell/AppShell'
import { AppProviders } from './crm/context/AppProviders'
import { tFactory } from './crm/i18n'
import { usePageDataPolicy } from './crm/hooks/usePageDataPolicy'
import { useNavPerf } from './crm/hooks/useNavPerf'
import { useActivePagePolling } from './crm/hooks/useActivePagePolling'
import { useLoaderOrchestrator } from './crm/hooks/useLoaderOrchestrator'
import { useAppNavigationModel } from './crm/hooks/useAppNavigationModel'
import { useAppShellBindings } from './crm/hooks/useAppShellBindings'
import { useAppMainContentModel } from './crm/hooks/useAppMainContentModel'
import { useAppPageActions } from './crm/hooks/useAppPageActions'
import { useAppViewBindings } from './crm/hooks/useAppViewBindings'
import { useCoreListDomainLoaders } from './crm/hooks/useCoreListDomainLoaders'
import { useGovernanceDomainLoaders } from './crm/hooks/useGovernanceDomainLoaders'
import { useApprovalDomainLoaders } from './crm/hooks/useApprovalDomainLoaders'
import { useReportingAuditDomainLoaders } from './crm/hooks/useReportingAuditDomainLoaders'
import { useWorkbenchDomainLoaders } from './crm/hooks/useWorkbenchDomainLoaders'
import { useCommerceDomainLoaders } from './crm/hooks/useCommerceDomainLoaders'
import { useLeadImportActions } from './crm/hooks/useLeadImportActions'
import { useAppCrudActions } from './crm/hooks/useAppCrudActions'
import { useLeadImportDomainLoaders } from './crm/hooks/useLeadImportDomainLoaders'
import { useAppAuthModel } from './crm/hooks/useAppAuthModel'
import { useAppStateModel } from './crm/hooks/useAppStateModel'
import './App.css'

const PAGE_TO_PATH = {
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

const PATH_TO_PAGE = Object.entries(PAGE_TO_PATH).reduce((acc, [page, path]) => ({ ...acc, [path]: page }), {})
const PAGE_CHUNK_PRELOADERS = {
  customers: () => import('./crm/components/pages/CustomersPanel'),
  quotes: () => import('./crm/components/pages/QuotesPanel'),
  orders: () => import('./crm/components/pages/OrdersPanel'),
  approvals: () => import('./crm/components/pages/ApprovalsPageContainer'),
}
const SUPPORTED_DATE_FORMATS = ['yyyy-MM-dd', 'dd/MM/yyyy', 'MM-dd-yyyy']
const LEGACY_DATE_FORMAT = 'YYYY-MM-DD'
const REFRESH_REASONS = new Set(['topbar_refresh', 'panel_action', 'workbench_jump', 'sidebar_nav'])
const PAGE_DOMAIN_MAP = {
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
const normalizeDateFormat = (raw) => {
  const text = String(raw || '').trim()
  if (!text || text === LEGACY_DATE_FORMAT) return 'yyyy-MM-dd'
  return SUPPORTED_DATE_FORMATS.includes(text) ? text : 'yyyy-MM-dd'
}
const parseDateByFormat = (raw, format) => {
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

function App() {
  const location = useLocation()
  const navigate = useNavigate()
  const perfEnabledByQuery = useMemo(() => {
    const params = new URLSearchParams(location.search || '')
    return params.get('perf') === '1'
  }, [location.search])
  const {
    lang,
    setLang,
    auth,
    setAuth,
    persisted,
    readPageSize,
  } = useAppStateModel()
  const t = useMemo(() => tFactory(lang), [lang])

  const [loading] = useState(false)
  const [error, setError] = useState('')
  const [loginError, setLoginError] = useState('')
  const [crudErrors, setCrudErrors] = useState({ lead: '', customer: '', opportunity: '', followUp: '', contact: '', contract: '', payment: '' })
  const [crudFieldErrors, setCrudFieldErrors] = useState({ lead: {}, customer: {}, opportunity: {}, followUp: {}, contact: {}, contract: {}, payment: {} })
  const [stats] = useState([])
  const [leads, setLeads] = useState([])
  const [customers, setCustomers] = useState([])
  const [tasks, setTasks] = useState([])
  const [opportunities, setOpportunities] = useState([])
  const [followUps, setFollowUps] = useState([])
  const [contacts, setContacts] = useState([])
  const [contracts, setContracts] = useState([])
  const [payments, setPayments] = useState([])
  const [auditLogs, setAuditLogs] = useState([])
  const [reports, setReports] = useState(null)
  const [workbenchToday, setWorkbenchToday] = useState(null)
  const [customerTimeline, setCustomerTimeline] = useState([])
  const [opportunityTimeline, setOpportunityTimeline] = useState([])
  const [permissionMatrix, setPermissionMatrix] = useState([])
  const [permissionConflicts, setPermissionConflicts] = useState([])
  const [permissionRole, setPermissionRole] = useState('SALES')
  const [permissionPreview, setPermissionPreview] = useState(null)
  const [pendingPack, setPendingPack] = useState('')
  const [exportJobs, setExportJobs] = useState([])
  const [exportJobsPage, setExportJobsPage] = useState(1)
  const [exportJobsTotalPages, setExportJobsTotalPages] = useState(1)
  const [exportJobsSize, setExportJobsSize] = useState(() => readPageSize('crm_page_size_audit_export_jobs', 8))
  const [autoRefreshJobs, setAutoRefreshJobs] = useState(true)
  const [exportStatusFilter, setExportStatusFilter] = useState('ALL')
  const [reportExportJobs, setReportExportJobs] = useState([])
  const [reportExportJobsPage, setReportExportJobsPage] = useState(1)
  const [reportExportJobsTotalPages, setReportExportJobsTotalPages] = useState(1)
  const [reportExportJobsSize, setReportExportJobsSize] = useState(() => readPageSize('crm_page_size_report_export_jobs', 8))
  const [designerTemplates, setDesignerTemplates] = useState([])
  const [designerRunResult, setDesignerRunResult] = useState(null)
  const [leadImportJob, setLeadImportJob] = useState(null)
  const [leadImportJobs, setLeadImportJobs] = useState([])
  const [leadImportStatusFilter, setLeadImportStatusFilter] = useState('ALL')
  const [leadImportPage, setLeadImportPage] = useState(1)
  const [leadImportTotalPages, setLeadImportTotalPages] = useState(1)
  const [leadImportSize, setLeadImportSize] = useState(() => readPageSize('crm_page_size_lead_import_jobs', 10))
  const [leadImportFailedRows, setLeadImportFailedRows] = useState([])
  const [leadImportMetrics, setLeadImportMetrics] = useState(null)
  const [leadImportExportJobs, setLeadImportExportJobs] = useState([])
  const [leadImportExportStatusFilter, setLeadImportExportStatusFilter] = useState('ALL')
  const [leadImportExportPage, setLeadImportExportPage] = useState(1)
  const [leadImportExportTotalPages, setLeadImportExportTotalPages] = useState(1)
  const [leadImportExportSize, setLeadImportExportSize] = useState(() => readPageSize('crm_page_size_lead_import_export_jobs', 10))
  const [leadAssignmentRules, setLeadAssignmentRules] = useState([])
  const [assignmentRuleForm, setAssignmentRuleForm] = useState({ id: '', name: '', enabled: true, membersText: 'sales:1' })
  const [automationRules, setAutomationRules] = useState([])
  const [automationRuleForm, setAutomationRuleForm] = useState({ id: '', name: '', triggerType: 'LEAD_CREATED', triggerExpr: '{}', actionType: 'CREATE_TASK', actionPayload: '{"title":"Follow up lead"}', enabled: true })
  const [autoRefreshReportJobs, setAutoRefreshReportJobs] = useState(true)
  const [reportExportStatusFilter, setReportExportStatusFilter] = useState('ALL')

  const [customerQ, setCustomerQ] = useState(persisted.customerQ || '')
  const [leadQ, setLeadQ] = useState(persisted.leadQ || '')
  const [leadStatus, setLeadStatus] = useState(persisted.leadStatus || '')
  const [customerStatus, setCustomerStatus] = useState(persisted.customerStatus || '')
  const [oppStage, setOppStage] = useState(persisted.oppStage || '')
  const [followCustomerId, setFollowCustomerId] = useState(persisted.followCustomerId || '')
  const [followQ, setFollowQ] = useState(persisted.followQ || '')
  const [auditUser, setAuditUser] = useState(persisted.auditUser || '')
  const [auditRole, setAuditRole] = useState(persisted.auditRole || '')
  const [auditAction, setAuditAction] = useState(persisted.auditAction || '')
  const [auditFrom, setAuditFrom] = useState(persisted.auditFrom || '')
  const [auditTo, setAuditTo] = useState(persisted.auditTo || '')
  const [reportOwner, setReportOwner] = useState(persisted.reportOwner || '')
  const [reportDepartment, setReportDepartment] = useState(persisted.reportDepartment || '')
  const [reportTimezone, setReportTimezone] = useState(persisted.reportTimezone || (Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Shanghai'))
  const [reportCurrency, setReportCurrency] = useState(persisted.reportCurrency || 'CNY')
  const [quoteOpportunityFilter, setQuoteOpportunityFilter] = useState('')
  const [orderOpportunityFilter, setOrderOpportunityFilter] = useState('')
  const [quotePrefill, setQuotePrefill] = useState(null)

  const [loginForm, setLoginForm] = useState(() => ({
    tenantId: localStorage.getItem('crm_last_tenant') || 'tenant_default',
    username: '',
    password: '',
    mfaCode: '',
  }))
  const [mfaChallengeId, setMfaChallengeId] = useState('')
  const [ssoConfig, setSsoConfig] = useState({ enabled: false, providerName: '', mode: 'mock' })
  const [ssoForm, setSsoForm] = useState({ username: 'sso_user', code: 'SSO-ACCESS', displayName: '' })
  const [adminUsers, setAdminUsers] = useState([])
  const [oidcAuthorizing, setOidcAuthorizing] = useState(false)
  const [sessionBootstrapping, setSessionBootstrapping] = useState(true)
  const [formErrors, setFormErrors] = useState({ login: {}, register: {}, sso: {} })
  const [auditRangeError, setAuditRangeError] = useState('')
  const [activePage, setActivePage] = useState('dashboard')
  const [customerForm, setCustomerForm] = useState({ id: '', name: '', owner: '', status: '', tag: '', value: '' })
  const [leadForm, setLeadForm] = useState({ id: '', name: '', company: '', phone: '', email: '', status: 'NEW', owner: '', source: '' })
  const [opportunityForm, setOpportunityForm] = useState({ id: '', stage: '', count: '', amount: '', progress: '', owner: '' })
  const [followUpForm, setFollowUpForm] = useState({ id: '', customerId: '', summary: '', channel: '', result: '', nextActionDate: '' })
  const [contactForm, setContactForm] = useState({ id: '', customerId: '', name: '', title: '', phone: '', email: '' })
  const [contractForm, setContractForm] = useState({ id: '', customerId: '', contractNo: '', title: '', amount: '', status: '', signDate: '' })
  const [paymentForm, setPaymentForm] = useState({ id: '', contractId: '', amount: '', receivedDate: '', method: '', status: '', remark: '' })
  const [approvalTemplateForm, setApprovalTemplateForm] = useState({ bizType: 'CONTRACT', name: '', approverRoles: 'MANAGER,ADMIN' })
  const [approvalInstanceForm, setApprovalInstanceForm] = useState({ bizType: 'CONTRACT', bizId: '', amount: '' })
  const [approvalTemplates, setApprovalTemplates] = useState([])
  const [approvalStats, setApprovalStats] = useState(null)
  const [approvalTasks, setApprovalTasks] = useState([])
  const [approvalInstances, setApprovalInstances] = useState([])
  const [approvalDetail, setApprovalDetail] = useState(null)
  const [approvalTemplateVersions, setApprovalTemplateVersions] = useState([])
  const [approvalVersionTemplateId, setApprovalVersionTemplateId] = useState('')
  const [approvalTaskStatus, setApprovalTaskStatus] = useState('PENDING')
  const [approvalOverdueOnly, setApprovalOverdueOnly] = useState(false)
  const [approvalEscalatedOnly, setApprovalEscalatedOnly] = useState(false)
  const [approvalActionComment, setApprovalActionComment] = useState('')
  const [approvalTransferTo, setApprovalTransferTo] = useState('')
  const [approvalActionResult, setApprovalActionResult] = useState(null)
  const [approvalPendingTaskIds, setApprovalPendingTaskIds] = useState({})
  const [notificationJobs, setNotificationJobs] = useState([])
  const [notificationStatusFilter, setNotificationStatusFilter] = useState('ALL')
  const [notificationPage, setNotificationPage] = useState(1)
  const [notificationTotalPages, setNotificationTotalPages] = useState(1)
  const [notificationSize, setNotificationSize] = useState(() => readPageSize('crm_page_size_notification_jobs', 10))
  const [selectedNotificationJobs, setSelectedNotificationJobs] = useState([])
  const [tenantForm, setTenantForm] = useState({
    name: '',
    quotaUsers: '100',
    timezone: 'Asia/Shanghai',
    currency: 'CNY',
    status: 'ACTIVE',
    dateFormat: 'yyyy-MM-dd',
    marketProfile: 'CN',
    taxRule: 'VAT_CN',
    approvalMode: 'STRICT',
    channels: '["WECOM","DINGTALK"]',
    dataResidency: 'CN',
    maskLevel: 'STANDARD',
  })
  const [lastCreatedTenant, setLastCreatedTenant] = useState(null)
  const [tenantRows, setTenantRows] = useState([])
  const [inviteForm, setInviteForm] = useState({ username: '', role: 'SALES', ownerScope: '', department: 'DEFAULT', dataScope: 'SELF' })
  const [inviteResult, setInviteResult] = useState(null)
  const [reportDesignerForm, setReportDesignerForm] = useState({ name: '', dataset: 'LEADS', visibility: 'PRIVATE', limit: '100' })

  const [customerPage, setCustomerPage] = useState(1)
  const [leadPage, setLeadPage] = useState(1)
  const [leadTotalPages, setLeadTotalPages] = useState(1)
  const [leadSize, setLeadSize] = useState(() => readPageSize('crm_page_size_leads'))
  const [customerTotalPages, setCustomerTotalPages] = useState(1)
  const [customerSize, setCustomerSize] = useState(() => readPageSize('crm_page_size_customers'))
  const [opportunityPage, setOpportunityPage] = useState(1)
  const [opportunityTotalPages, setOpportunityTotalPages] = useState(1)
  const [opportunitySize, setOpportunitySize] = useState(() => readPageSize('crm_page_size_opportunities'))
  const [contactQ, setContactQ] = useState(persisted.contactQ || '')
  const [contactPage, setContactPage] = useState(1)
  const [contactTotalPages, setContactTotalPages] = useState(1)
  const [contactSize, setContactSize] = useState(() => readPageSize('crm_page_size_contacts'))
  const [contractQ, setContractQ] = useState(persisted.contractQ || '')
  const [contractStatus, setContractStatus] = useState(persisted.contractStatus || '')
  const [contractPage, setContractPage] = useState(1)
  const [contractTotalPages, setContractTotalPages] = useState(1)
  const [contractSize, setContractSize] = useState(() => readPageSize('crm_page_size_contracts'))
  const [paymentStatus, setPaymentStatus] = useState(persisted.paymentStatus || '')
  const [paymentPage, setPaymentPage] = useState(1)
  const [paymentTotalPages, setPaymentTotalPages] = useState(1)
  const [paymentSize, setPaymentSize] = useState(() => readPageSize('crm_page_size_payments'))
  const seqRef = useRef({ leads: 0, customers: 0, opportunities: 0, contacts: 0, contracts: 0, payments: 0 })
  const logoutGuardRef = useRef(false)
  const suppressLoginErrorUntilRef = useRef(0)
  const { beginPageRequest, canSkipFetch, isInFlight, markInFlight, clearInFlight, markFetched, abortAll } = usePageDataPolicy(10000)

  const {
    markNavStart,
    markNavEnd,
    markAbort,
    markFetchLatency,
    markCacheDecision,
    markChunkPreloadHit,
    markDuplicateFetchBlocked,
    markWorkbenchJumpDecision,
    markWorkbenchActionResult,
    markPollingActiveInstances,
    markLoginErrorLeakBlocked,
    markLoaderFallbackUsed,
    markAuthChannelMisroute,
    markRefreshSourceAnomaly,
    markCustomer360ActionResult,
    markCustomer360ModuleRefreshLatency,
    markCustomer360JumpHit,
    markCustomer360ModuleCacheHit,
    markCustomer360PrefetchHit,
    markCustomer360PrefetchAbort,
    markCustomer360PrefetchModules,
    getMetrics,
  } = useNavPerf('[perf]')
  const [perfMetrics, setPerfMetrics] = useState(() => getMetrics())
  const [lastPerfSnapshotAt, setLastPerfSnapshotAt] = useState('')
  const [currentLoaderKey, setCurrentLoaderKey] = useState('')
  const [lastRefreshReason, setLastRefreshReason] = useState('default')
  const [currentPageSignature, setCurrentPageSignature] = useState('')
  const [currentSignatureHit, setCurrentSignatureHit] = useState(false)
  const [recentWorkbenchJump, setRecentWorkbenchJump] = useState({ targetPage: '', signature: '', reason: 'workbench_jump', hit: false, at: '' })
  const [domainLoadSource, setDomainLoadSource] = useState({
    customer: { source: '', at: '' },
    commerce: { source: '', at: '' },
    governance: { source: '', at: '' },
    approval: { source: '', at: '' },
    reporting: { source: '', at: '' },
    workbench: { source: '', at: '' },
  })
  const loadReasonRef = useRef('default')
  const workbenchJumpRef = useRef({ page: '', signature: '' })
  const markWorkbenchJumpMeta = useCallback((meta) => {
    setRecentWorkbenchJump({
      targetPage: String(meta?.targetPage || ''),
      signature: String(meta?.signature || ''),
      reason: String(meta?.reason || 'workbench_jump'),
      hit: !!meta?.hit,
      at: new Date().toISOString(),
    })
  }, [])
  const markDomainLoadSource = useCallback(({ pageKey, reason, event }) => {
    const domain = PAGE_DOMAIN_MAP[pageKey]
    if (!domain) return
    const shortReason = String(reason || 'default')
    const source = `${event}:${pageKey}:${shortReason}`
    const at = new Date().toISOString()
    setDomainLoadSource((prev) => {
      const current = prev[domain]
      if (current?.source === source) return prev
      return {
        ...prev,
        [domain]: { source, at },
      }
    })
  }, [])

  const hasAuthToken = !!String(auth?.token || '').trim()
  const role = auth?.role || ''
  const canWrite = ['ADMIN', 'MANAGER', 'SALES'].includes(role)
  const canDeleteCustomer = ['ADMIN', 'MANAGER'].includes(role)
  const canDeleteOpportunity = ['ADMIN', 'MANAGER'].includes(role)
  const canViewAudit = ['ADMIN', 'MANAGER', 'ANALYST'].includes(role)
  const canViewReports = ['ADMIN', 'MANAGER', 'ANALYST'].includes(role)
  const canViewOpsMetrics = ['ADMIN', 'MANAGER', 'ANALYST'].includes(role)
  const canManagePermissions = role === 'ADMIN'
  const canManageUsers = role === 'ADMIN'
  const canManageSalesAutomation = ['ADMIN', 'MANAGER'].includes(role)

  const { navGroups, currentPageLabel } = useAppNavigationModel({
    t,
    permissions: {
      canViewAudit,
      canManagePermissions,
      canManageUsers,
      canManageSalesAutomation,
    },
    activePage,
    setActivePage,
    locationPathname: location.pathname,
    authToken: auth?.token,
    navigate,
    pathToPage: PATH_TO_PAGE,
    pageToPath: PAGE_TO_PATH,
  })

  const formatValidation = (err) => { const ve = err?.validationErrors; if (!ve || typeof ve !== 'object') return err.message; const first = Object.entries(ve)[0]; return first ? `${first[0]}: ${first[1]}` : err.message }
  const formatErrorMessage = useCallback((err) => {
    const msg = err?.status === 400 ? formatValidation(err) : err?.message
    return err?.requestId ? `${msg} [${err.requestId}]` : msg
  }, [])
  const {
    saveAuth,
    handleLoginError,
    handleError,
    performLogout,
  } = useAppAuthModel({
    auth,
    setAuth,
    setError,
    setLoginError,
    setCrudErrors,
    setCrudFieldErrors,
    setCurrentLoaderKey,
    setCurrentPageSignature,
    setCurrentSignatureHit,
    setRecentWorkbenchJump,
    setDomainLoadSource,
    setLastRefreshReason,
    abortAll,
    loadReasonRef,
    workbenchJumpRef,
    navigate,
    pathname: location.pathname,
    logoutGuardRef,
    suppressLoginErrorUntilRef,
    markLoginErrorLeakBlocked,
    markAuthChannelMisroute,
    t,
    formatErrorMessage,
  })
  const setCrudError = (key, msg) => setCrudErrors((prev) => ({ ...prev, [key]: msg || '' }))
  const setCrudFieldError = (key, fields) => setCrudFieldErrors((prev) => ({ ...prev, [key]: fields || {} }))
  const pickFieldErrors = (err, allowed) => {
    const ve = err?.validationErrors
    if (!ve || typeof ve !== 'object') return {}
    const out = {}
    allowed.forEach((f) => { if (ve[f]) out[f] = ve[f] })
    return out
  }
  const hasInvalidAuditRange = () => !!(auditFrom && auditTo && auditFrom > auditTo)

  const {
    loaders: coreListLoaders,
    paginationActions: corePaginationActions,
  } = useCoreListDomainLoaders({
    authToken: auth?.token,
    lang,
    seqRef,
    beginPageRequest,
    handleError,
    leadQ,
    leadStatus,
    leadPage,
    leadSize,
    setLeads,
    setLeadPage,
    setLeadTotalPages,
    setLeadSize,
    customerQ,
    customerStatus,
    customerPage,
    customerSize,
    setCustomers,
    setCustomerPage,
    setCustomerTotalPages,
    setCustomerSize,
    oppStage,
    opportunityPage,
    opportunitySize,
    setOpportunities,
    setOpportunityPage,
    setOpportunityTotalPages,
    setOpportunitySize,
    contactQ,
    contactPage,
    contactSize,
    setContacts,
    setContactPage,
    setContactTotalPages,
    setContactSize,
    contractQ,
    contractStatus,
    contractPage,
    contractSize,
    setContracts,
    setContractPage,
    setContractTotalPages,
    setContractSize,
    paymentStatus,
    paymentPage,
    paymentSize,
    setPayments,
    setPaymentPage,
    setPaymentTotalPages,
    setPaymentSize,
  })
  const commerceDomain = useCommerceDomainLoaders({
    authToken: auth?.token,
    lang,
    quoteOpportunityFilter,
    orderOpportunityFilter,
  })
  const {
    loadLeadImportJobs,
    loadLeadImportFailedRows,
    loadLeadImportMetrics,
    loadLeadImportExportJobs,
  } = useLeadImportDomainLoaders({
    authToken: auth?.token,
    lang,
    canViewOpsMetrics,
    leadImportStatusFilter,
    leadImportPage,
    leadImportSize,
    leadImportJob,
    setLeadImportJobs,
    setLeadImportPage,
    setLeadImportTotalPages,
    setLeadImportJob,
    setLeadImportFailedRows,
    setLeadImportMetrics,
    leadImportExportStatusFilter,
    leadImportExportPage,
    leadImportExportSize,
    setLeadImportExportJobs,
    setLeadImportExportPage,
    setLeadImportExportTotalPages,
  })
  const {
    loadLeads,
    loadCustomers,
    loadOpportunities,
    loadContacts,
    loadContracts,
    loadPayments,
  } = coreListLoaders
  const {
    onLeadPageChange,
    onCustomerPageChange,
    onOpportunityPageChange,
    onContactPageChange,
    onContractPageChange,
    onPaymentPageChange,
    onLeadSizeChange,
    onCustomerSizeChange,
    onOpportunitySizeChange,
    onContactSizeChange,
    onContractSizeChange,
    onPaymentSizeChange,
  } = corePaginationActions
  const loadTasks = async () => { const controller = beginPageRequest('tasks'); const d = await api('/tasks/search?page=1&size=10', { signal: controller.signal }, auth.token, lang); setTasks(d.items || []) }
  const loadFollowUps = async () => { const q = new URLSearchParams({ customerId: followCustomerId, q: followQ, page: '1', size: '8' }); const controller = beginPageRequest('followUps'); const d = await api('/follow-ups/search?' + q, { signal: controller.signal }, auth.token, lang); setFollowUps(d.items || []) }
  const loadSsoConfig = async () => { try { const d = await api('/auth/sso/config', {}, null, lang); setSsoConfig(d || { enabled: false, providerName: '', mode: 'mock' }) } catch { setSsoConfig({ enabled: false, providerName: '', mode: 'mock' }) } }
  const {
    loadPermissionMatrix,
    loadPermissionConflicts,
    loadLeadAssignmentRules,
    loadAutomationRulesV1,
    loadAdminUsers,
    loadTenants,
  } = useGovernanceDomainLoaders({
    canManageUsers,
    canManageSalesAutomation,
    authToken: auth?.token,
    lang,
    setPermissionMatrix,
    setPermissionConflicts,
    setLeadAssignmentRules,
    setAutomationRules,
    setAdminUsers,
    setTenantRows,
    normalizeDateFormat,
  })
  const {
    loadApprovalTemplates,
    loadApprovalStats,
    loadApprovalTasks,
    loadApprovalInstances,
    loadApprovalDetail,
    loadApprovalTemplateVersions,
    loadNotificationJobs,
  } = useApprovalDomainLoaders({
    authToken: auth?.token,
    lang,
    approvalTaskStatus,
    approvalOverdueOnly,
    approvalEscalatedOnly,
    notificationStatusFilter,
    notificationPage,
    notificationSize,
    setApprovalTemplates,
    setApprovalStats,
    setApprovalTasks,
    setApprovalInstances,
    setApprovalDetail,
    setApprovalTemplateVersions,
    setApprovalVersionTemplateId,
    setNotificationJobs,
    setNotificationPage,
    setNotificationTotalPages,
    setSelectedNotificationJobs,
  })
  const {
    loadAudit,
    loadReports,
    loadExportJobs,
    loadReportExportJobs,
    loadDesignerTemplates,
  } = useReportingAuditDomainLoaders({
    canViewAudit,
    canViewReports,
    authToken: auth?.token,
    lang,
    auditUser,
    auditRole,
    auditAction,
    auditFrom,
    auditTo,
    reportOwner,
    reportDepartment,
    reportTimezone,
    reportCurrency,
    exportStatusFilter,
    exportJobsPage,
    exportJobsSize,
    reportExportStatusFilter,
    reportExportJobsPage,
    reportExportJobsSize,
    hasInvalidAuditRange,
    setAuditRangeError,
    t,
    setAuditLogs,
    setReports,
    setExportJobs,
    setExportJobsPage,
    setExportJobsTotalPages,
    setReportExportJobs,
    setReportExportJobsPage,
    setReportExportJobsTotalPages,
    setDesignerTemplates,
  })
  const {
    loadWorkbenchToday,
    loadCustomerTimeline,
    loadOpportunityTimeline,
  } = useWorkbenchDomainLoaders({
    authToken: auth?.token,
    lang,
    auditFrom,
    auditTo,
    reportOwner,
    reportDepartment,
    reportTimezone,
    setWorkbenchToday,
    setCustomerTimeline,
    setOpportunityTimeline,
  })

  useEffect(() => localStorage.setItem(LANG_KEY, lang), [lang])
  useEffect(() => () => abortAll(), [abortAll])
  useEffect(() => {
    let disposed = false
    const run = async () => {
      if (auth?.token) {
        if (!disposed) setSessionBootstrapping(false)
        return
      }
      if (location.pathname === '/activate') {
        if (!disposed) setSessionBootstrapping(false)
        return
      }
      try {
        const restored = await api('/v1/auth/session', {}, null, lang)
        if (!disposed) {
          saveAuth(restored)
        }
      } catch {
        try {
          const restored = await api('/auth/session', {}, null, lang)
          if (!disposed) {
            saveAuth(restored)
          }
        } catch {
          if (!disposed) {
            saveAuth(null)
          }
        }
      } finally {
        if (!disposed) setSessionBootstrapping(false)
      }
    }
    run()
    return () => {
      disposed = true
    }
  }, [auth?.token, location.pathname, lang, saveAuth])
  useEffect(() => {
    if (sessionBootstrapping) return
    const isAuthRoute = location.pathname === '/login' || location.pathname === '/activate'
    if (!auth?.token && !isAuthRoute) {
      navigate('/login', { replace: true })
      return
    }
    if (auth?.token && location.pathname === '/') {
      navigate(PAGE_TO_PATH.dashboard, { replace: true })
    }
  }, [auth?.token, location.pathname, navigate, sessionBootstrapping])
  /* eslint-disable react-hooks/exhaustive-deps */
  useEffect(() => { if (!auth?.token) loadSsoConfig() }, [lang, auth?.token])
  useEffect(() => {
    if (auth?.token) return
    if (!(ssoConfig?.enabled && ssoConfig?.mode === 'oidc')) return
    const params = new URLSearchParams(window.location.search)
    const code = params.get('code')
    const state = params.get('state')
    if (!code) return
    const expected = localStorage.getItem(OIDC_STATE_KEY)
    if (expected && state && expected !== state) { setLoginError(t('invalidOidcState')); return }
    localStorage.removeItem(OIDC_STATE_KEY)
    ;(async () => {
      try {
        setOidcAuthorizing(true)
        const tenantId = loginForm.tenantId.trim() || localStorage.getItem('crm_last_tenant') || 'tenant_default'
        const d = await api('/auth/sso/login', { method: 'POST', headers: { 'X-Tenant-Id': tenantId }, body: JSON.stringify({ code }) }, null, lang)
        saveAuth(d)
        localStorage.setItem('crm_last_tenant', tenantId)
        const url = new URL(window.location.href)
        url.searchParams.delete('code')
        url.searchParams.delete('state')
        window.history.replaceState({}, document.title, url.toString())
      } catch (err) { handleLoginError(err) } finally { setOidcAuthorizing(false) }
    })()
  }, [auth?.token, ssoConfig?.enabled, ssoConfig?.mode, lang, handleLoginError, saveAuth, loginForm.tenantId])

  useEffect(() => localStorage.setItem(FILTERS_KEY, JSON.stringify({ leadQ, leadStatus, customerQ, customerStatus, oppStage, followCustomerId, followQ, contactQ, contractQ, contractStatus, paymentStatus, auditUser, auditRole, auditAction, auditFrom, auditTo, reportOwner, reportDepartment, reportTimezone, reportCurrency })), [leadQ, leadStatus, customerQ, customerStatus, oppStage, followCustomerId, followQ, contactQ, contractQ, contractStatus, paymentStatus, auditUser, auditRole, auditAction, auditFrom, auditTo, reportOwner, reportDepartment, reportTimezone, reportCurrency])
  useEffect(() => {
    localStorage.setItem('crm_page_size_customers', String(customerSize))
    localStorage.setItem('crm_page_size_leads', String(leadSize))
    localStorage.setItem('crm_page_size_opportunities', String(opportunitySize))
    localStorage.setItem('crm_page_size_contacts', String(contactSize))
    localStorage.setItem('crm_page_size_contracts', String(contractSize))
    localStorage.setItem('crm_page_size_payments', String(paymentSize))
    localStorage.setItem('crm_page_size_notification_jobs', String(notificationSize))
    localStorage.setItem('crm_page_size_lead_import_export_jobs', String(leadImportExportSize))
  }, [leadSize, customerSize, opportunitySize, contactSize, contractSize, paymentSize, notificationSize, leadImportExportSize])
  const pageSignature = useCallback((pageKey, filters, pageNo, pageSize) => {
    return JSON.stringify({
      pageKey,
      filters,
      pageNo: Number(pageNo || 1),
      pageSize: Number(pageSize || 0),
    })
  }, [])
  const commonPageLoaders = useMemo(() => ({
    leads: {
      signature: pageSignature('leads', {
        q: leadQ,
        status: leadStatus,
        importStatus: leadImportStatusFilter,
        importPage: leadImportPage,
        importSize: leadImportSize,
        importExportStatus: leadImportExportStatusFilter,
        importExportPage: leadImportExportPage,
        importExportSize: leadImportExportSize,
        importJobId: leadImportJob?.id || '',
      }, leadPage, leadSize),
      delay: 220,
      run: async (controller) => {
        const options = { signal: controller?.signal }
        await loadLeads(leadPage, leadSize, options)
        await loadLeadImportJobs(leadImportPage, leadImportSize, options)
        if (leadImportJob?.id) {
          await loadLeadImportFailedRows(leadImportJob.id, options)
          await loadLeadImportExportJobs(leadImportJob.id, leadImportExportPage, leadImportExportSize, options)
        }
        if (canViewOpsMetrics) await loadLeadImportMetrics(options)
      },
    },
    customers: {
      signature: pageSignature('customers', { q: customerQ, status: customerStatus }, customerPage, customerSize),
      delay: 220,
      run: (controller) => loadCustomers(customerPage, customerSize, { signal: controller?.signal }),
    },
    pipeline: {
      signature: pageSignature('pipeline', { stage: oppStage }, opportunityPage, opportunitySize),
      delay: 220,
      run: () => loadOpportunities(opportunityPage, opportunitySize),
    },
    contacts: {
      signature: pageSignature('contacts', { q: contactQ }, contactPage, contactSize),
      delay: 220,
      run: () => loadContacts(contactPage, contactSize),
    },
    contracts: {
      signature: pageSignature('contracts', { q: contractQ, status: contractStatus }, contractPage, contractSize),
      delay: 220,
      run: () => loadContracts(contractPage, contractSize),
    },
    payments: {
      signature: pageSignature('payments', { status: paymentStatus }, paymentPage, paymentSize),
      delay: 220,
      run: () => loadPayments(paymentPage, paymentSize),
    },
    priceBooks: {
      signature: pageSignature('priceBooks', {
        status: commerceDomain.signatures.priceBooks.filters.status,
        name: commerceDomain.signatures.priceBooks.filters.name,
      }, commerceDomain.signatures.priceBooks.pageNo, commerceDomain.signatures.priceBooks.pageSize),
      delay: 180,
      run: (controller) => commerceDomain.loaders.loadPriceBooks({ signal: controller?.signal }),
    },
    products: {
      signature: pageSignature('products', {
        status: commerceDomain.signatures.products.filters.status,
        code: commerceDomain.signatures.products.filters.code,
        name: commerceDomain.signatures.products.filters.name,
        category: commerceDomain.signatures.products.filters.category,
      }, commerceDomain.signatures.products.pageNo, commerceDomain.signatures.products.pageSize),
      delay: 180,
      run: (controller) => commerceDomain.loaders.loadProducts({ signal: controller?.signal }),
    },
    quotes: {
      signature: pageSignature('quotes', {
        status: commerceDomain.signatures.quotes.filters.status,
        owner: commerceDomain.signatures.quotes.filters.owner,
        opportunityId: commerceDomain.signatures.quotes.filters.opportunityId || quoteOpportunityFilter,
      }, commerceDomain.signatures.quotes.pageNo, commerceDomain.signatures.quotes.pageSize),
      delay: 180,
      run: (controller) => commerceDomain.loaders.loadQuotes({ signal: controller?.signal }),
    },
    orders: {
      signature: pageSignature('orders', {
        status: commerceDomain.signatures.orders.filters.status,
        owner: commerceDomain.signatures.orders.filters.owner,
        opportunityId: commerceDomain.signatures.orders.filters.opportunityId || orderOpportunityFilter,
      }, commerceDomain.signatures.orders.pageNo, commerceDomain.signatures.orders.pageSize),
      delay: 180,
      run: (controller) => commerceDomain.loaders.loadOrders({ signal: controller?.signal }),
    },
    followUps: {
      signature: pageSignature('followUps', { customerId: followCustomerId, q: followQ }, 1, 0),
      delay: 250,
      run: () => loadFollowUps(),
    },
    tasks: {
      signature: pageSignature('tasks', {}, 1, 0),
      delay: 0,
      run: () => loadTasks(),
    },
  }), [pageSignature, leadQ, leadStatus, leadPage, leadSize, leadImportStatusFilter, leadImportPage, leadImportSize, leadImportExportStatusFilter, leadImportExportPage, leadImportExportSize, leadImportJob?.id, canViewOpsMetrics, customerQ, customerStatus, customerPage, customerSize, oppStage, opportunityPage, opportunitySize, contactQ, contactPage, contactSize, contractQ, contractStatus, contractPage, contractSize, paymentStatus, paymentPage, paymentSize, quoteOpportunityFilter, orderOpportunityFilter, followCustomerId, followQ, loadLeadImportExportJobs, loadLeadImportMetrics, commerceDomain])
  const keyPageLoaders = useMemo(() => ({
    dashboard: {
      signature: pageSignature('dashboard', {
        from: auditFrom,
        to: auditTo,
        owner: reportOwner,
        department: reportDepartment,
        timezone: reportTimezone,
        role: auditRole,
        currency: reportCurrency,
        canViewReports,
        reportExportStatusFilter,
        reportExportJobsPage,
        reportExportJobsSize,
      }, 1, 0),
      canRun: () => true,
      run: () => Promise.all([
        loadTasks(),
        loadWorkbenchToday(),
        canViewReports ? loadReports() : Promise.resolve(null),
        canViewReports ? loadReportExportJobs(reportExportJobsPage, reportExportJobsSize) : Promise.resolve(null),
      ]),
    },
    reports: {
      signature: pageSignature('reports', {
        from: auditFrom,
        to: auditTo,
        owner: reportOwner,
        department: reportDepartment,
        timezone: reportTimezone,
        role: auditRole,
        currency: reportCurrency,
        canViewReports,
        reportExportStatusFilter,
        reportExportJobsPage,
        reportExportJobsSize,
      }, 1, 0),
      canRun: () => canViewReports,
      run: () => Promise.all([
        loadReports(),
        loadReportExportJobs(reportExportJobsPage, reportExportJobsSize),
      ]),
    },
    audit: {
      signature: pageSignature('audit', {
        user: auditUser,
        role: auditRole,
        action: auditAction,
        from: auditFrom,
        to: auditTo,
        exportStatusFilter,
        exportJobsPage,
        exportJobsSize,
      }, 1, 0),
      canRun: () => canViewAudit,
      run: () => Promise.all([
        loadAudit(),
        loadExportJobs(exportJobsPage, exportJobsSize),
      ]),
    },
    permissions: {
      signature: pageSignature('permissions', {}, 1, 0),
      canRun: () => true,
      run: () => Promise.all([loadPermissionMatrix(), loadPermissionConflicts()]),
    },
    usersAdmin: {
      signature: pageSignature('usersAdmin', {}, 1, 0),
      canRun: () => canManageUsers,
      run: () => Promise.all([loadAdminUsers(), loadTenants()]),
    },
    salesAutomation: {
      signature: pageSignature('salesAutomation', {}, 1, 0),
      canRun: () => canManageSalesAutomation,
      run: () => Promise.all([loadLeadAssignmentRules(), loadAutomationRulesV1()]),
    },
    adminTenants: {
      signature: pageSignature('adminTenants', {}, 1, 0),
      canRun: () => canManageUsers,
      run: () => loadTenants(),
    },
    approvals: {
      signature: pageSignature('approvals', {
        taskStatus: approvalTaskStatus,
        overdueOnly: approvalOverdueOnly,
        escalatedOnly: approvalEscalatedOnly,
        notificationStatusFilter,
        notificationPage,
        notificationSize,
      }, 1, 0),
      canRun: () => true,
      run: () => Promise.all([
        loadApprovalTemplates(),
        loadApprovalStats(),
        loadApprovalInstances(),
        loadApprovalTasks(),
        loadNotificationJobs(notificationPage, notificationSize),
      ]),
    },
    reportDesigner: {
      signature: pageSignature('reportDesigner', {}, 1, 0),
      canRun: () => true,
      run: () => loadDesignerTemplates(),
    },
  }), [pageSignature, auditFrom, auditTo, reportOwner, reportDepartment, reportTimezone, auditRole, reportCurrency, canViewReports, reportExportStatusFilter, reportExportJobsPage, reportExportJobsSize, canViewAudit, auditUser, auditAction, exportStatusFilter, exportJobsPage, exportJobsSize, canManageUsers, canManageSalesAutomation, approvalTaskStatus, approvalOverdueOnly, approvalEscalatedOnly, notificationStatusFilter, notificationPage, notificationSize])
  const { refreshPage } = useLoaderOrchestrator({
    authToken: hasAuthToken ? auth.token : null,
    activePage,
    commonPageLoaders,
    keyPageLoaders,
    loadReasonRef,
    beginPageRequest,
    canSkipFetch,
    isInFlight,
    markInFlight,
    clearInFlight,
    markCacheDecision,
    markDuplicateFetchBlocked,
    markWorkbenchJumpDecision,
    markFetched,
    markFetchLatency,
    markAbort,
    markLoaderFallbackUsed,
    handleError,
    setLastRefreshReason,
    setCurrentLoaderKey,
    setCurrentPageSignature,
    setCurrentSignatureHit,
    refreshReasons: REFRESH_REASONS,
    onLoaderLifecycle: markDomainLoadSource,
    markRefreshSourceAnomaly,
  })
  useEffect(() => {
    if (!auth?.token) {
      setError('')
    }
  }, [auth?.token])
  useEffect(() => {
    if (auth?.token) setLoginError('')
  }, [auth?.token])
  useEffect(() => {
    if (location.pathname === '/login' || location.pathname === '/activate') {
      setError('')
      if (!auth?.token) setLoginError('')
    }
  }, [auth?.token, location.pathname])
  useEffect(() => { setNotificationPage(1) }, [notificationStatusFilter])
  useEffect(() => { setNotificationPage(1) }, [notificationSize])
  useEffect(() => { setLeadImportPage(1) }, [leadImportStatusFilter, leadImportSize])
  useEffect(() => { setExportJobsPage(1) }, [exportStatusFilter, exportJobsSize])
  useEffect(() => { setReportExportJobsPage(1) }, [reportExportStatusFilter, reportExportJobsSize])
  useEffect(() => { setLeadImportExportPage(1) }, [leadImportExportStatusFilter, leadImportExportSize, leadImportJob?.id])
  /* eslint-enable react-hooks/exhaustive-deps */
  const activePollers = useMemo(() => ([
    {
      id: 'audit-export-jobs',
      intervalMs: 1800,
      canRun: () => !!auth?.token && activePage === 'audit' && autoRefreshJobs && exportJobs.some((j) => ['PENDING', 'RUNNING'].includes(j.status)),
      run: async (signal) => {
        if (signal.aborted) return
        await refreshPage('audit', 'panel_action')
      },
    },
    {
      id: 'report-export-jobs',
      intervalMs: 1800,
      canRun: () => !!auth?.token && ['dashboard', 'reports'].includes(activePage) && autoRefreshReportJobs && reportExportJobs.some((j) => ['PENDING', 'RUNNING'].includes(j.status)),
      run: async (signal) => {
        if (signal.aborted) return
        await refreshPage(activePage === 'dashboard' ? 'dashboard' : 'reports', 'panel_action')
      },
    },
    {
      id: 'lead-import-jobs',
      intervalMs: 3000,
      canRun: () => !!auth?.token && activePage === 'leads' && (leadImportJobs || []).some((j) => ['PENDING', 'RUNNING'].includes(String(j.status || '').toUpperCase())),
      run: async (signal) => {
        if (signal.aborted) return
        await refreshPage('leads', 'panel_action')
      },
    },
    {
      id: 'lead-import-export-jobs',
      intervalMs: 2500,
      canRun: () => !!auth?.token && activePage === 'leads' && !!leadImportJob?.id && (leadImportExportJobs || []).some((j) => ['PENDING', 'RUNNING'].includes(String(j.status || '').toUpperCase())),
      run: async () => {
        await refreshPage('leads', 'panel_action')
      },
    },
  ]), [
    auth?.token,
    activePage,
    autoRefreshJobs,
    exportJobs,
    autoRefreshReportJobs,
    reportExportJobs,
    leadImportJobs,
    leadImportJob?.id,
    leadImportExportJobs,
    refreshPage,
  ])
  useEffect(() => {
    const activeCount = (activePollers || []).filter((poller) => {
      try {
        return typeof poller?.canRun === 'function' && !!poller.canRun()
      } catch {
        return false
      }
    }).length
    markPollingActiveInstances(activeCount)
  }, [activePollers, markPollingActiveInstances])
  useActivePagePolling({ enabled: hasAuthToken, pollers: activePollers })
  const validateLogin = () => {
    const next = {}
    if (!loginForm.tenantId?.trim()) next.tenantId = t('fieldRequired')
    if (!loginForm.username?.trim()) next.username = t('fieldRequired')
    if (!loginForm.password?.trim()) next.password = t('fieldRequired')
    return next
  }

  const validateSso = () => {
    const next = {}
    if (!ssoForm.username?.trim()) next.username = t('fieldRequired')
    if (!ssoForm.code?.trim()) next.code = t('fieldRequired')
    return next
  }

  const validateContactForm = () => {
    const next = {}
    const phoneValue = String(contactForm.phone || '').trim()
    const emailValue = String(contactForm.email || '').trim()
    if (!String(contactForm.customerId || '').trim()) next.customerId = t('fieldRequired')
    if (!String(contactForm.name || '').trim()) next.name = t('fieldRequired')
    if (phoneValue && !/^\+?[0-9 ()-]{6,20}$/.test(phoneValue)) next.phone = t('invalidPhone')
    if (emailValue && !/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(emailValue)) next.email = t('invalidEmail')
    return next
  }

  const isValidDateString = (value) => {
    const raw = String(value || '').trim()
    if (!raw) return true
    const tenantFormat = normalizeDateFormat(auth?.dateFormat)
    if (parseDateByFormat(raw, tenantFormat)) return true
    return SUPPORTED_DATE_FORMATS.some((fmt) => parseDateByFormat(raw, fmt))
  }

  const validateCustomerForm = () => {
    const next = {}
    const valueRaw = String(customerForm.value || '').trim()
    if (!String(customerForm.name || '').trim()) next.name = t('fieldRequired')
    if (!String(customerForm.owner || '').trim()) next.owner = t('fieldRequired')
    if (!String(customerForm.status || '').trim()) next.status = t('fieldRequired')
    else if (!CUSTOMER_STATUS_OPTIONS.includes(customerForm.status)) next.status = t('invalidStatusOption')
    if (valueRaw) {
      const valueNum = Number(valueRaw)
      if (!Number.isFinite(valueNum)) next.value = t('invalidNumber')
      else if (valueNum < 0) next.value = t('nonNegativeNumber')
    }
    return next
  }

  const validateContractForm = () => {
    const next = {}
    const amountRaw = String(contractForm.amount || '').trim()
    if (!String(contractForm.customerId || '').trim()) next.customerId = t('fieldRequired')
    if (!String(contractForm.title || '').trim()) next.title = t('fieldRequired')
    if (!String(contractForm.status || '').trim()) next.status = t('fieldRequired')
    else if (!CONTRACT_STATUS_OPTIONS.includes(contractForm.status)) next.status = t('invalidStatusOption')
    if (amountRaw) {
      const amountNum = Number(amountRaw)
      if (!Number.isFinite(amountNum)) next.amount = t('invalidNumber')
      else if (amountNum < 0) next.amount = t('nonNegativeNumber')
    }
    if (!isValidDateString(contractForm.signDate)) next.signDate = t('invalidDateFormatText')
    return next
  }

  const validatePaymentForm = () => {
    const next = {}
    const amountRaw = String(paymentForm.amount || '').trim()
    if (!String(paymentForm.contractId || '').trim()) next.contractId = t('fieldRequired')
    if (amountRaw) {
      const amountNum = Number(amountRaw)
      if (!Number.isFinite(amountNum)) next.amount = t('invalidNumber')
      else if (amountNum < 0) next.amount = t('nonNegativeNumber')
    }
    if (!isValidDateString(paymentForm.receivedDate)) next.receivedDate = t('invalidDateFormatText')
    if (String(paymentForm.method || '').trim() && !PAYMENT_METHOD_OPTIONS.includes(paymentForm.method)) next.method = t('invalidMethodOption')
    if (String(paymentForm.status || '').trim() && !PAYMENT_STATUS_OPTIONS.includes(paymentForm.status)) next.status = t('invalidStatusOption')
    return next
  }
  const jumpToQuoteAfterLeadConvert = useCallback((prefill = {}) => {
    const nextOpportunityId = String(prefill?.opportunityId || '')
    setQuoteOpportunityFilter(nextOpportunityId)
    markNavStart(activePage, 'quotes')
    loadReasonRef.current = 'workbench_jump'
    setActivePage('quotes')
    navigate(PAGE_TO_PATH.quotes)
  }, [activePage, markNavStart, navigate])

  const {
    toggleTaskDone,
    saveLead,
    editLead,
    convertLead,
    bulkAssignLeadsByRule,
    bulkUpdateLeadStatus,
    saveCustomer,
    editCustomer,
    removeCustomer,
    saveOpportunity,
    editOpportunity,
    removeOpportunity,
    saveFollowUp,
    editFollowUp,
    removeFollowUp,
    saveContact,
    editContact,
    removeContact,
    saveContract,
    editContract,
    removeContract,
    savePayment,
    editPayment,
    removePayment,
  } = useAppCrudActions({
    authToken: auth?.token,
    lang,
    t,
    canWrite,
    canDeleteCustomer,
    canDeleteOpportunity,
    handleError,
    setCrudError,
    setCrudFieldError,
    pickFieldErrors,
    formatValidation,
    validateCustomerForm,
    validateContactForm,
    validateContractForm,
    validatePaymentForm,
    leadForm,
    setLeadForm,
    leads,
    customerForm,
    setCustomerForm,
    customerPage,
    setCustomerPage,
    customers,
    opportunityForm,
    setOpportunityForm,
    opportunityPage,
    setOpportunityPage,
    opportunities,
    followUpForm,
    setFollowUpForm,
    contactForm,
    setContactForm,
    contactPage,
    setContactPage,
    contacts,
    contractForm,
    setContractForm,
    contractPage,
    setContractPage,
    contracts,
    paymentForm,
    setPaymentForm,
    paymentPage,
    setPaymentPage,
    payments,
    refreshPage,
    setQuotePrefill,
    onLeadConvertedToQuote: jumpToQuoteAfterLeadConvert,
  })

  const submitLogin = async (e) => {
    e.preventDefault()
    logoutGuardRef.current = false
    setLoginError('')
    const nextErrors = validateLogin()
    setFormErrors((p) => ({ ...p, login: nextErrors }))
    if (Object.keys(nextErrors).length > 0) return
    try {
      if (mfaChallengeId && loginForm.mfaCode?.trim()) {
        const mfaAuth = await api('/v1/auth/mfa/verify', { method: 'POST', body: JSON.stringify({ challengeId: mfaChallengeId, code: loginForm.mfaCode }), headers: { 'X-Tenant-Id': loginForm.tenantId.trim() } }, null, lang)
        saveAuth(mfaAuth)
        localStorage.setItem('crm_last_tenant', loginForm.tenantId.trim())
        setMfaChallengeId('')
        setError('')
        setLoginError('')
        return
      }

      const payload = {
        tenantId: loginForm.tenantId.trim(),
        username: loginForm.username.trim(),
        password: loginForm.password,
        mfaCode: loginForm.mfaCode?.trim() || '',
      }
      const d = await api('/v1/auth/login', { method: 'POST', body: JSON.stringify(payload), headers: { 'X-Tenant-Id': payload.tenantId } }, null, lang)
      if (d?.mfaRequired) {
        setMfaChallengeId(d.challengeId || '')
        setLoginError(t('mfaPending'))
        return
      }
      saveAuth(d)
      localStorage.setItem('crm_last_tenant', payload.tenantId)
      setMfaChallengeId('')
      setLoginError('')
    } catch (err) { handleLoginError(err) }
  }

  const submitSsoLogin = async (e) => {
    e.preventDefault()
    logoutGuardRef.current = false
    setLoginError('')
    const nextErrors = validateSso()
    setFormErrors((p) => ({ ...p, sso: nextErrors }))
    if (Object.keys(nextErrors).length > 0) return
    try {
      const tenantId = loginForm.tenantId.trim() || localStorage.getItem('crm_last_tenant') || 'tenant_default'
      saveAuth(await api('/auth/sso/login', { method: 'POST', body: JSON.stringify(ssoForm), headers: { 'X-Tenant-Id': tenantId } }, null, lang))
      setLoginError('')
    } catch (err) { handleLoginError(err) }
  }

  const startOidcLogin = () => {
    logoutGuardRef.current = false
    if (!ssoConfig?.authorizeEndpoint || !ssoConfig?.clientId) { setLoginError(t('oidcConfigMissing')); return }
    const state = Math.random().toString(36).slice(2) + Date.now().toString(36)
    localStorage.setItem(OIDC_STATE_KEY, state)
    const redirect = ssoConfig.redirectUri || window.location.origin
    const scope = ssoConfig.scope || 'openid profile email'
    const q = new URLSearchParams({ response_type: 'code', client_id: ssoConfig.clientId, redirect_uri: redirect, scope, state })
    setOidcAuthorizing(true)
    window.location.assign(`${ssoConfig.authorizeEndpoint}?${q}`)
  }

  const {
    importLeadsCsv,
    selectLeadImportJob,
    cancelLeadImportJob,
    retryLeadImportJob,
    createLeadImportFailedRowsExportJob,
    updateLeadImportExportStatusFilter,
    onLeadImportExportPageChange,
    onLeadImportExportSizeChange,
    downloadLeadImportFailedRowsExportJob,
    updateLeadImportStatusFilter,
    onLeadImportPageChange,
    onLeadImportSizeChange,
    downloadLeadImportTemplate,
    isLeadImportActionPending,
  } = useLeadImportActions({
    auth,
    lang,
    t,
    canViewOpsMetrics,
    handleError,
    refreshPage,
    setLeadImportJob,
    setLeadImportFailedRows,
    setLeadImportStatusFilter,
    setLeadImportPage,
    setLeadImportSize,
    setLeadImportExportStatusFilter,
    setLeadImportExportPage,
    setLeadImportExportSize,
  })

  const {
    exportReportCsv,
    createExportJob,
    retryExportJob,
    downloadExportJob,
    retryReportExportJob,
    downloadReportExportJob,
    createReportDesignerTemplate,
    updateReportDesignerTemplate,
    runReportDesignerTemplate,
    saveLeadAssignmentRule,
    saveAutomationRule,
    changePermission,
    previewPermissionPack,
    commitPendingPack,
    rollbackPermissionRole,
    saveAdminUser,
    unlockAdminUser,
    getAdminUserError,
    inviteUser,
    createApprovalTemplate,
    submitApprovalInstance,
    updateApprovalTemplate,
    actApprovalTask,
    urgeApprovalTask,
    publishApprovalTemplate,
    rollbackApprovalTemplate,
    retryNotificationJob,
    toggleNotificationJob,
    toggleAllNotificationJobs,
    retryNotificationJobsByIds,
    retryNotificationJobsByFilter,
    createTenant,
    updateTenant,
  } = useAppPageActions({
    auth,
    lang,
    role,
    t,
    normalizeDateFormat,
    setError,
    handleError,
    assignmentRuleForm,
    setAssignmentRuleForm,
    loadLeadAssignmentRules,
    automationRuleForm,
    setAutomationRuleForm,
    loadAutomationRulesV1,
    canManagePermissions,
    permissionRole,
    pendingPack,
    setPendingPack,
    setPermissionPreview,
    setPermissionMatrix,
    loadPermissionConflicts,
    setAdminUsers,
    setInviteResult,
    setInviteForm,
    inviteForm,
    loadAdminUsers,
    approvalTemplateForm,
    setApprovalTemplateForm,
    setApprovalTemplates,
    loadApprovalTemplates,
    loadApprovalStats,
    loadApprovalInstances,
    loadApprovalTasks,
    approvalInstanceForm,
    setApprovalInstanceForm,
    approvalActionComment,
    setApprovalActionComment,
    approvalTransferTo,
    setApprovalTransferTo,
    setApprovalActionResult,
    approvalPendingTaskIds,
    setApprovalPendingTaskIds,
    approvalDetail,
    loadApprovalDetail,
    loadNotificationJobs,
    setSelectedNotificationJobs,
    notificationJobs,
    selectedNotificationJobs,
    notificationStatusFilter,
    notificationPage,
    notificationSize,
    tenantForm,
    setTenantForm,
    setLastCreatedTenant,
    loadTenants,
    setTenantRows,
    refreshPage,
    hasInvalidAuditRange,
    setAuditRangeError,
    auditUser,
    auditRole,
    auditAction,
    auditFrom,
    auditTo,
    reportOwner,
    reportDepartment,
    reportTimezone,
    reportCurrency,
    canViewReports,
    reportDesignerForm,
    setReportDesignerForm,
    setDesignerRunResult,
    loadApprovalTemplateVersions,
  })

  const {
    onPrefetch,
    onNavigate,
    trackWorkbenchEvent,
    buildWorkbenchFilterSignature,
    navigateToWorkbenchTarget,
    createQuoteFromOpportunity,
    viewOrdersFromOpportunity,
    createFollowUpShortcut,
    createTaskShortcut,
    urgeApprovalShortcut,
    copyPerfSnapshot,
  } = useAppShellBindings({
    prefetch: {
      authToken: auth?.token,
      pageSignature,
      customerQ,
      customerStatus,
      customerSize,
      canSkipFetch,
      loadCustomers,
      markFetched,
      loadApprovalTemplates,
      loadApprovalStats,
      markChunkPreloadHit,
      markNavStart,
      activePage,
      loadReasonRef,
      setActivePage,
      navigate,
      pageToPath: PAGE_TO_PATH,
      pageChunkPreloaders: PAGE_CHUNK_PRELOADERS,
    },
    workbench: {
      activePage,
      setActivePage,
      navigate,
      pageToPath: PAGE_TO_PATH,
      markNavStart,
      markWorkbenchJumpDecision,
      markWorkbenchActionResult,
      loadReasonRef,
      setPaymentStatus,
      setContractStatus,
      setApprovalTaskStatus,
      setFollowCustomerId,
      setFollowQ,
      setFollowUpForm,
      setQuoteOpportunityFilter,
      setQuoteStatusFilter: commerceDomain.quotes.setStatusFilter,
      setQuoteOwnerFilter: commerceDomain.quotes.setOwnerFilter,
      setQuotePrefill,
      setOrderOpportunityFilter,
      setOrderStatusFilter: commerceDomain.orders.setStatusFilter,
      setOrderOwnerFilter: commerceDomain.orders.setOwnerFilter,
      setLeadQ,
      setLeadStatus,
      setLeadPage,
      setLeadSize,
      leadSize,
      setCustomerQ,
      setCustomerStatus,
      setCustomerPage,
      setCustomerSize,
      customerSize,
      workbenchJumpRef,
      onWorkbenchJumpMeta: markWorkbenchJumpMeta,
    },
    perf: {
      activePage,
      markNavEnd,
      devEnabled: typeof import.meta !== 'undefined' && !!import.meta.env?.DEV,
      getMetrics,
      setPerfMetrics,
      currentLoaderKey,
      lastRefreshReason,
      currentPageSignature,
      domainLoadSource,
      setLastPerfSnapshotAt,
    },
  })
  const consumeQuotePrefill = () => setQuotePrefill(null)
  const mainContentInputs = useAppViewBindings({
    base: {
      currentPageLabel, lang, setLang, refreshPage, t, canWrite, role, error, loading, activePage,
      stats, reports, workbenchToday, canViewReports, auditFrom, auditTo, auditRole, reportOwner, setReportOwner,
      reportDepartment, setReportDepartment, reportTimezone, setReportTimezone, reportCurrency, setReportCurrency,
      reportExportJobs, reportExportStatusFilter, setReportExportStatusFilter, reportExportJobsPage,
      setReportExportJobsPage, reportExportJobsTotalPages, reportExportJobsSize, setReportExportJobsSize,
      autoRefreshReportJobs, setAutoRefreshReportJobs, quoteOpportunityFilter, orderOpportunityFilter,
      quotePrefill, consumeQuotePrefill, auth, tasks, reportDesignerForm, setReportDesignerForm,
      designerTemplates, designerRunResult,
    },
    domains: {
      customer: {
        leadForm, setLeadForm, saveLead, convertLead, crudErrors, crudFieldErrors, leads, editLead, leadQ, setLeadQ,
        leadStatus, setLeadStatus, leadPage, leadTotalPages, leadSize, onLeadPageChange, onLeadSizeChange, loadLeads,
        bulkAssignLeadsByRule, bulkUpdateLeadStatus,
        importLeadsCsv, leadImportJob, leadImportJobs, leadImportStatusFilter, updateLeadImportStatusFilter,
        leadImportPage, leadImportTotalPages, leadImportSize, onLeadImportPageChange, onLeadImportSizeChange,
        selectLeadImportJob, cancelLeadImportJob, retryLeadImportJob, leadImportFailedRows, downloadLeadImportTemplate,
        leadImportMetrics, leadImportExportJobs, leadImportExportStatusFilter, updateLeadImportExportStatusFilter,
        leadImportExportPage, leadImportExportTotalPages, leadImportExportSize, onLeadImportExportPageChange,
        onLeadImportExportSizeChange, createLeadImportFailedRowsExportJob, downloadLeadImportFailedRowsExportJob, isLeadImportActionPending,
        customerForm, setCustomerForm, saveCustomer, customers, editCustomer, canDeleteCustomer, removeCustomer,
        customerQ, setCustomerQ, customerStatus, setCustomerStatus, customerPage, customerTotalPages, customerSize,
        onCustomerPageChange, onCustomerSizeChange, loadCustomers, loadCustomerTimeline, customerTimeline,
        opportunityForm, setOpportunityForm, saveOpportunity, opportunities, editOpportunity, canDeleteOpportunity,
        removeOpportunity, oppStage, setOppStage, opportunityPage, opportunityTotalPages, opportunitySize,
        onOpportunityPageChange, onOpportunitySizeChange, loadOpportunities, createQuoteFromOpportunity,
        viewOrdersFromOpportunity, loadOpportunityTimeline, opportunityTimeline, urgeApprovalShortcut,
        customer360Metrics: {
          markActionResult: markCustomer360ActionResult,
          markModuleRefreshLatency: markCustomer360ModuleRefreshLatency,
          markJumpHit: markCustomer360JumpHit,
          markModuleCacheHit: markCustomer360ModuleCacheHit,
          markPrefetchHit: markCustomer360PrefetchHit,
          markPrefetchAbort: markCustomer360PrefetchAbort,
          markPrefetchModules: markCustomer360PrefetchModules,
        },
        buildWorkbenchFilterSignature,
        followUpForm, setFollowUpForm, saveFollowUp, followUps, editFollowUp, removeFollowUp, followCustomerId,
        setFollowCustomerId, followQ, setFollowQ, loadFollowUps, createFollowUpShortcut,
        contactForm, setContactForm, saveContact, contacts, editContact, removeContact, contactQ, setContactQ,
        contactPage, contactTotalPages, contactSize, onContactPageChange, onContactSizeChange, loadContacts,
      },
      commerce: {
        commerceDomain,
        contractForm, setContractForm, saveContract, contracts, editContract, removeContract, contractQ, setContractQ,
        contractStatus, setContractStatus, contractPage, contractTotalPages, contractSize, onContractPageChange,
        onContractSizeChange, loadContracts, paymentForm, setPaymentForm, savePayment, payments, editPayment,
        removePayment, paymentStatus, setPaymentStatus, paymentPage, paymentTotalPages, paymentSize,
        onPaymentPageChange, onPaymentSizeChange, loadPayments,
      },
      governance: {
        permissionRole, setPermissionRole, canManagePermissions, pendingPack, permissionPreview, permissionMatrix, permissionConflicts,
        canManageUsers, adminUsers, loadAdminUsers, setAdminUsers, inviteForm, setInviteForm, inviteResult, canManageSalesAutomation, leadAssignmentRules,
        assignmentRuleForm, setAssignmentRuleForm, loadLeadAssignmentRules,
        automationRules, automationRuleForm, setAutomationRuleForm, loadAutomationRulesV1,
        tenantForm, setTenantForm, tenantRows, setTenantRows, loadTenants, lastCreatedTenant,
      },
      approval: {
        approvalTemplateForm, setApprovalTemplateForm, approvalTemplates, setApprovalTemplates, approvalTemplateVersions, loadApprovalTemplateVersions,
        approvalVersionTemplateId, approvalStats, approvalInstanceForm, setApprovalInstanceForm, approvalTasks, approvalInstances, approvalDetail, loadApprovalDetail, approvalTaskStatus,
        setApprovalTaskStatus, approvalOverdueOnly, setApprovalOverdueOnly, approvalEscalatedOnly, setApprovalEscalatedOnly,
        approvalActionComment, setApprovalActionComment, approvalTransferTo, setApprovalTransferTo, notificationJobs, notificationStatusFilter, setNotificationStatusFilter, notificationPage,
        notificationTotalPages, notificationSize, setNotificationPage, setNotificationSize, selectedNotificationJobs,
        approvalActionResult, setApprovalActionResult,
        approvalPendingTaskIds, setApprovalPendingTaskIds,
        loadApprovalTasks, loadApprovalTemplates, loadNotificationJobs,
      },
      reporting: {
        canViewAudit, auditUser, setAuditUser, setAuditRole, auditAction, setAuditAction, setAuditFrom, setAuditTo,
        auditRangeError, setAuditRangeError, hasInvalidAuditRange, loadAudit, createExportJob, loadExportJobs,
        autoRefreshJobs, setAutoRefreshJobs, auditLogs, exportStatusFilter, setExportStatusFilter, exportJobs,
        exportJobsPage, setExportJobsPage, exportJobsTotalPages, exportJobsSize, setExportJobsSize, downloadExportJob,
        retryExportJob,
      },
      workbench: {},
    },
    actions: {
      exportReportCsv, loadReportExportJobs, retryReportExportJob, downloadReportExportJob,
      performLogout, navigateToWorkbenchTarget, createTaskShortcut, trackWorkbenchEvent,
      toggleTaskDone, createReportDesignerTemplate,
      updateReportDesignerTemplate, runReportDesignerTemplate, loadDesignerTemplates,
    },
    pageActions: {
      saveLeadAssignmentRule,
      saveAutomationRule,
      changePermission,
      previewPermissionPack,
      commitPendingPack,
      rollbackPermissionRole,
      saveAdminUser,
      unlockAdminUser,
      getAdminUserError,
      inviteUser,
      createApprovalTemplate,
      submitApprovalInstance,
      updateApprovalTemplate,
      actApprovalTask,
      urgeApprovalTask,
      publishApprovalTemplate,
      rollbackApprovalTemplate,
      retryNotificationJob,
      toggleNotificationJob,
      toggleAllNotificationJobs,
      retryNotificationJobsByIds,
      retryNotificationJobsByFilter,
      createTenant,
      updateTenant,
    },
    crudActions: {
      saveLead,
      saveCustomer,
      removeCustomer,
      saveOpportunity,
      removeOpportunity,
      saveFollowUp,
      removeFollowUp,
      saveContact,
      removeContact,
      saveContract,
      removeContract,
      savePayment,
      removePayment,
      toggleTaskDone,
    },
    capabilities: {
      canViewReports,
      canManagePermissions,
      canManageUsers,
      canManageSalesAutomation,
      canViewAudit,
    },
  })

  const {
    mainBase,
    mainPermissions,
    mainUsers,
    mainCommerce,
    mainLeads,
    mainCustomers,
    mainPipeline,
    mainFollowUps,
    mainContacts,
    mainContracts,
    mainPayments,
    mainTasks,
    mainReportDesigner,
    mainSalesAutomation,
    mainAudit,
    mainApprovals,
    mainTenants,
  } = useAppMainContentModel(mainContentInputs)
  const apiContext = useMemo(() => ({ token: auth?.token || '', lang, tenantId: auth?.tenantId || '' }), [auth?.token, auth?.tenantId, lang])

  const isAuthRoute = location.pathname === '/login' || location.pathname === '/activate'
  const authShell = (
    <AuthShell
      auth={!hasAuthToken || isAuthRoute ? null : auth}
      locationPathname={location.pathname}
      apiContext={apiContext}
      lang={lang}
      setLang={setLang}
      t={t}
      navigate={navigate}
      submitLogin={submitLogin}
      loginForm={loginForm}
      setLoginForm={setLoginForm}
      formErrors={formErrors}
      setFormErrors={setFormErrors}
      submitSsoLogin={submitSsoLogin}
      ssoConfig={ssoConfig}
      ssoForm={ssoForm}
      setSsoForm={setSsoForm}
      oidcAuthorizing={oidcAuthorizing}
      startOidcLogin={startOidcLogin}
      loginError={loginError}
    />
  )
  if (sessionBootstrapping && !hasAuthToken && !isAuthRoute) {
    return <div className="app-bootstrapping">Loading session...</div>
  }
  if (!hasAuthToken || isAuthRoute) return <AppProviders apiContext={apiContext}>{authShell}</AppProviders>

  return (
    <AppProviders apiContext={apiContext}>
      <AppShell
        auth={auth}
        navGroups={navGroups}
        activePage={activePage}
        onNavigate={onNavigate}
        onPrefetch={onPrefetch}
        performLogout={performLogout}
        t={t}
        mainBase={mainBase}
        mainPermissions={mainPermissions}
        mainUsers={mainUsers}
        mainSalesAutomation={mainSalesAutomation}
        mainLeads={mainLeads}
        mainCustomers={mainCustomers}
        mainPipeline={mainPipeline}
        mainCommerce={mainCommerce}
        mainFollowUps={mainFollowUps}
        mainContacts={mainContacts}
        mainContracts={mainContracts}
        mainPayments={mainPayments}
        mainReportDesigner={mainReportDesigner}
        mainTasks={mainTasks}
        mainAudit={mainAudit}
        mainApprovals={mainApprovals}
        mainTenants={mainTenants}
        dev={import.meta.env.DEV && perfEnabledByQuery}
        perfMetrics={perfMetrics}
        currentLoaderKey={currentLoaderKey}
        lastRefreshReason={lastRefreshReason}
        currentPageSignature={currentPageSignature}
        currentSignatureHit={currentSignatureHit}
        recentWorkbenchJump={recentWorkbenchJump}
        domainLoadSource={domainLoadSource}
        copyPerfSnapshot={copyPerfSnapshot}
        lastPerfSnapshotAt={lastPerfSnapshotAt}
      />
    </AppProviders>
  )
}

export default App



















