import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { API_BASE, FILTERS_KEY, LANG_KEY, OIDC_STATE_KEY, ROLES, READ_OPS, WRITE_OPS, CUSTOMER_STATUS_OPTIONS, CONTRACT_STATUS_OPTIONS, PAYMENT_METHOD_OPTIONS, PAYMENT_STATUS_OPTIONS, readFilters, api } from './crm/shared'
import LoginView from './crm/components/LoginView'
import InvitationAcceptView from './crm/components/InvitationAcceptView'
import SidebarNav from './crm/components/SidebarNav'
import MainContent from './crm/components/MainContent'
import { tFactory } from './crm/i18n'
import './App.css'

const PAGE_TO_PATH = {
  dashboard: '/dashboard',
  customers: '/customers',
  contacts: '/contacts',
  pipeline: '/opportunities',
  contracts: '/contracts',
  payments: '/payments',
  followUps: '/follow-ups',
  tasks: '/tasks',
  reports: '/reports',
  approvals: '/approvals',
  audit: '/audit',
  permissions: '/admin/permissions',
  usersAdmin: '/admin/users',
  adminTenants: '/admin/tenants',
}

const PATH_TO_PAGE = Object.entries(PAGE_TO_PATH).reduce((acc, [page, path]) => ({ ...acc, [path]: page }), {})

function App() {
  const location = useLocation()
  const navigate = useNavigate()
  const [lang, setLang] = useState(() => localStorage.getItem(LANG_KEY) || 'en')
  const t = tFactory(lang)
  const [auth, setAuth] = useState(() => JSON.parse(localStorage.getItem('crm_auth') || 'null'))
  const persisted = useMemo(() => readFilters(), [])
  const readPageSize = (key, fallback = 8) => {
    const raw = Number(localStorage.getItem(key) || fallback)
    if (!Number.isFinite(raw)) return fallback
    return Math.min(Math.max(Math.floor(raw), 5), 50)
  }

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [crudErrors, setCrudErrors] = useState({ customer: '', opportunity: '', followUp: '', contact: '', contract: '', payment: '' })
  const [crudFieldErrors, setCrudFieldErrors] = useState({ customer: {}, opportunity: {}, followUp: {}, contact: {}, contract: {}, payment: {} })
  const [stats, setStats] = useState([])
  const [customers, setCustomers] = useState([])
  const [tasks, setTasks] = useState([])
  const [opportunities, setOpportunities] = useState([])
  const [followUps, setFollowUps] = useState([])
  const [contacts, setContacts] = useState([])
  const [contracts, setContracts] = useState([])
  const [payments, setPayments] = useState([])
  const [auditLogs, setAuditLogs] = useState([])
  const [reports, setReports] = useState(null)
  const [permissionMatrix, setPermissionMatrix] = useState([])
  const [permissionConflicts, setPermissionConflicts] = useState([])
  const [permissionRole, setPermissionRole] = useState('SALES')
  const [permissionPreview, setPermissionPreview] = useState(null)
  const [pendingPack, setPendingPack] = useState('')
  const [exportJobs, setExportJobs] = useState([])
  const [autoRefreshJobs, setAutoRefreshJobs] = useState(true)
  const [exportStatusFilter, setExportStatusFilter] = useState('ALL')
  const [reportExportJobs, setReportExportJobs] = useState([])
  const [autoRefreshReportJobs, setAutoRefreshReportJobs] = useState(true)
  const [reportExportStatusFilter, setReportExportStatusFilter] = useState('ALL')

  const [customerQ, setCustomerQ] = useState(persisted.customerQ || '')
  const [customerStatus, setCustomerStatus] = useState(persisted.customerStatus || '')
  const [oppStage, setOppStage] = useState(persisted.oppStage || '')
  const [followCustomerId] = useState(persisted.followCustomerId || '')
  const [followQ] = useState(persisted.followQ || '')
  const [auditUser, setAuditUser] = useState(persisted.auditUser || '')
  const [auditRole, setAuditRole] = useState(persisted.auditRole || '')
  const [auditAction, setAuditAction] = useState(persisted.auditAction || '')
  const [auditFrom, setAuditFrom] = useState(persisted.auditFrom || '')
  const [auditTo, setAuditTo] = useState(persisted.auditTo || '')
  const [reportOwner, setReportOwner] = useState(persisted.reportOwner || '')
  const [reportDepartment, setReportDepartment] = useState(persisted.reportDepartment || '')
  const [reportTimezone, setReportTimezone] = useState(persisted.reportTimezone || (Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Shanghai'))
  const [reportCurrency, setReportCurrency] = useState(persisted.reportCurrency || 'CNY')

  const [loginForm, setLoginForm] = useState({ tenantId: 'tenant_default', username: '', password: '', mfaCode: '' })
  const [mfaChallengeId, setMfaChallengeId] = useState('')
  const [ssoConfig, setSsoConfig] = useState({ enabled: false, providerName: '', mode: 'mock' })
  const [ssoForm, setSsoForm] = useState({ username: 'sso_user', code: 'SSO-ACCESS', displayName: '' })
  const [adminUsers, setAdminUsers] = useState([])
  const [oidcAuthorizing, setOidcAuthorizing] = useState(false)
  const [formErrors, setFormErrors] = useState({ login: {}, register: {}, sso: {} })
  const [auditRangeError, setAuditRangeError] = useState('')
  const [activePage, setActivePage] = useState('dashboard')
  const [customerForm, setCustomerForm] = useState({ id: '', name: '', owner: '', status: '', tag: '', value: '' })
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
  const [notificationJobs, setNotificationJobs] = useState([])
  const [notificationStatusFilter, setNotificationStatusFilter] = useState('ALL')
  const [notificationPage, setNotificationPage] = useState(1)
  const [notificationTotalPages, setNotificationTotalPages] = useState(1)
  const [notificationSize, setNotificationSize] = useState(() => readPageSize('crm_page_size_notification_jobs', 10))
  const [selectedNotificationJobs, setSelectedNotificationJobs] = useState([])
  const [tenantForm, setTenantForm] = useState({ name: '', quotaUsers: '100', timezone: 'Asia/Shanghai', currency: 'CNY', status: 'ACTIVE' })
  const [lastCreatedTenant, setLastCreatedTenant] = useState(null)
  const [tenantRows, setTenantRows] = useState([])
  const [inviteForm, setInviteForm] = useState({ username: '', role: 'SALES', ownerScope: '', department: 'DEFAULT', dataScope: 'SELF' })
  const [inviteResult, setInviteResult] = useState(null)

  const [customerPage, setCustomerPage] = useState(1)
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
  const seqRef = useRef({ customers: 0, opportunities: 0, contacts: 0, contracts: 0, payments: 0 })

  const role = auth?.role || ''
  const canWrite = ['ADMIN', 'MANAGER', 'SALES'].includes(role)
  const canDeleteCustomer = ['ADMIN', 'MANAGER'].includes(role)
  const canDeleteOpportunity = ['ADMIN', 'MANAGER'].includes(role)
  const canViewAudit = ['ADMIN', 'MANAGER', 'ANALYST'].includes(role)
  const canViewReports = ['ADMIN', 'MANAGER', 'ANALYST'].includes(role)
  const canManagePermissions = role === 'ADMIN'
  const canManageUsers = role === 'ADMIN'

  const navItems = useMemo(() => {
    const businessGroup = t('businessGroup')
    const governanceGroup = t('governanceGroup')
    const items = [
      { key: 'dashboard', label: t('dashboard'), group: businessGroup },
      { key: 'customers', label: t('customers'), group: businessGroup },
      { key: 'contacts', label: t('contacts'), group: businessGroup },
      { key: 'pipeline', label: t('pipeline'), group: businessGroup },
      { key: 'contracts', label: t('contracts'), group: businessGroup },
      { key: 'payments', label: t('payments'), group: businessGroup },
      { key: 'followUps', label: t('followUps'), group: businessGroup },
      { key: 'tasks', label: t('tasks'), group: businessGroup },
      { key: 'approvals', label: t('approvals'), group: businessGroup },
      { key: 'reports', label: t('reports'), group: businessGroup },
    ]
    if (canViewAudit) items.push({ key: 'audit', label: t('audit'), group: governanceGroup })
    if (canManagePermissions) items.push({ key: 'permissions', label: t('permissions'), group: governanceGroup })
    if (canManageUsers) items.push({ key: 'usersAdmin', label: t('usersAdmin'), group: governanceGroup })
    if (canManageUsers) items.push({ key: 'adminTenants', label: t('tenantsAdmin'), group: governanceGroup })
    return items
  }, [canViewAudit, canManagePermissions, canManageUsers, t])

  useEffect(() => { if (!navItems.some((it) => it.key === activePage)) setActivePage(navItems[0]?.key || 'dashboard') }, [activePage, navItems])
  useEffect(() => {
    const routePage = PATH_TO_PAGE[location.pathname]
    if (routePage && navItems.some((it) => it.key === routePage)) {
      setActivePage(routePage)
      return
    }
    if (!routePage && auth?.token && location.pathname !== '/activate') {
      navigate(PAGE_TO_PATH.dashboard, { replace: true })
    }
  }, [location.pathname, navItems, auth?.token, navigate])

  const saveAuth = useCallback((next) => { if (!next) { localStorage.removeItem('crm_auth'); setAuth(null); return } localStorage.setItem('crm_auth', JSON.stringify(next)); setAuth(next) }, [])
  const formatValidation = (err) => { const ve = err?.validationErrors; if (!ve || typeof ve !== 'object') return err.message; const first = Object.entries(ve)[0]; return first ? `${first[0]}: ${first[1]}` : err.message }
  const handleError = useCallback((err) => { if (err.status === 401 && auth?.token) { saveAuth(null); setError(t('sessionExpired')); return } const msg = err.status === 400 ? formatValidation(err) : err.message; setError(err.requestId ? `${msg} [${err.requestId}]` : msg) }, [auth?.token, saveAuth, t])
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

  const loadCustomers = async (page = customerPage, size = customerSize) => { const reqId = ++seqRef.current.customers; const q = new URLSearchParams({ q: customerQ, status: customerStatus, page: String(page), size: String(size) }); const d = await api('/customers/search?' + q, {}, auth.token, lang); if (reqId !== seqRef.current.customers) return; setCustomers(d.items || []); setCustomerPage(d.page || page); setCustomerTotalPages(Math.max(1, d.totalPages || 1)) }
  const loadTasks = async () => { const d = await api('/tasks/search?page=1&size=10', {}, auth.token, lang); setTasks(d.items || []) }
  const loadOpportunities = async (page = opportunityPage, size = opportunitySize) => { const reqId = ++seqRef.current.opportunities; const q = new URLSearchParams({ stage: oppStage, page: String(page), size: String(size) }); const d = await api('/opportunities/search?' + q, {}, auth.token, lang); if (reqId !== seqRef.current.opportunities) return; setOpportunities(d.items || []); setOpportunityPage(d.page || page); setOpportunityTotalPages(Math.max(1, d.totalPages || 1)) }
  const loadFollowUps = async () => { const q = new URLSearchParams({ customerId: followCustomerId, q: followQ, page: '1', size: '8' }); const d = await api('/follow-ups/search?' + q, {}, auth.token, lang); setFollowUps(d.items || []) }
  const loadContacts = async (page = contactPage, size = contactSize) => { const reqId = ++seqRef.current.contacts; const q = new URLSearchParams({ customerId: '', q: contactQ, page: String(page), size: String(size) }); const d = await api('/contacts/search?' + q, {}, auth.token, lang); if (reqId !== seqRef.current.contacts) return; setContacts(d.items || []); setContactPage(d.page || page); setContactTotalPages(Math.max(1, d.totalPages || 1)) }
  const loadContracts = async (page = contractPage, size = contractSize) => { const reqId = ++seqRef.current.contracts; const q = new URLSearchParams({ customerId: '', status: contractStatus, q: contractQ, page: String(page), size: String(size) }); const d = await api('/contracts/search?' + q, {}, auth.token, lang); if (reqId !== seqRef.current.contracts) return; setContracts(d.items || []); setContractPage(d.page || page); setContractTotalPages(Math.max(1, d.totalPages || 1)) }
  const loadPayments = async (page = paymentPage, size = paymentSize) => { const reqId = ++seqRef.current.payments; const q = new URLSearchParams({ customerId: '', contractId: '', status: paymentStatus, page: String(page), size: String(size) }); const d = await api('/payments/search?' + q, {}, auth.token, lang); if (reqId !== seqRef.current.payments) return; setPayments(d.items || []); setPaymentPage(d.page || page); setPaymentTotalPages(Math.max(1, d.totalPages || 1)) }
  const loadAudit = async () => { if (!canViewAudit) return; if (hasInvalidAuditRange()) { setAuditRangeError(t('dateRangeInvalid')); return } setAuditRangeError(''); const q = new URLSearchParams({ username: auditUser, role: auditRole, action: auditAction, from: auditFrom, to: auditTo, page: '1', size: '10' }); const d = await api('/audit-logs/search?' + q, {}, auth.token, lang); setAuditLogs(d.items || []) }
  const loadReports = async () => { if (!canViewReports) return; const q = new URLSearchParams({ from: auditFrom, to: auditTo, role: auditRole, owner: reportOwner, department: reportDepartment, timezone: reportTimezone, currency: reportCurrency }); setReports(await api('/v1/reports/overview?' + q, {}, auth.token, lang)) }
  const loadPermissionMatrix = async () => { const d = await api('/permissions/matrix', {}, auth.token, lang); setPermissionMatrix(d.matrix || []) }
  const loadPermissionConflicts = async () => { const d = await api('/permissions/conflicts', {}, auth.token, lang); setPermissionConflicts(d.items || []) }
  const loadExportJobs = async () => { const q = new URLSearchParams({ limit: '8' }); if (exportStatusFilter !== 'ALL') q.set('status', exportStatusFilter); const d = await api('/audit-logs/export-jobs?' + q, {}, auth.token, lang); setExportJobs(d.items || []) }
  const loadReportExportJobs = async () => { const q = new URLSearchParams({ limit: '8' }); if (reportExportStatusFilter !== 'ALL') q.set('status', reportExportStatusFilter); const d = await api('/v1/reports/export-jobs?' + q, {}, auth.token, lang); setReportExportJobs(d.items || []) }
  const loadSsoConfig = async () => { try { const d = await api('/auth/sso/config', {}, null, lang); setSsoConfig(d || { enabled: false, providerName: '', mode: 'mock' }) } catch { setSsoConfig({ enabled: false, providerName: '', mode: 'mock' }) } }
  const loadAdminUsers = async () => { if (!canManageUsers) return; const d = await api('/v1/admin/users', {}, auth.token, lang); setAdminUsers(d.items || []) }
  const loadApprovalTemplates = async () => { const q = new URLSearchParams({ limit: '100' }); const d = await api('/v1/approval/templates?' + q, {}, auth.token, lang); setApprovalTemplates(d.items || []) }
  const loadApprovalStats = async () => { const d = await api('/v1/approval/stats', {}, auth.token, lang); setApprovalStats(d || null) }
  const loadApprovalTasks = async () => {
    const q = new URLSearchParams({ status: approvalTaskStatus, limit: '20' })
    if (approvalOverdueOnly) q.set('overdue', 'true')
    if (approvalEscalatedOnly) q.set('escalated', 'true')
    const d = await api('/v1/approval/tasks?' + q, {}, auth.token, lang)
    setApprovalTasks(d.items || [])
  }
  const loadApprovalInstances = async () => { const d = await api('/v1/approval/instances?limit=12', {}, auth.token, lang); setApprovalInstances(d.items || []) }
  const loadApprovalDetail = async (id) => { const d = await api('/v1/approval/instances/' + id, {}, auth.token, lang); setApprovalDetail(d) }
  const loadApprovalTemplateVersions = async (templateId) => { const d = await api('/v1/approval/templates/' + templateId + '/versions', {}, auth.token, lang); setApprovalTemplateVersions(d.items || []); setApprovalVersionTemplateId(templateId) }
  const loadNotificationJobs = async (page = notificationPage, size = notificationSize) => {
    const q = new URLSearchParams({ page: String(page), size: String(size) })
    if (notificationStatusFilter !== 'ALL') q.set('status', notificationStatusFilter)
    const d = await api('/v1/integrations/notifications/jobs?' + q, {}, auth.token, lang)
    setNotificationJobs(d.items || [])
    setNotificationPage(d.page || page)
    setNotificationTotalPages(Math.max(1, d.totalPages || 1))
    setSelectedNotificationJobs([])
  }
  const loadTenants = async () => { if (!canManageUsers) return; const d = await api('/v1/tenants', {}, auth.token, lang); setTenantRows(d.items || []) }

  const loadAll = async () => {
    if (!auth?.token) return
    setLoading(true); setError('')
    try {
      const d = await api('/dashboard', {}, auth.token, lang)
      setStats(d.stats || [])
      await Promise.all([loadCustomers(), loadTasks(), loadOpportunities(), loadFollowUps(), loadContacts(), loadContracts(), loadPayments(), loadAudit(), loadReports(), loadPermissionMatrix(), loadPermissionConflicts(), loadExportJobs(), loadApprovalTemplates(), loadApprovalStats(), loadApprovalTasks(), loadApprovalInstances(), loadNotificationJobs(), canViewReports ? loadReportExportJobs() : Promise.resolve(null), canManageUsers ? loadAdminUsers() : Promise.resolve(null), canManageUsers ? loadTenants() : Promise.resolve(null)])
    } catch (err) { handleError(err) } finally { setLoading(false) }
  }

  useEffect(() => localStorage.setItem(LANG_KEY, lang), [lang])
  useEffect(() => {
    if (!auth?.token && location.pathname !== '/login' && location.pathname !== '/activate') {
      navigate('/login', { replace: true })
      return
    }
    if (auth?.token && (location.pathname === '/' || location.pathname === '/login' || location.pathname === '/activate')) {
      navigate(PAGE_TO_PATH.dashboard, { replace: true })
    }
  }, [auth?.token, location.pathname, navigate])
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
    if (expected && state && expected !== state) { setError(t('invalidOidcState')); return }
    localStorage.removeItem(OIDC_STATE_KEY)
    ;(async () => {
      try { setOidcAuthorizing(true); const d = await api('/auth/sso/login', { method: 'POST', body: JSON.stringify({ code }) }, null, lang); saveAuth(d); const url = new URL(window.location.href); url.searchParams.delete('code'); url.searchParams.delete('state'); window.history.replaceState({}, document.title, url.toString()) } catch (err) { handleError(err) } finally { setOidcAuthorizing(false) }
    })()
  }, [auth?.token, ssoConfig?.enabled, ssoConfig?.mode, lang, handleError, saveAuth])

  useEffect(() => localStorage.setItem(FILTERS_KEY, JSON.stringify({ customerQ, customerStatus, oppStage, followCustomerId, followQ, contactQ, contractQ, contractStatus, paymentStatus, auditUser, auditRole, auditAction, auditFrom, auditTo, reportOwner, reportDepartment, reportTimezone, reportCurrency })), [customerQ, customerStatus, oppStage, followCustomerId, followQ, contactQ, contractQ, contractStatus, paymentStatus, auditUser, auditRole, auditAction, auditFrom, auditTo, reportOwner, reportDepartment, reportTimezone, reportCurrency])
  useEffect(() => {
    localStorage.setItem('crm_page_size_customers', String(customerSize))
    localStorage.setItem('crm_page_size_opportunities', String(opportunitySize))
    localStorage.setItem('crm_page_size_contacts', String(contactSize))
    localStorage.setItem('crm_page_size_contracts', String(contractSize))
    localStorage.setItem('crm_page_size_payments', String(paymentSize))
    localStorage.setItem('crm_page_size_notification_jobs', String(notificationSize))
  }, [customerSize, opportunitySize, contactSize, contractSize, paymentSize, notificationSize])
  useEffect(() => { loadAll() }, [auth?.token, lang])
  useEffect(() => { if (!auth?.token) return; const timer = setTimeout(() => { loadCustomers(1, customerSize).catch(handleError) }, 350); return () => clearTimeout(timer) }, [auth?.token, customerQ, customerStatus, customerSize, lang, handleError])
  useEffect(() => { if (!auth?.token) return; const timer = setTimeout(() => { loadOpportunities(1, opportunitySize).catch(handleError) }, 350); return () => clearTimeout(timer) }, [auth?.token, oppStage, opportunitySize, lang, handleError])
  useEffect(() => { if (!auth?.token) return; const timer = setTimeout(() => { loadContacts(1, contactSize).catch(handleError) }, 350); return () => clearTimeout(timer) }, [auth?.token, contactQ, contactSize, lang, handleError])
  useEffect(() => { if (!auth?.token) return; const timer = setTimeout(() => { loadContracts(1, contractSize).catch(handleError) }, 350); return () => clearTimeout(timer) }, [auth?.token, contractQ, contractStatus, contractSize, lang, handleError])
  useEffect(() => { if (!auth?.token) return; const timer = setTimeout(() => { loadPayments(1, paymentSize).catch(handleError) }, 350); return () => clearTimeout(timer) }, [auth?.token, paymentStatus, paymentSize, lang, handleError])
  useEffect(() => { if (!auth?.token || !canViewReports) return; const timer = setTimeout(() => { loadReports().catch(handleError) }, 350); return () => clearTimeout(timer) }, [auth?.token, canViewReports, auditFrom, auditTo, auditRole, reportOwner, reportDepartment, reportTimezone, reportCurrency, lang, handleError])
  useEffect(() => { if (!auth?.token) return; loadApprovalTasks().catch(handleError) }, [auth?.token, approvalTaskStatus, approvalOverdueOnly, approvalEscalatedOnly, lang, handleError])
  useEffect(() => { setNotificationPage(1) }, [notificationStatusFilter])
  useEffect(() => { setNotificationPage(1) }, [notificationSize])
  useEffect(() => { if (!auth?.token) return; loadNotificationJobs(notificationPage, notificationSize).catch(handleError) }, [auth?.token, notificationStatusFilter, notificationPage, notificationSize, lang, handleError])
  useEffect(() => { if (auth?.token) loadExportJobs() }, [auth?.token, exportStatusFilter, lang])
  useEffect(() => { if (auth?.token && canViewReports) loadReportExportJobs() }, [auth?.token, canViewReports, reportExportStatusFilter, lang])
  /* eslint-enable react-hooks/exhaustive-deps */
  useEffect(() => { if (!auth?.token || !autoRefreshJobs || exportJobs.length === 0) return; const pending = exportJobs.some((j) => ['PENDING', 'RUNNING'].includes(j.status)); if (!pending) return; const timer = setInterval(async () => { try { const jobs = await Promise.all(exportJobs.map(async (job) => !['PENDING', 'RUNNING'].includes(job.status) ? job : { ...job, ...(await api('/audit-logs/export-jobs/' + job.jobId, {}, auth.token, lang)) })); setExportJobs(jobs) } catch (err) { handleError(err) } }, 1800); return () => clearInterval(timer) }, [auth?.token, exportJobs, lang, autoRefreshJobs, handleError])
  useEffect(() => { if (!auth?.token || !autoRefreshReportJobs || reportExportJobs.length === 0) return; const pending = reportExportJobs.some((j) => ['PENDING', 'RUNNING'].includes(j.status)); if (!pending) return; const timer = setInterval(async () => { try { const jobs = await Promise.all(reportExportJobs.map(async (job) => !['PENDING', 'RUNNING'].includes(job.status) ? job : { ...job, ...(await api('/v1/reports/export-jobs/' + job.jobId, {}, auth.token, lang)) })); setReportExportJobs(jobs) } catch (err) { handleError(err) } }, 1800); return () => clearInterval(timer) }, [auth?.token, reportExportJobs, lang, autoRefreshReportJobs, handleError])
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
    if (!/^\d{4}-\d{2}-\d{2}$/.test(raw)) return false
    const d = new Date(`${raw}T00:00:00Z`)
    if (Number.isNaN(d.getTime())) return false
    const [y, m, day] = raw.split('-').map((n) => Number(n))
    return d.getUTCFullYear() === y && d.getUTCMonth() + 1 === m && d.getUTCDate() === day
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

  const submitLogin = async (e) => {
    e.preventDefault()
    const nextErrors = validateLogin()
    setFormErrors((p) => ({ ...p, login: nextErrors }))
    if (Object.keys(nextErrors).length > 0) return
    try {
      if (mfaChallengeId && loginForm.mfaCode?.trim()) {
        const mfaAuth = await api('/v1/auth/mfa/verify', { method: 'POST', body: JSON.stringify({ challengeId: mfaChallengeId, code: loginForm.mfaCode }), headers: { 'X-Tenant-Id': loginForm.tenantId.trim() } }, null, lang)
        saveAuth(mfaAuth)
        setMfaChallengeId('')
        setError('')
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
        setError(t('mfaPending'))
        return
      }
      saveAuth(d)
      setMfaChallengeId('')
    } catch (err) { handleError(err) }
  }

  const submitSsoLogin = async (e) => {
    e.preventDefault()
    const nextErrors = validateSso()
    setFormErrors((p) => ({ ...p, sso: nextErrors }))
    if (Object.keys(nextErrors).length > 0) return
    try { saveAuth(await api('/auth/sso/login', { method: 'POST', body: JSON.stringify(ssoForm) }, null, lang)) } catch (err) { handleError(err) }
  }

  const startOidcLogin = () => {
    if (!ssoConfig?.authorizeEndpoint || !ssoConfig?.clientId) { setError('OIDC config missing'); return }
    const state = Math.random().toString(36).slice(2) + Date.now().toString(36)
    localStorage.setItem(OIDC_STATE_KEY, state)
    const redirect = ssoConfig.redirectUri || window.location.origin
    const scope = ssoConfig.scope || 'openid profile email'
    const q = new URLSearchParams({ response_type: 'code', client_id: ssoConfig.clientId, redirect_uri: redirect, scope, state })
    setOidcAuthorizing(true)
    window.location.assign(`${ssoConfig.authorizeEndpoint}?${q}`)
  }

  const toggleTaskDone = async (task) => { if (!canWrite) return; try { await api('/tasks/' + task.id, { method: 'PATCH', body: JSON.stringify({ done: !task.done }) }, auth.token, lang); await loadTasks() } catch (err) { handleError(err) } }

  const saveCustomer = async () => {
    if (!canWrite) return
    setCrudError('customer', '')
    setCrudFieldError('customer', {})
    const localErrors = validateCustomerForm()
    if (Object.keys(localErrors).length > 0) {
      setCrudFieldError('customer', localErrors)
      setCrudError('customer', Object.values(localErrors)[0])
      return
    }
    try {
      const valueRaw = String(customerForm.value || '').trim()
      const payload = {
        name: String(customerForm.name || '').trim(),
        owner: String(customerForm.owner || '').trim(),
        status: String(customerForm.status || '').trim(),
        tag: String(customerForm.tag || '').trim(),
        value: valueRaw ? Number(valueRaw) : 0,
      }
      if (customerForm.id) await api('/customers/' + customerForm.id, { method: 'PATCH', body: JSON.stringify(payload) }, auth.token, lang)
      else await api('/customers', { method: 'POST', body: JSON.stringify(payload) }, auth.token, lang)
      setCustomerForm({ id: '', name: '', owner: '', status: '', tag: '', value: '' })
      await loadCustomers(customerPage, customerSize)
    } catch (err) { setCrudError('customer', formatValidation(err)); setCrudFieldError('customer', pickFieldErrors(err, ['name', 'owner', 'status', 'value'])); handleError(err) }
  }

  const editCustomer = (c) => setCustomerForm({ id: c.id, name: c.name || '', owner: c.owner || '', status: c.status || '', tag: c.tag || '', value: String(c.value || '') })
  const removeCustomer = async (id) => { if (!canDeleteCustomer) return; try { await api('/customers/' + id, { method: 'DELETE' }, auth.token, lang); const nextPage = customerPage > 1 && customers.length <= 1 ? customerPage - 1 : customerPage; await loadCustomers(nextPage, customerSize) } catch (err) { handleError(err) } }

  const saveOpportunity = async () => {
    if (!canWrite) return
    setCrudError('opportunity', '')
    setCrudFieldError('opportunity', {})
    try {
      const payload = { stage: opportunityForm.stage, count: Number(opportunityForm.count || 0), amount: Number(opportunityForm.amount || 0), progress: Number(opportunityForm.progress || 0), owner: opportunityForm.owner }
      if (opportunityForm.id) await api('/opportunities/' + opportunityForm.id, { method: 'PATCH', body: JSON.stringify(payload) }, auth.token, lang)
      else await api('/opportunities', { method: 'POST', body: JSON.stringify(payload) }, auth.token, lang)
      setOpportunityForm({ id: '', stage: '', count: '', amount: '', progress: '', owner: '' })
      await loadOpportunities(opportunityPage, opportunitySize)
    } catch (err) { setCrudError('opportunity', formatValidation(err)); setCrudFieldError('opportunity', pickFieldErrors(err, ['stage', 'count', 'amount', 'progress', 'owner'])); handleError(err) }
  }

  const editOpportunity = (o) => setOpportunityForm({ id: o.id, stage: o.stage || '', count: String(o.count || ''), amount: String(o.amount || ''), progress: String(o.progress || ''), owner: o.owner || '' })
  const removeOpportunity = async (id) => { if (!canDeleteOpportunity) return; try { await api('/opportunities/' + id, { method: 'DELETE' }, auth.token, lang); const nextPage = opportunityPage > 1 && opportunities.length <= 1 ? opportunityPage - 1 : opportunityPage; await loadOpportunities(nextPage, opportunitySize) } catch (err) { handleError(err) } }

  const saveFollowUp = async () => {
    if (!canWrite) return
    setCrudError('followUp', '')
    setCrudFieldError('followUp', {})
    try {
      const payload = { customerId: followUpForm.customerId, summary: followUpForm.summary, channel: followUpForm.channel, result: followUpForm.result, nextActionDate: followUpForm.nextActionDate }
      if (followUpForm.id) await api('/follow-ups/' + followUpForm.id, { method: 'PATCH', body: JSON.stringify(payload) }, auth.token, lang)
      else await api('/follow-ups', { method: 'POST', body: JSON.stringify(payload) }, auth.token, lang)
      setFollowUpForm({ id: '', customerId: '', summary: '', channel: '', result: '', nextActionDate: '' })
      await loadFollowUps()
    } catch (err) { setCrudError('followUp', formatValidation(err)); setCrudFieldError('followUp', pickFieldErrors(err, ['customerId', 'summary', 'channel', 'result', 'nextActionDate'])); handleError(err) }
  }

  const editFollowUp = (f) => setFollowUpForm({ id: f.id, customerId: f.customerId || '', summary: f.summary || '', channel: f.channel || '', result: f.result || '', nextActionDate: f.nextActionDate || '' })
  const removeFollowUp = async (id) => { if (!canWrite) return; try { await api('/follow-ups/' + id, { method: 'DELETE' }, auth.token, lang); await loadFollowUps() } catch (err) { handleError(err) } }

  const saveContact = async () => {
    if (!canWrite) return
    setCrudError('contact', '')
    setCrudFieldError('contact', {})
    const localErrors = validateContactForm()
    if (Object.keys(localErrors).length > 0) {
      setCrudFieldError('contact', localErrors)
      setCrudError('contact', Object.values(localErrors)[0])
      return
    }
    try {
      const payload = {
        customerId: String(contactForm.customerId || '').trim(),
        name: String(contactForm.name || '').trim(),
        title: String(contactForm.title || '').trim(),
        phone: String(contactForm.phone || '').trim(),
        email: String(contactForm.email || '').trim(),
      }
      if (contactForm.id) await api('/contacts/' + contactForm.id, { method: 'PATCH', body: JSON.stringify(payload) }, auth.token, lang)
      else await api('/contacts', { method: 'POST', body: JSON.stringify(payload) }, auth.token, lang)
      setContactForm({ id: '', customerId: '', name: '', title: '', phone: '', email: '' })
      await loadContacts(contactPage, contactSize)
    } catch (err) {
      setCrudError('contact', formatValidation(err))
      setCrudFieldError('contact', pickFieldErrors(err, ['customerId', 'name', 'title', 'phone', 'email']))
      handleError(err)
    }
  }

  const editContact = (c) => setContactForm({ id: c.id, customerId: c.customerId || '', name: c.name || '', title: c.title || '', phone: c.phone || '', email: c.email || '' })
  const removeContact = async (id) => { if (!canWrite) return; try { await api('/contacts/' + id, { method: 'DELETE' }, auth.token, lang); const nextPage = contactPage > 1 && contacts.length <= 1 ? contactPage - 1 : contactPage; await loadContacts(nextPage, contactSize) } catch (err) { handleError(err) } }

  const saveContract = async () => {
    if (!canWrite) return
    setCrudError('contract', '')
    setCrudFieldError('contract', {})
    const localErrors = validateContractForm()
    if (Object.keys(localErrors).length > 0) {
      setCrudFieldError('contract', localErrors)
      setCrudError('contract', Object.values(localErrors)[0])
      return
    }
    try {
      const amountRaw = String(contractForm.amount || '').trim()
      const payload = {
        customerId: String(contractForm.customerId || '').trim(),
        contractNo: String(contractForm.contractNo || '').trim(),
        title: String(contractForm.title || '').trim(),
        amount: amountRaw ? Number(amountRaw) : 0,
        status: String(contractForm.status || '').trim(),
        signDate: String(contractForm.signDate || '').trim(),
      }
      if (contractForm.id) await api('/contracts/' + contractForm.id, { method: 'PATCH', body: JSON.stringify(payload) }, auth.token, lang)
      else await api('/contracts', { method: 'POST', body: JSON.stringify(payload) }, auth.token, lang)
      setContractForm({ id: '', customerId: '', contractNo: '', title: '', amount: '', status: '', signDate: '' })
      await loadContracts(contractPage, contractSize)
    } catch (err) { setCrudError('contract', formatValidation(err)); setCrudFieldError('contract', pickFieldErrors(err, ['customerId', 'contractNo', 'title', 'amount', 'status', 'signDate'])); handleError(err) }
  }

  const editContract = (c) => setContractForm({ id: c.id, customerId: c.customerId || '', contractNo: c.contractNo || '', title: c.title || '', amount: String(c.amount || ''), status: c.status || '', signDate: c.signDate || '' })
  const removeContract = async (id) => { if (!canDeleteCustomer) return; try { await api('/contracts/' + id, { method: 'DELETE' }, auth.token, lang); const nextPage = contractPage > 1 && contracts.length <= 1 ? contractPage - 1 : contractPage; await loadContracts(nextPage, contractSize) } catch (err) { handleError(err) } }

  const savePayment = async () => {
    if (!canWrite) return
    setCrudError('payment', '')
    setCrudFieldError('payment', {})
    const localErrors = validatePaymentForm()
    if (Object.keys(localErrors).length > 0) {
      setCrudFieldError('payment', localErrors)
      setCrudError('payment', Object.values(localErrors)[0])
      return
    }
    try {
      const amountRaw = String(paymentForm.amount || '').trim()
      const payload = {
        contractId: String(paymentForm.contractId || '').trim(),
        amount: amountRaw ? Number(amountRaw) : 0,
        receivedDate: String(paymentForm.receivedDate || '').trim(),
        method: String(paymentForm.method || '').trim(),
        status: String(paymentForm.status || '').trim(),
        remark: String(paymentForm.remark || '').trim(),
      }
      if (paymentForm.id) await api('/payments/' + paymentForm.id, { method: 'PATCH', body: JSON.stringify(payload) }, auth.token, lang)
      else await api('/payments', { method: 'POST', body: JSON.stringify(payload) }, auth.token, lang)
      setPaymentForm({ id: '', contractId: '', amount: '', receivedDate: '', method: '', status: '', remark: '' })
      await loadPayments(paymentPage, paymentSize)
    } catch (err) { setCrudError('payment', formatValidation(err)); setCrudFieldError('payment', pickFieldErrors(err, ['contractId', 'amount', 'receivedDate', 'method', 'status', 'remark'])); handleError(err) }
  }

  const editPayment = (p) => setPaymentForm({ id: p.id, contractId: p.contractId || '', amount: String(p.amount || ''), receivedDate: p.receivedDate || '', method: p.method || '', status: p.status || '', remark: p.remark || '' })
  const removePayment = async (id) => { if (!canDeleteCustomer) return; try { await api('/payments/' + id, { method: 'DELETE' }, auth.token, lang); const nextPage = paymentPage > 1 && payments.length <= 1 ? paymentPage - 1 : paymentPage; await loadPayments(nextPage, paymentSize) } catch (err) { handleError(err) } }

  const createExportJob = async () => {
    try {
      if (hasInvalidAuditRange()) { setAuditRangeError(t('dateRangeInvalid')); return }
      setAuditRangeError('')
      const q = new URLSearchParams({ username: auditUser, role: auditRole, action: auditAction, from: auditFrom, to: auditTo })
      const job = await api('/audit-logs/export-jobs?' + q, { method: 'POST' }, auth.token, lang)
      setExportJobs((prev) => [{ ...job }, ...prev].slice(0, 8))
    } catch (err) { handleError(err) }
  }

  const retryExportJob = async (jobId) => {
    try { const job = await api('/audit-logs/export-jobs/' + jobId + '/retry', { method: 'POST' }, auth.token, lang); setExportJobs((prev) => [{ ...job }, ...prev].slice(0, 8)) } catch (err) { handleError(err) }
  }

  const downloadExportJob = async (jobId) => {
    try {
      const res = await fetch(`${API_BASE}/audit-logs/export-jobs/${jobId}/download`, { headers: { Authorization: `Bearer ${auth.token}`, 'Accept-Language': lang } })
      if (!res.ok) throw new Error(t('downloadFailed'))
      const blob = await res.blob(); const url = URL.createObjectURL(blob)
      const a = document.createElement('a'); a.href = url; a.download = `audit-${jobId}.csv`; document.body.appendChild(a); a.click(); a.remove(); URL.revokeObjectURL(url)
    } catch (err) { handleError(err) }
  }


  const createReportExportJob = async () => {
    if (!canViewReports) return
    try {
      if (hasInvalidAuditRange()) { setAuditRangeError(t('dateRangeInvalid')); return }
      setAuditRangeError('')
      const q = new URLSearchParams({ from: auditFrom, to: auditTo, role: auditRole, owner: reportOwner, department: reportDepartment, timezone: reportTimezone, currency: reportCurrency })
      const job = await api('/v1/reports/export-jobs?' + q, { method: 'POST' }, auth.token, lang)
      setReportExportJobs((prev) => [{ ...job }, ...prev].slice(0, 8))
    } catch (err) { handleError(err) }
  }

  const retryReportExportJob = async (jobId) => {
    try { const job = await api('/v1/reports/export-jobs/' + jobId + '/retry', { method: 'POST' }, auth.token, lang); setReportExportJobs((prev) => [{ ...job }, ...prev].slice(0, 8)) } catch (err) { handleError(err) }
  }

  const downloadReportExportJob = async (jobId) => {
    try {
      const res = await fetch(`${API_BASE}/v1/reports/export-jobs/${jobId}/download`, { headers: { Authorization: `Bearer ${auth.token}`, 'Accept-Language': lang } })
      if (!res.ok) throw new Error(t('downloadFailed'))
      const blob = await res.blob(); const url = URL.createObjectURL(blob)
      const a = document.createElement('a'); a.href = url; a.download = `report-${jobId}.csv`; document.body.appendChild(a); a.click(); a.remove(); URL.revokeObjectURL(url)
    } catch (err) { handleError(err) }
  }

  const exportReportCsv = async () => { await createReportExportJob() }
  const changePermission = async (roleKey, opKey, grant) => { if (!canManagePermissions) return; try { const payload = grant ? { grant: [opKey], revoke: [] } : { grant: [], revoke: [opKey] }; const d = await api('/permissions/roles/' + roleKey, { method: 'PATCH', body: JSON.stringify(payload) }, auth.token, lang); setPermissionMatrix(d.matrix || []); await loadPermissionConflicts() } catch (err) { handleError(err) } }
  const previewPermissionPack = async (type) => { if (!canManagePermissions) return; try { const payload = type === 'grant-read' ? { grant: READ_OPS, revoke: [] } : { grant: [], revoke: WRITE_OPS }; const d = await api('/permissions/roles/' + permissionRole + '/preview', { method: 'POST', body: JSON.stringify(payload) }, auth.token, lang); setPermissionPreview(d); setPendingPack(type) } catch (err) { handleError(err) } }
  const applyPermissionPack = async (type) => { if (!canManagePermissions) return; try { const payload = type === 'grant-read' ? { grant: READ_OPS, revoke: [] } : { grant: [], revoke: WRITE_OPS }; const d = await api('/permissions/roles/' + permissionRole, { method: 'PATCH', body: JSON.stringify(payload) }, auth.token, lang); setPermissionMatrix(d.matrix || []); await loadPermissionConflicts() } catch (err) { handleError(err) } }
  const commitPendingPack = async () => { if (!pendingPack) return; await applyPermissionPack(pendingPack); setPendingPack(''); setPermissionPreview(null) }
  const rollbackPermissionRole = async () => { try { const d = await api('/permissions/roles/' + permissionRole + '/rollback', { method: 'POST' }, auth.token, lang); setPermissionMatrix(d.matrix || []); await loadPermissionConflicts(); setPendingPack(''); setPermissionPreview(null) } catch (err) { handleError(err) } }

  const saveAdminUser = async (u) => {
    const roleValue = String(u.role || '').trim().toUpperCase()
    const ownerScopeValue = String(u.ownerScope || '').trim()
    if (!ROLES.includes(roleValue)) { setError(t('invalidRoleText')); return }
    if (roleValue === 'SALES' && !ownerScopeValue) { setError(t('ownerScopeRequired')); return }
    if (ownerScopeValue.length > 64) { setError(t('ownerScopeTooLong')); return }
    try { const d = await api('/v1/admin/users/' + u.id, { method: 'PATCH', body: JSON.stringify({ role: roleValue, ownerScope: ownerScopeValue, enabled: !!u.enabled }) }, auth.token, lang); setAdminUsers((prev) => prev.map((x) => x.id === d.id ? d : x)); setError('') } catch (err) { handleError(err) }
  }

  const unlockAdminUser = async (id) => { try { const d = await api('/v1/admin/users/' + id + '/unlock', { method: 'POST' }, auth.token, lang); setAdminUsers((prev) => prev.map((x) => x.id === d.id ? d : x)) } catch (err) { handleError(err) } }
  const getAdminUserError = (u) => { const roleValue = String(u?.role || '').trim().toUpperCase(); const ownerScopeValue = String(u?.ownerScope || '').trim(); if (!ROLES.includes(roleValue)) return t('invalidRoleText'); if (roleValue === 'SALES' && !ownerScopeValue) return t('ownerScopeRequired'); if (ownerScopeValue.length > 64) return t('ownerScopeTooLong'); return '' }
  const inviteUser = async () => {
    try {
      const invited = await api('/v1/admin/users/invite', { method: 'POST', body: JSON.stringify(inviteForm) }, auth.token, lang)
      setInviteResult(invited)
      setInviteForm((p) => ({ ...p, username: '', ownerScope: '' }))
      await loadAdminUsers()
    } catch (err) { handleError(err) }
  }

  const createApprovalTemplate = async () => {
    try {
      await api('/v1/approval/templates', {
        method: 'POST',
        body: JSON.stringify({
          bizType: approvalTemplateForm.bizType,
          name: approvalTemplateForm.name,
          approverRoles: approvalTemplateForm.approverRoles,
        }),
      }, auth.token, lang)
      setApprovalTemplateForm((p) => ({ ...p, name: '' }))
      await loadApprovalTemplates()
      await loadApprovalStats()
      await loadApprovalInstances()
      await loadApprovalTasks()
    } catch (err) { handleError(err) }
  }

  const submitApprovalInstance = async () => {
    try {
      await api(`/v1/approval/instances/${approvalInstanceForm.bizType}/${approvalInstanceForm.bizId}/submit`, {
        method: 'POST',
        body: JSON.stringify({
          amount: Number(approvalInstanceForm.amount || 0),
          role: role,
          department: auth?.department || 'DEFAULT',
          comment: 'Submitted from web panel',
        }),
      }, auth.token, lang)
      setApprovalInstanceForm((p) => ({ ...p, bizId: '', amount: '' }))
      await loadApprovalStats()
      await loadApprovalInstances()
      await loadApprovalTasks()
    } catch (err) { handleError(err) }
  }

  const updateApprovalTemplate = async (row) => {
    try {
      const updated = await api('/v1/approval/templates/' + row.id, {
        method: 'PATCH',
        body: JSON.stringify({
          name: row.name,
          amountMin: row.amountMin === '' || row.amountMin === null || row.amountMin === undefined ? null : Number(row.amountMin),
          amountMax: row.amountMax === '' || row.amountMax === null || row.amountMax === undefined ? null : Number(row.amountMax),
          role: row.role || '',
          department: row.department || '',
          approverRoles: row.approverRoles || '',
          flowDefinition: row.flowDefinition || null,
          status: row.status || undefined,
          version: row.version || undefined,
          enabled: !!row.enabled,
        }),
      }, auth.token, lang)
      setApprovalTemplates((prev) => prev.map((x) => x.id === updated.id ? updated : x))
      await loadApprovalStats()
    } catch (err) { handleError(err) }
  }

  const actApprovalTask = async (taskId, action) => {
    try {
      const payload = { comment: approvalActionComment }
      if (action === 'transfer') payload.transferTo = approvalTransferTo
      await api('/v1/approval/tasks/' + taskId + '/' + action, { method: 'POST', body: JSON.stringify(payload) }, auth.token, lang)
      await loadApprovalStats()
      await loadApprovalTasks()
      await loadApprovalInstances()
      setApprovalActionComment('')
      if (action === 'transfer') setApprovalTransferTo('')
    } catch (err) { handleError(err) }
  }

  const urgeApprovalTask = async (taskId) => {
    try {
      await api('/v1/approval/tasks/' + taskId + '/urge', { method: 'POST', body: JSON.stringify({ comment: approvalActionComment, urgeChannel: 'IN_APP' }) }, auth.token, lang)
      await loadApprovalStats()
      await loadApprovalTasks()
      await loadNotificationJobs()
      setApprovalActionComment('')
    } catch (err) { handleError(err) }
  }

  const publishApprovalTemplate = async (templateId) => {
    try {
      await api('/v1/approval/templates/' + templateId + '/publish', { method: 'POST' }, auth.token, lang)
      await loadApprovalTemplates()
      await loadApprovalStats()
      await loadApprovalTemplateVersions(templateId)
    } catch (err) { handleError(err) }
  }

  const rollbackApprovalTemplate = async (templateId, version) => {
    try {
      await api('/v1/approval/templates/' + templateId + '/rollback/' + version, { method: 'POST' }, auth.token, lang)
      await loadApprovalTemplates()
      await loadApprovalStats()
      await loadApprovalTemplateVersions(templateId)
    } catch (err) { handleError(err) }
  }

  const retryNotificationJob = async (jobId) => {
    try {
      await api('/v1/integrations/notifications/jobs/' + jobId + '/retry', { method: 'POST' }, auth.token, lang)
      await loadNotificationJobs()
    } catch (err) { handleError(err) }
  }

  const toggleNotificationJob = (jobId, checked) => {
    setSelectedNotificationJobs((prev) => {
      const set = new Set(prev)
      if (checked) set.add(jobId)
      else set.delete(jobId)
      return Array.from(set)
    })
  }

  const toggleAllNotificationJobs = (checked) => {
    if (!checked) { setSelectedNotificationJobs([]); return }
    setSelectedNotificationJobs((notificationJobs || []).map((j) => j.jobId))
  }

  const retryNotificationJobsByIds = async () => {
    try {
      if ((selectedNotificationJobs || []).length === 0) return
      const result = await api('/v1/integrations/notifications/jobs/batch-retry', {
        method: 'POST',
        body: JSON.stringify({ jobIds: selectedNotificationJobs }),
      }, auth.token, lang)
      setError(`${t('retry')}: requested=${result.requested}, succeeded=${result.succeeded}, skipped=${result.skipped}`)
      await loadNotificationJobs(notificationPage, notificationSize)
    } catch (err) { handleError(err) }
  }

  const retryNotificationJobsByFilter = async () => {
    try {
      const result = await api('/v1/integrations/notifications/jobs/retry-by-filter', {
        method: 'POST',
        body: JSON.stringify({ status: notificationStatusFilter, page: notificationPage, size: notificationSize }),
      }, auth.token, lang)
      setError(`${t('retry')}: requested=${result.requested}, succeeded=${result.succeeded}, skipped=${result.skipped}`)
      await loadNotificationJobs(notificationPage, notificationSize)
    } catch (err) { handleError(err) }
  }

  const createTenant = async () => {
    try {
      const created = await api('/v1/tenants', {
        method: 'POST',
        body: JSON.stringify({
          name: tenantForm.name,
          status: tenantForm.status,
          quotaUsers: Number(tenantForm.quotaUsers || 100),
          timezone: tenantForm.timezone,
          currency: tenantForm.currency,
          dateFormat: 'YYYY-MM-DD',
        }),
      }, auth.token, lang)
      setLastCreatedTenant(created)
      setTenantForm((p) => ({ ...p, name: '' }))
      await loadTenants()
    } catch (err) { handleError(err) }
  }

  const updateTenant = async (row) => {
    try {
      const updated = await api('/v1/tenants/' + row.id, {
        method: 'PATCH',
        body: JSON.stringify({
          name: row.name,
          status: row.status,
          quotaUsers: Number(row.quotaUsers || 0),
          timezone: row.timezone,
          currency: row.currency,
          dateFormat: row.dateFormat || 'YYYY-MM-DD',
        }),
      }, auth.token, lang)
      setTenantRows((prev) => prev.map((x) => x.id === updated.id ? updated : x))
    } catch (err) { handleError(err) }
  }

  const onCustomerPageChange = async (next) => { if (!auth?.token) return; try { await loadCustomers(next, customerSize) } catch (err) { handleError(err) } }
  const onOpportunityPageChange = async (next) => { if (!auth?.token) return; try { await loadOpportunities(next, opportunitySize) } catch (err) { handleError(err) } }
  const onContactPageChange = async (next) => { if (!auth?.token) return; try { await loadContacts(next, contactSize) } catch (err) { handleError(err) } }
  const onContractPageChange = async (next) => { if (!auth?.token) return; try { await loadContracts(next, contractSize) } catch (err) { handleError(err) } }
  const onPaymentPageChange = async (next) => { if (!auth?.token) return; try { await loadPayments(next, paymentSize) } catch (err) { handleError(err) } }
  const onCustomerSizeChange = async (nextSize) => { if (!auth?.token) return; setCustomerSize(nextSize); try { await loadCustomers(1, nextSize) } catch (err) { handleError(err) } }
  const onOpportunitySizeChange = async (nextSize) => { if (!auth?.token) return; setOpportunitySize(nextSize); try { await loadOpportunities(1, nextSize) } catch (err) { handleError(err) } }
  const onContactSizeChange = async (nextSize) => { if (!auth?.token) return; setContactSize(nextSize); try { await loadContacts(1, nextSize) } catch (err) { handleError(err) } }
  const onContractSizeChange = async (nextSize) => { if (!auth?.token) return; setContractSize(nextSize); try { await loadContracts(1, nextSize) } catch (err) { handleError(err) } }
  const onPaymentSizeChange = async (nextSize) => { if (!auth?.token) return; setPaymentSize(nextSize); try { await loadPayments(1, nextSize) } catch (err) { handleError(err) } }

  const currentPageLabel = navItems.find((item) => item.key === activePage)?.label || t('salesOverview')
  const navGroups = navItems.reduce((acc, item) => { const key = item.group || 'Default'; if (!acc[key]) acc[key] = []; acc[key].push(item); return acc }, {})
  const onNavigate = (key) => {
    setActivePage(key)
    navigate(PAGE_TO_PATH[key] || PAGE_TO_PATH.dashboard)
  }

  const mainBase = { currentPageLabel, lang, setLang, loadAll, t, canWrite, error, loading, activePage, stats, reports, canViewReports, auditFrom, auditTo, auditRole, reportOwner, setReportOwner, reportDepartment, setReportDepartment, reportTimezone, setReportTimezone, reportCurrency, setReportCurrency, exportReportCsv, reportExportJobs, reportExportStatusFilter, setReportExportStatusFilter, autoRefreshReportJobs, setAutoRefreshReportJobs, loadReportExportJobs, retryReportExportJob, downloadReportExportJob }
  const mainPermissions = { permissionRole, setPermissionRole, canManagePermissions, previewPermissionPack, pendingPack, commitPendingPack, rollbackPermissionRole, permissionPreview, permissionMatrix, changePermission, permissionConflicts }
  const mainUsers = { canManageUsers, adminUsers, loadAdminUsers, setAdminUsers, getAdminUserError, saveAdminUser, unlockAdminUser, inviteForm, setInviteForm, inviteUser, inviteResult }
  const mainCustomers = { customerForm, setCustomerForm, saveCustomer, formError: crudErrors.customer, fieldErrors: crudFieldErrors.customer, customers, editCustomer, canDeleteCustomer, removeCustomer, customerQ, setCustomerQ, customerStatus, setCustomerStatus, pagination: { page: customerPage, totalPages: customerTotalPages, size: customerSize }, onPageChange: onCustomerPageChange, onSizeChange: onCustomerSizeChange, reload: loadCustomers }
  const mainPipeline = { opportunityForm, setOpportunityForm, saveOpportunity, formError: crudErrors.opportunity, fieldErrors: crudFieldErrors.opportunity, opportunities, editOpportunity, canDeleteOpportunity, removeOpportunity, oppStage, setOppStage, pagination: { page: opportunityPage, totalPages: opportunityTotalPages, size: opportunitySize }, onPageChange: onOpportunityPageChange, onSizeChange: onOpportunitySizeChange, reload: loadOpportunities }
  const mainFollowUps = { followUpForm, setFollowUpForm, saveFollowUp, formError: crudErrors.followUp, fieldErrors: crudFieldErrors.followUp, followUps, editFollowUp, removeFollowUp }
  const mainContacts = { contactForm, setContactForm, saveContact, formError: crudErrors.contact, fieldErrors: crudFieldErrors.contact, contacts, editContact, removeContact, contactQ, setContactQ, pagination: { page: contactPage, totalPages: contactTotalPages, size: contactSize }, onPageChange: onContactPageChange, onSizeChange: onContactSizeChange, reload: loadContacts }
  const mainContracts = { contractForm, setContractForm, saveContract, formError: crudErrors.contract, fieldErrors: crudFieldErrors.contract, contracts, editContract, removeContract, contractQ, setContractQ, contractStatus, setContractStatus, pagination: { page: contractPage, totalPages: contractTotalPages, size: contractSize }, onPageChange: onContractPageChange, onSizeChange: onContractSizeChange, reload: loadContracts }
  const mainPayments = { paymentForm, setPaymentForm, savePayment, formError: crudErrors.payment, fieldErrors: crudFieldErrors.payment, payments, editPayment, removePayment, paymentStatus, setPaymentStatus, pagination: { page: paymentPage, totalPages: paymentTotalPages, size: paymentSize }, onPageChange: onPaymentPageChange, onSizeChange: onPaymentSizeChange, reload: loadPayments }
  const mainTasks = { tasks, toggleTaskDone }
  const mainAudit = { canViewAudit, auditUser, setAuditUser, auditRole, setAuditRole, auditAction, setAuditAction, auditFrom, setAuditFrom, auditTo, setAuditTo, auditRangeError, setAuditRangeError, hasInvalidAuditRange, loadAudit, createExportJob, loadExportJobs, autoRefreshJobs, setAutoRefreshJobs, auditLogs, exportStatusFilter, setExportStatusFilter, exportJobs, downloadExportJob, retryExportJob }
  const mainApprovals = {
    template: approvalTemplateForm,
    setTemplate: setApprovalTemplateForm,
    templates: approvalTemplates,
    setTemplates: setApprovalTemplates,
    updateTemplate: updateApprovalTemplate,
    publishTemplate: publishApprovalTemplate,
    rollbackTemplate: rollbackApprovalTemplate,
    versions: approvalTemplateVersions,
    loadVersions: loadApprovalTemplateVersions,
    versionTemplateId: approvalVersionTemplateId,
    stats: approvalStats,
    instance: approvalInstanceForm,
    setInstance: setApprovalInstanceForm,
    createTemplate: createApprovalTemplate,
    submitInstance: submitApprovalInstance,
    tasks: approvalTasks,
    instances: approvalInstances,
    detail: approvalDetail,
    loadDetail: loadApprovalDetail,
    taskStatus: approvalTaskStatus,
    setTaskStatus: setApprovalTaskStatus,
    overdueOnly: approvalOverdueOnly,
    setOverdueOnly: setApprovalOverdueOnly,
    escalatedOnly: approvalEscalatedOnly,
    setEscalatedOnly: setApprovalEscalatedOnly,
    actionComment: approvalActionComment,
    setActionComment: setApprovalActionComment,
    transferTo: approvalTransferTo,
    setTransferTo: setApprovalTransferTo,
    actTask: actApprovalTask,
    urgeTask: urgeApprovalTask,
    notifications: notificationJobs,
    notificationStatus: notificationStatusFilter,
    setNotificationStatus: setNotificationStatusFilter,
    notificationPage,
    notificationTotalPages,
    notificationSize,
    setNotificationPage,
    setNotificationSize,
    selectedNotificationJobs,
    toggleNotificationJob,
    toggleAllNotificationJobs,
    retryNotification: retryNotificationJob,
    retryNotificationByIds: retryNotificationJobsByIds,
    retryNotificationByFilter: retryNotificationJobsByFilter,
    reloadTasks: loadApprovalTasks,
    reloadTemplates: loadApprovalTemplates,
    reloadNotifications: loadNotificationJobs,
    canRetryNotifications: ['ADMIN', 'MANAGER'].includes(role),
  }
  const mainTenants = { form: tenantForm, setForm: setTenantForm, createTenant, rows: tenantRows, setRows: setTenantRows, updateTenant, reload: loadTenants, lastCreated: lastCreatedTenant }

  if (!auth && location.pathname === '/activate') {
    return <InvitationAcceptView lang={lang} setLang={setLang} t={t} onBackToLogin={() => navigate('/login')} />
  }

  if (!auth) {
    return (
      <LoginView
        lang={lang}
        setLang={setLang}
        t={t}
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
        openActivate={() => navigate('/activate')}
        error={error}
      />
    )
  }

  return (
    <div className="app-shell">
      <SidebarNav auth={auth} navGroups={navGroups} activePage={activePage} onNavigate={onNavigate} saveAuth={saveAuth} t={t} />
      <MainContent
        base={mainBase}
        permissions={mainPermissions}
        users={mainUsers}
        customers={mainCustomers}
        pipeline={mainPipeline}
        followUps={mainFollowUps}
        contacts={mainContacts}
        contracts={mainContracts}
        payments={mainPayments}
        tasks={mainTasks}
        audit={mainAudit}
        approvals={mainApprovals}
        tenants={mainTenants}
      />
    </div>
  )
}

export default App
















