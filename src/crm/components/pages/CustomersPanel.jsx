import { memo, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { api, CUSTOMER_STATUS_OPTIONS, formatMoney, formatDateTime, translateStatus, translateStage } from '../../shared'
import ListState from '../ListState'
import RowDetailDrawer from '../RowDetailDrawer'
import ServerPager from '../ServerPager'
import { useBatchActions } from '../useBatchActions'
import BatchResultModal from '../BatchResultModal'
import VirtualListTable from '../VirtualListTable'
import { useSelectionSet } from '../../hooks/useSelectionSet'

const MAX_ASSOC_ITEMS = 8
const ONE_DAY_MS = 24 * 60 * 60 * 1000
const THIRTY_DAYS_MS = 30 * ONE_DAY_MS
const CUSTOMER360_MODULE_KEYS = [
  'contacts',
  'opportunities',
  'quotes',
  'orders',
  'contracts',
  'payments',
  'approvals',
  'audits',
  'notifications',
]
const CUSTOMER360_PRIMARY_MODULES = [
  'contacts',
  'opportunities',
  'quotes',
  'orders',
  'contracts',
  'payments',
]
const CUSTOMER360_SECONDARY_MODULES = ['approvals', 'audits', 'notifications']
const CUSTOMER360_PREFETCH_MODULES = ['contacts', 'opportunities', 'quotes', 'orders']
const CUSTOMER360_MODULE_TTL_MS = 45 * 1000
const CUSTOMER360_ADAPTIVE_WINDOW = 8
const CUSTOMER360_PREFETCH_LOW_HIT = 25
const CUSTOMER360_PREFETCH_HIGH_HIT = 70
const CUSTOMER360_PREFETCH_HIGH_LATENCY = 450
const CUSTOMER360_ACTION_MODULES = {
  followup: ['timeline', 'audits'],
  task: ['timeline', 'audits'],
  quote: ['quotes', 'approvals'],
  urgeApproval: ['approvals', 'notifications'],
  manualRefresh: CUSTOMER360_MODULE_KEYS,
}

function createCustomer360ModuleMeta() {
  return CUSTOMER360_MODULE_KEYS.reduce((acc, key) => {
    acc[key] = {
      loading: false,
      error: '',
      lastLoadedAt: 0,
      signature: '',
    }
    return acc
  }, {})
}

function buildCustomer360ModuleSignature(customerIdText, moduleName, serverFilterSignature = '') {
  return `${customerIdText}:${moduleName}:v1:${serverFilterSignature || 'default'}`
}

function copyIdToClipboard(value) {
  if (value === null || value === undefined) return
  const text = String(value)
  if (!text) return
  if (navigator?.clipboard?.writeText) {
    navigator.clipboard.writeText(text)
    return
  }
  const input = document.createElement('textarea')
  input.value = text
  input.setAttribute('readonly', 'true')
  input.style.position = 'absolute'
  input.style.left = '-9999px'
  document.body.appendChild(input)
  input.select()
  document.execCommand('copy')
  document.body.removeChild(input)
}

const CustomerRow = memo(function CustomerRow({ row, checked, onToggle, t, openDetail, editCustomer, canDeleteCustomer, removeCustomer }) {
  return (
    <div className="table-row table-row-6">
      <span><input type="checkbox" checked={checked} onChange={onToggle} /></span>
      <span>{row.name}</span>
      <span>{row.owner}</span>
      <span>{row.statusLabel}</span>
      <span>{row.valueText}</span>
      <span>
        <button className="mini-btn" onClick={() => openDetail(row)}>{t('detail')}</button>
        <button className="mini-btn" onClick={() => editCustomer(row)}>{t('save')}</button>
        {canDeleteCustomer ? <button className="danger-btn" onClick={() => removeCustomer(row.id)}>{t('delete')}</button> : null}
      </span>
    </div>
  )
})

const Customer360ItemActions = memo(function Customer360ItemActions({ t, onOpen, onCopyId }) {
  return (
    <div className="crm360-item-actions">
      {onOpen ? <button className="mini-btn" onClick={onOpen}>{t('openSource')}</button> : null}
      {onCopyId ? <button className="mini-btn" onClick={onCopyId}>{t('copyId')}</button> : null}
    </div>
  )
})

const Customer360SectionRow = memo(function Customer360SectionRow({
  item,
  t,
  mainText,
  statusText,
  timeText,
  onOpen,
  onCopyId,
  onQuickAction,
  quickLabel,
}) {
  return (
    <div className="table-row table-row-4 crm360-table-row">
      <span>{mainText || '-'}</span>
      <span>{statusText || '-'}</span>
      <span>{timeText || '-'}</span>
      <span>
        <Customer360ItemActions t={t} onOpen={onOpen ? () => onOpen(item) : null} onCopyId={onCopyId ? () => onCopyId(item) : null} />
        {onQuickAction && quickLabel ? <button className="mini-btn" onClick={() => onQuickAction(item)}>{quickLabel}</button> : null}
      </span>
    </div>
  )
})

const Customer360SectionTable = memo(function Customer360SectionTable({
  t,
  title,
  items,
  emptyText,
  buildMainText,
  buildStatusText,
  buildTimeText,
  onOpen,
  onCopyId,
  onQuickAction,
  quickLabel,
  loading,
  error,
  onRetry,
}) {
  const rows = useMemo(() => toArray(items), [items])
  const viewportHeight = useMemo(
    () => Math.min(240, Math.max(56, rows.length * 52)),
    [rows.length],
  )
  return (
    <section className="crm360-section">
      <div className="crm360-section-head"><h4>{title}</h4></div>
      {loading ? <div className="loading">{t('loading')}</div> : null}
      {!loading && error ? (
        <div className="empty-tip">
          {error}
          {onRetry ? <button className="mini-btn" onClick={onRetry}>{t('refresh')}</button> : null}
        </div>
      ) : null}
      {!loading && !error && !rows.length ? <div className="empty-tip">{emptyText}</div> : null}
      {!loading && !error && !!rows.length && (
        <>
          <div className="table-row table-head-row table-row-4 crm360-table-head">
            <span>{t('title')}</span>
            <span>{t('status')}</span>
            <span>{t('createdAt')}</span>
            <span>{t('action')}</span>
          </div>
          <VirtualListTable
            rows={rows}
            rowHeight={52}
            viewportHeight={viewportHeight}
            getRowKey={(row, index) => row?.id || row?.sourceId || `${title}-${index}`}
            renderRow={(item, _index, key) => (
              <Customer360SectionRow
                key={key}
                item={item}
                t={t}
                mainText={buildMainText(item)}
                statusText={buildStatusText(item)}
                timeText={buildTimeText(item)}
                onOpen={onOpen}
                onCopyId={onCopyId}
                onQuickAction={onQuickAction}
                quickLabel={quickLabel}
              />
            )}
          />
        </>
      )}
    </section>
  )
})

const Customer360View = memo(function Customer360View({
  t,
  customer,
  vm,
  loading,
  moduleMeta,
  mode,
  onBackToList,
  onOpenFullDetail,
  onQuickCreateFollowUp,
  onQuickCreateTask,
  onQuickCreateQuote,
  onQuickViewOrders,
  onQuickUrgeApproval,
  onNavigatePrev,
  onNavigateNext,
  onNavigateTarget,
  onRefreshModules,
  onRefresh,
}) {
  const headerClass = mode === 'page' ? 'crm360-header crm360-header-page' : 'crm360-header'
  const related = vm.related
  return (
    <div className={mode === 'page' ? 'crm360-page' : 'crm360-drawer'}>
      <div className={headerClass}>
        <div>
          <h3>{customer?.name || `${t('customers')} #${customer?.id || '-'}`}</h3>
          <div className="crm360-subtitle">
            {t('owner')}: {customer?.owner || '-'} | {t('status')}: {translateStatus(t, customer?.status)}
          </div>
        </div>
        <div className="crm360-head-actions">
          {mode === 'drawer' ? <button className="mini-btn" onClick={onNavigatePrev}>{t('pagePrev')}</button> : null}
          {mode === 'drawer' ? <button className="mini-btn" onClick={onNavigateNext}>{t('pageNext')}</button> : null}
          {mode === 'drawer' ? <button className="mini-btn" onClick={onOpenFullDetail}>{t('customer360OpenPage')}</button> : null}
          {mode === 'page' ? <button className="mini-btn" onClick={onBackToList}>{t('customer360BackToList')}</button> : null}
          <button className="mini-btn" onClick={onQuickCreateFollowUp}>{t('quickCreateFollowUp')}</button>
          <button className="mini-btn" onClick={onQuickCreateTask}>{t('quickCreateTask')}</button>
          <button className="mini-btn" onClick={onQuickCreateQuote}>{t('quickCreateQuote')}</button>
          <button className="mini-btn" onClick={onQuickViewOrders}>{t('quickViewOrders')}</button>
          <button className="mini-btn" onClick={onQuickUrgeApproval}>{t('quickUrgeApproval')}</button>
          <button className="mini-btn" onClick={onRefresh}>{t('refresh')}</button>
        </div>
      </div>
      {loading ? <div className="loading">{t('loading')}</div> : null}

      <div className="crm360-kpis">
        <div className="kpi"><strong>{t('recent30FollowUps')}</strong><span>{vm.metrics.recentFollowUps30d}</span></div>
        <div className="kpi"><strong>{t('inFlightOpportunityAmount')}</strong><span>{formatMoney(vm.metrics.inFlightAmount)}</span></div>
        <div className="kpi"><strong>{t('orderTarget')}</strong><span>{formatMoney(vm.metrics.orderAmount)}</span></div>
        <div className="kpi"><strong>{t('paymentReceived')}</strong><span>{formatMoney(vm.metrics.paymentReceived)}</span></div>
        <div className="kpi"><strong>{t('paymentOutstanding')}</strong><span>{formatMoney(vm.metrics.paymentOutstanding)}</span></div>
        <div className="kpi"><strong>{t('pendingApprovals')}</strong><span>{vm.metrics.pendingApprovals}</span></div>
      </div>

      <div className="crm360-risk-tags">
        <strong>{t('riskTags')}:</strong>
        {!vm.riskTags.length ? <span>{t('noRiskTags')}</span> : null}
        {vm.riskTags.map((tag) => <span className="crm360-risk-tag" key={tag}>{tag}</span>)}
      </div>

      <div className="crm360-grid">
        <Customer360SectionTable
          t={t}
          title={t('contacts')}
          items={related.contacts}
          emptyText={t('noData')}
          buildMainText={(item) => `${item.name || '-'} (${item.title || '-'})`}
          buildStatusText={() => '-'}
          buildTimeText={(item) => formatDateTime(item.updatedAt || item.createdAt)}
          onOpen={(item) => onNavigateTarget && onNavigateTarget('contacts', { q: item.name || '', page: 1 })}
          onCopyId={(item) => copyIdToClipboard(item.id)}
          onQuickAction={(item) => onQuickCreateFollowUp && onQuickCreateFollowUp({ customerId: customer?.id, id: customer?.id, owner: customer?.owner, q: item.name || '' })}
          quickLabel={t('quickCreateFollowUp')}
          loading={moduleMeta?.contacts?.loading}
          error={moduleMeta?.contacts?.error}
          onRetry={() => onRefreshModules?.(['contacts'])}
        />
        <Customer360SectionTable
          t={t}
          title={t('pipeline')}
          items={related.opportunities}
          emptyText={t('noData')}
          buildMainText={(item) => item.title || `${item.owner || '-'} / ${translateStage(t, item.stage)}`}
          buildStatusText={(item) => translateStage(t, item.stage)}
          buildTimeText={(item) => formatDateTime(item.updatedAt || item.createdAt)}
          onOpen={(item) => onNavigateTarget && onNavigateTarget('pipeline', { q: item.owner || '', page: 1 })}
          onCopyId={(item) => copyIdToClipboard(item.id)}
          onQuickAction={() => onQuickCreateQuote && onQuickCreateQuote()}
          quickLabel={t('quickCreateQuote')}
          loading={moduleMeta?.opportunities?.loading}
          error={moduleMeta?.opportunities?.error}
          onRetry={() => onRefreshModules?.(['opportunities'])}
        />
        <Customer360SectionTable
          t={t}
          title={t('quotes')}
          items={related.quotes}
          emptyText={t('noData')}
          buildMainText={(item) => item.title || item.quoteNo || item.id}
          buildStatusText={(item) => translateStatus(t, item.status)}
          buildTimeText={(item) => formatDateTime(item.updatedAt || item.createdAt)}
          onOpen={(item) => onNavigateTarget && onNavigateTarget('quotes', {
            owner: item.owner || '',
            status: item.status || '',
            opportunityId: item.opportunityId || '',
            customerId: item.customerId || customer?.id || '',
            page: 1,
          })}
          onCopyId={(item) => copyIdToClipboard(item.id)}
          onQuickAction={() => onQuickViewOrders && onQuickViewOrders()}
          quickLabel={t('quickViewOrders')}
          loading={moduleMeta?.quotes?.loading}
          error={moduleMeta?.quotes?.error}
          onRetry={() => onRefreshModules?.(['quotes'])}
        />
        <Customer360SectionTable
          t={t}
          title={t('orders')}
          items={related.orders}
          emptyText={t('noData')}
          buildMainText={(item) => item.orderNo || item.id}
          buildStatusText={(item) => translateStatus(t, item.status)}
          buildTimeText={(item) => formatDateTime(item.updatedAt || item.createdAt)}
          onOpen={(item) => onNavigateTarget && onNavigateTarget('orders', {
            owner: item.owner || '',
            status: item.status || '',
            opportunityId: item.opportunityId || '',
            customerId: item.customerId || customer?.id || '',
            page: 1,
          })}
          onCopyId={(item) => copyIdToClipboard(item.id)}
          onQuickAction={() => onQuickViewOrders && onQuickViewOrders()}
          quickLabel={t('quickViewOrders')}
          loading={moduleMeta?.orders?.loading}
          error={moduleMeta?.orders?.error}
          onRetry={() => onRefreshModules?.(['orders'])}
        />
        <Customer360SectionTable
          t={t}
          title={t('contracts')}
          items={related.contracts}
          emptyText={t('noData')}
          buildMainText={(item) => item.contractNo || item.title || item.id}
          buildStatusText={(item) => translateStatus(t, item.status)}
          buildTimeText={(item) => formatDateTime(item.signDate || item.updatedAt || item.createdAt)}
          onOpen={(item) => onNavigateTarget && onNavigateTarget('contracts', { status: item.status || '', q: item.contractNo || '', page: 1 })}
          onCopyId={(item) => copyIdToClipboard(item.id)}
          onQuickAction={() => onQuickUrgeApproval && onQuickUrgeApproval()}
          quickLabel={t('quickUrgeApproval')}
          loading={moduleMeta?.contracts?.loading}
          error={moduleMeta?.contracts?.error}
          onRetry={() => onRefreshModules?.(['contracts'])}
        />
        <Customer360SectionTable
          t={t}
          title={t('payments')}
          items={related.payments}
          emptyText={t('noData')}
          buildMainText={(item) => formatMoney(item.amount)}
          buildStatusText={(item) => translateStatus(t, item.status)}
          buildTimeText={(item) => formatDateTime(item.receivedDate || item.updatedAt || item.createdAt)}
          onOpen={(item) => onNavigateTarget && onNavigateTarget('payments', { status: item.status || '', page: 1 })}
          onCopyId={(item) => copyIdToClipboard(item.id)}
          loading={moduleMeta?.payments?.loading}
          error={moduleMeta?.payments?.error}
          onRetry={() => onRefreshModules?.(['payments'])}
        />
        <Customer360SectionTable
          t={t}
          title={t('approvals')}
          items={related.approvals}
          emptyText={t('noData')}
          buildMainText={(item) => `${item.bizType || '-'} #${item.bizId || '-'} `}
          buildStatusText={(item) => translateStatus(t, item.status)}
          buildTimeText={(item) => formatDateTime(item.createdAt)}
          onOpen={(item) => onNavigateTarget && onNavigateTarget('approvals', { status: item.status || 'PENDING', page: 1 })}
          onCopyId={(item) => copyIdToClipboard(item.id)}
          onQuickAction={() => onQuickUrgeApproval && onQuickUrgeApproval()}
          quickLabel={t('quickUrgeApproval')}
          loading={moduleMeta?.approvals?.loading}
          error={moduleMeta?.approvals?.error}
          onRetry={() => onRefreshModules?.(['approvals'])}
        />
        <Customer360SectionTable
          t={t}
          title={t('audit')}
          items={related.audits}
          emptyText={t('noData')}
          buildMainText={(item) => item.action || '-'}
          buildStatusText={() => '-'}
          buildTimeText={(item) => formatDateTime(item.createdAt)}
          onOpen={(item) => onNavigateTarget && onNavigateTarget('audit', { q: String(item.entityId || customer?.id || ''), page: 1 })}
          onCopyId={(item) => copyIdToClipboard(item.id)}
          loading={moduleMeta?.audits?.loading}
          error={moduleMeta?.audits?.error}
          onRetry={() => onRefreshModules?.(['audits'])}
        />
        <Customer360SectionTable
          t={t}
          title={t('notifications')}
          items={related.notifications}
          emptyText={t('noData')}
          buildMainText={(item) => item.eventType || item.targetType || '-'}
          buildStatusText={(item) => translateStatus(t, item.status)}
          buildTimeText={(item) => formatDateTime(item.createdAt)}
          onOpen={() => onNavigateTarget && onNavigateTarget('approvals', { status: 'PENDING', page: 1 })}
          onCopyId={(item) => copyIdToClipboard(item.id)}
          loading={moduleMeta?.notifications?.loading}
          error={moduleMeta?.notifications?.error}
          onRetry={() => onRefreshModules?.(['notifications'])}
        />
      </div>

      <section className="crm360-section">
        <div className="crm360-section-head"><h4>{t('timeline')}</h4></div>
        {!vm.timeline.length ? <div className="empty-tip">{t('noData')}</div> : null}
        {vm.timeline.map((item, idx) => (
          <div className="drawer-timeline-item" key={`${item.sourceId || idx}-${idx}`}>
            <div>{item.title}</div>
            <small>{item.time ? String(item.time).replace('T', ' ').slice(0, 16) : '-'} | {translateStatus(t, item.status)}</small>
          </div>
        ))}
      </section>
    </div>
  )
})

const toArray = (value) => (Array.isArray(value) ? value : [])
const toNumber = (value) => Number(value || 0)

const toList = (result) => {
  if (!result) return []
  if (Array.isArray(result.items)) return result.items
  if (Array.isArray(result.data)) return result.data
  if (Array.isArray(result.rows)) return result.rows
  if (Array.isArray(result)) return result
  return []
}

function CustomersPanel({
  activePage,
  t,
  canWrite,
  customerForm,
  setCustomerForm,
  saveCustomer,
  formError,
  fieldErrors,
  customers,
  editCustomer,
  canDeleteCustomer,
  removeCustomer,
  loading,
  customerQ,
  setCustomerQ,
  customerStatus,
  setCustomerStatus,
  pagination,
  onPageChange,
  onSizeChange,
  onRefresh,
  loadTimeline,
  timeline,
  quickCreateFollowUp,
  quickCreateTask,
  onWorkbenchNavigate,
  buildWorkbenchFilterSignature,
  customer360Metrics,
  apiContext,
}) {
  const location = useLocation()
  const navigate = useNavigate()
  const [sortBy, setSortBy] = useState('nameAsc')
  const [detail, setDetail] = useState(null)
  const [detailMode, setDetailMode] = useState('drawer')
  const [customerQDraft, setCustomerQDraft] = useState(customerQ || '')
  const [customerStatusDraft, setCustomerStatusDraft] = useState(customerStatus || '')
  const [batchOwner, setBatchOwner] = useState('')
  const [batchStatus, setBatchStatus] = useState('')
  const [batchModalOpen, setBatchModalOpen] = useState(false)
  const [timelineLoading, setTimelineLoading] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailModules, setDetailModules] = useState({
    contacts: [],
    opportunities: [],
    quotes: [],
    orders: [],
    contracts: [],
    payments: [],
    approvals: [],
    audits: [],
    notifications: [],
  })
  const [detailModuleMeta, setDetailModuleMeta] = useState(() => createCustomer360ModuleMeta())
  const detailsAbortRef = useRef(null)
  const prefetchAbortRef = useRef(null)
  const activePrefetchModulesRef = useRef(new Set())
  const moduleCacheRef = useRef(new Map())
  const prefetchedSignaturesRef = useRef(new Set())
  const adaptivePrefetchModulesRef = useRef([...CUSTOMER360_PREFETCH_MODULES])
  const adaptiveModuleStatsRef = useRef({})
  const detailsCustomerIdRef = useRef('')
  const detailRequestSeqRef = useRef(0)
  const prefetchTimerRef = useRef(null)
  const lastPrefetchAtRef = useRef(0)
  const currentDetailIdRef = useRef('')
  const detailModulesRef = useRef(detailModules)
  const detailModuleMetaRef = useRef(detailModuleMeta)
  const customer360JumpRef = useRef({ signature: '' })
  const token = apiContext?.token
  const lang = apiContext?.lang || 'en'
  const { summary: batchSummary, toastMessage: batchMessage, runBatch, clearSummary } = useBatchActions({ t })
  const markCustomer360ActionResult = customer360Metrics?.markActionResult
  const markCustomer360ModuleRefreshLatency = customer360Metrics?.markModuleRefreshLatency
  const markCustomer360JumpHit = customer360Metrics?.markJumpHit
  const markCustomer360ModuleCacheHit = customer360Metrics?.markModuleCacheHit
  const markCustomer360PrefetchHit = customer360Metrics?.markPrefetchHit
  const markCustomer360PrefetchAbort = customer360Metrics?.markPrefetchAbort
  const markCustomer360PrefetchModules = customer360Metrics?.markPrefetchModules

  useEffect(() => {
    detailModulesRef.current = detailModules
  }, [detailModules])

  useEffect(() => {
    detailModuleMetaRef.current = detailModuleMeta
  }, [detailModuleMeta])

  useEffect(() => {
    currentDetailIdRef.current = String(detail?.id || '')
  }, [detail?.id])

  useEffect(() => {
    markCustomer360PrefetchModules?.(adaptivePrefetchModulesRef.current)
  }, [markCustomer360PrefetchModules])

  const abortPrefetchRequests = useCallback(() => {
    if (prefetchTimerRef.current) {
      clearTimeout(prefetchTimerRef.current)
      prefetchTimerRef.current = null
    }
    if (!prefetchAbortRef.current) return
    prefetchAbortRef.current.abort()
    ;[...activePrefetchModulesRef.current].forEach((moduleName) => markCustomer360PrefetchAbort?.(moduleName))
    activePrefetchModulesRef.current.clear()
  }, [markCustomer360PrefetchAbort])

  const rows = useMemo(() => {
    const sorted = [...(customers || [])]
    if (sortBy === 'nameAsc') sorted.sort((a, b) => String(a.name || '').localeCompare(String(b.name || '')))
    if (sortBy === 'nameDesc') sorted.sort((a, b) => String(b.name || '').localeCompare(String(a.name || '')))
    if (sortBy === 'valueAsc') sorted.sort((a, b) => Number(a.value || 0) - Number(b.value || 0))
    if (sortBy === 'valueDesc') sorted.sort((a, b) => Number(b.value || 0) - Number(a.value || 0))
    return sorted.map((row) => ({
      ...row,
      statusLabel: translateStatus(t, row.status),
      valueText: formatMoney(row.value),
    }))
  }, [customers, sortBy, t])

  const rowMap = useMemo(() => new Map(rows.map((row) => [String(row.id), row])), [rows])

  useEffect(() => { setCustomerQDraft(customerQ || '') }, [customerQ])
  useEffect(() => { setCustomerStatusDraft(customerStatus || '') }, [customerStatus])

  const page = pagination?.page || 1
  const totalPages = Math.max(1, pagination?.totalPages || 1)
  const selection = useSelectionSet(rows, (row) => row.id)
  const { selectedIds, selectedCount, allChecked, clearSelection, selectPage, toggleAll, toggleOne } = selection
  const byId = useMemo(() => new Map((customers || []).map((row) => [row.id, row])), [customers])
  const rowIndexById = useMemo(() => {
    const map = new Map()
    rows.forEach((row, idx) => map.set(String(row.id), idx))
    return map
  }, [rows])

  const syncFromUrl = useCallback(() => {
    const query = new URLSearchParams(location.search)
    const view = query.get('view')
    const customerId = query.get('customerId')
    if (view !== 'detail' || !customerId) return
    const row = rowMap.get(String(customerId))
    setDetail(row || { id: customerId, name: `${t('customers')} #${customerId}` })
    setDetailMode('page')
  }, [location.search, rowMap, t])

  useEffect(() => {
    if (activePage !== 'customers') return
    syncFromUrl()
  }, [activePage, syncFromUrl])

  useEffect(() => {
    if (activePage === 'customers') return
    if (detailsAbortRef.current) detailsAbortRef.current.abort()
    abortPrefetchRequests()
  }, [abortPrefetchRequests, activePage])

  useEffect(() => {
    return () => {
      if (detailsAbortRef.current) detailsAbortRef.current.abort()
      abortPrefetchRequests()
      if (prefetchTimerRef.current) {
        clearTimeout(prefetchTimerRef.current)
        prefetchTimerRef.current = null
      }
    }
  }, [abortPrefetchRequests])

  const loadCustomer360 = useCallback(async (customer, options = {}) => {
    const customerId = customer?.id
    if (!customerId || !token) return
    const requestedModules = Array.isArray(options.modules) && options.modules.length
      ? options.modules.filter((key) => CUSTOMER360_MODULE_KEYS.includes(key))
      : CUSTOMER360_MODULE_KEYS
    const fullRefresh = requestedModules.length === CUSTOMER360_MODULE_KEYS.length
    const shouldReset = !!options.resetModules
    const force = !!options.force
    const source = options.source || 'open'
    const customerIdText = String(customerId)
    const customerChanged = detailsCustomerIdRef.current !== customerIdText
    if (shouldReset || customerChanged) {
      detailsCustomerIdRef.current = customerIdText
      setDetailModules({
        contacts: [],
        opportunities: [],
        quotes: [],
        orders: [],
        contracts: [],
        payments: [],
        approvals: [],
        audits: [],
        notifications: [],
      })
      setDetailModuleMeta(createCustomer360ModuleMeta())
    }
    if (detailsAbortRef.current) detailsAbortRef.current.abort()
    const controller = new AbortController()
    const requestSeq = detailRequestSeqRef.current + 1
    detailRequestSeqRef.current = requestSeq
    detailsAbortRef.current = controller
    if (fullRefresh || shouldReset) setDetailLoading(true)
    const startedAt = Date.now()
    try {
      const loadList = async (path, fallbackPath = '') => {
        try {
          const data = await api(path, { signal: controller.signal }, token, lang)
          return toList(data)
        } catch {
          if (!fallbackPath) return []
          try {
            const fallbackData = await api(fallbackPath, { signal: controller.signal }, token, lang)
            return toList(fallbackData)
          } catch {
            return []
          }
        }
      }
      const timedLoad = async (moduleName, path, fallbackPath = '') => {
        const moduleStartedAt = Date.now()
        const list = await loadList(path, fallbackPath)
        const latency = Math.max(Date.now() - moduleStartedAt, 0)
        if (markCustomer360ModuleRefreshLatency) {
          markCustomer360ModuleRefreshLatency(moduleName, latency)
        }
        if (import.meta.env.DEV) {
          const stats = adaptiveModuleStatsRef.current[moduleName] || { total: 0, hits: 0, latencyTotal: 0, latencyCount: 0 }
          stats.latencyTotal += latency
          stats.latencyCount += 1
          adaptiveModuleStatsRef.current[moduleName] = stats
        }
        return list
      }
      const moduleLoaders = {
        contacts: async () => toArray(await timedLoad('contacts', `/contacts/search?customerId=${encodeURIComponent(customerIdText)}&page=1&size=${MAX_ASSOC_ITEMS}`)).slice(0, MAX_ASSOC_ITEMS),
        opportunities: async () => toArray(await timedLoad('opportunities', `/opportunities/search?customerId=${encodeURIComponent(customerIdText)}&page=1&size=${MAX_ASSOC_ITEMS}`)).slice(0, MAX_ASSOC_ITEMS),
        contracts: async () => toArray(await timedLoad('contracts', `/contracts/search?customerId=${encodeURIComponent(customerIdText)}&page=1&size=${MAX_ASSOC_ITEMS}`)).slice(0, MAX_ASSOC_ITEMS),
        payments: async () => toArray(await timedLoad('payments', `/payments/search?customerId=${encodeURIComponent(customerIdText)}&page=1&size=${MAX_ASSOC_ITEMS}`)).slice(0, MAX_ASSOC_ITEMS),
        quotes: async () => {
          const list = await timedLoad('quotes', `/v1/quotes?page=1&size=${MAX_ASSOC_ITEMS}&customerId=${encodeURIComponent(customerIdText)}`, '/v1/quotes?page=1&size=30')
          return toArray(list).filter((item) => String(item.customerId || item.customer?.id || '') === customerIdText).slice(0, MAX_ASSOC_ITEMS)
        },
        orders: async () => {
          const list = await timedLoad('orders', `/v1/orders?page=1&size=${MAX_ASSOC_ITEMS}&customerId=${encodeURIComponent(customerIdText)}`, '/v1/orders?page=1&size=30')
          return toArray(list).filter((item) => String(item.customerId || item.customer?.id || '') === customerIdText).slice(0, MAX_ASSOC_ITEMS)
        },
        approvals: async () => {
          const list = await timedLoad('approvals', '/v1/approval/instances?limit=30')
          return toArray(list).filter((item) => {
            const bizId = String(item.bizId || '')
            const ref = String(item.refId || '')
            return bizId === customerIdText || ref === customerIdText || bizId.includes(customerIdText) || ref.includes(customerIdText)
          }).slice(0, MAX_ASSOC_ITEMS)
        },
        audits: async () => toArray(await timedLoad('audits', `/audit-logs/search?q=${encodeURIComponent(customerIdText)}&page=1&size=${MAX_ASSOC_ITEMS}`)).slice(0, MAX_ASSOC_ITEMS),
        notifications: async () => {
          const list = await timedLoad('notifications', '/v1/integrations/notifications/jobs?status=ALL&page=1&size=30')
          return toArray(list).filter((item) => JSON.stringify(item).includes(customerIdText)).slice(0, MAX_ASSOC_ITEMS)
        },
      }

      const moduleServerFilterSignature = (moduleName) => {
        const signatures = {
          contacts: `size=${MAX_ASSOC_ITEMS}`,
          opportunities: `size=${MAX_ASSOC_ITEMS}`,
          contracts: `size=${MAX_ASSOC_ITEMS}`,
          payments: `size=${MAX_ASSOC_ITEMS}`,
          quotes: `size=${MAX_ASSOC_ITEMS}`,
          orders: `size=${MAX_ASSOC_ITEMS}`,
          approvals: 'limit=30',
          audits: `size=${MAX_ASSOC_ITEMS}`,
          notifications: 'status=ALL:size=30',
        }
        return signatures[moduleName] || `size=${MAX_ASSOC_ITEMS}`
      }

      const loadOneModule = async (moduleName) => {
        const now = Date.now()
        const signature = buildCustomer360ModuleSignature(
          customerIdText,
          moduleName,
          moduleServerFilterSignature(moduleName),
        )
        const cacheKey = signature
        const cacheEntry = moduleCacheRef.current.get(cacheKey)
        const metaEntry = detailModuleMetaRef.current?.[moduleName]
        const moduleData = detailModulesRef.current?.[moduleName]
        const metaHit = !force
          && metaEntry
          && metaEntry.signature === signature
          && (now - Number(metaEntry.lastLoadedAt || 0)) <= CUSTOMER360_MODULE_TTL_MS
          && Array.isArray(moduleData)
        const cacheHit = !force
          && cacheEntry
          && cacheEntry.signature === signature
          && (now - cacheEntry.at) <= CUSTOMER360_MODULE_TTL_MS
        const reusedData = cacheHit ? cacheEntry.data : (metaHit ? moduleData : null)

        if (reusedData) {
          setDetailModules((prev) => ({ ...prev, [moduleName]: reusedData }))
          setDetailModuleMeta((prev) => ({
            ...prev,
            [moduleName]: {
              ...(prev[moduleName] || {}),
              loading: false,
              error: '',
              signature,
              lastLoadedAt: cacheHit ? cacheEntry.at : Number(metaEntry?.lastLoadedAt || now),
            },
          }))
          markCustomer360ModuleCacheHit?.(true, moduleName)
          const prefetchedHit = cacheHit && prefetchedSignaturesRef.current.has(cacheKey)
          if (source === 'open' && adaptivePrefetchModulesRef.current.includes(moduleName)) {
            markCustomer360PrefetchHit?.(prefetchedHit, moduleName)
            if (prefetchedHit) prefetchedSignaturesRef.current.delete(cacheKey)
            if (import.meta.env.DEV) {
              const stats = adaptiveModuleStatsRef.current[moduleName] || { total: 0, hits: 0, latencyTotal: 0, latencyCount: 0 }
              stats.total += 1
              if (prefetchedHit) stats.hits += 1
              adaptiveModuleStatsRef.current[moduleName] = stats
            }
          }
          return
        }

        markCustomer360ModuleCacheHit?.(false, moduleName)
        if (source === 'open' && adaptivePrefetchModulesRef.current.includes(moduleName)) {
          markCustomer360PrefetchHit?.(false, moduleName)
          if (import.meta.env.DEV) {
            const stats = adaptiveModuleStatsRef.current[moduleName] || { total: 0, hits: 0, latencyTotal: 0, latencyCount: 0 }
            stats.total += 1
            adaptiveModuleStatsRef.current[moduleName] = stats
          }
        }

        setDetailModuleMeta((prev) => ({
          ...prev,
          [moduleName]: {
            ...(prev[moduleName] || {}),
            loading: true,
            error: '',
            signature,
          },
        }))
        try {
          const data = await moduleLoaders[moduleName]()
          if (controller.signal.aborted) return
          moduleCacheRef.current.set(cacheKey, {
            at: Date.now(),
            signature,
            data,
          })
          setDetailModules((prev) => ({ ...prev, [moduleName]: data }))
          setDetailModuleMeta((prev) => ({
            ...prev,
            [moduleName]: {
              ...(prev[moduleName] || {}),
              loading: false,
              error: '',
              signature,
              lastLoadedAt: Date.now(),
            },
          }))
        } catch (error) {
          if (controller.signal.aborted) return
          const message = error?.requestId
            ? `${error?.message || t('loadFailed')} [${error.requestId}]`
            : (error?.message || t('loadFailed'))
          setDetailModuleMeta((prev) => ({
            ...prev,
            [moduleName]: {
              ...(prev[moduleName] || {}),
              loading: false,
              error: message,
              signature,
              lastLoadedAt: Date.now(),
            },
          }))
        }
      }

      const primaryModules = requestedModules.filter((moduleName) => CUSTOMER360_PRIMARY_MODULES.includes(moduleName))
      const secondaryModules = requestedModules.filter((moduleName) => CUSTOMER360_SECONDARY_MODULES.includes(moduleName))
      const remainingModules = requestedModules.filter((moduleName) => !primaryModules.includes(moduleName) && !secondaryModules.includes(moduleName))
      const orderedModules = [...primaryModules, ...secondaryModules, ...remainingModules]
      await Promise.all(orderedModules.map((moduleName) => loadOneModule(moduleName)))
      if (import.meta.env.DEV) {
        const next = new Set(adaptivePrefetchModulesRef.current)
        CUSTOMER360_PREFETCH_MODULES.forEach((moduleName) => {
          const stats = adaptiveModuleStatsRef.current[moduleName]
          if (!stats || stats.total < CUSTOMER360_ADAPTIVE_WINDOW) return
          const hitRate = stats.total > 0 ? Math.round((stats.hits / stats.total) * 100) : 0
          const avgLatency = stats.latencyCount > 0 ? Math.round(stats.latencyTotal / stats.latencyCount) : 0
          if (hitRate <= CUSTOMER360_PREFETCH_LOW_HIT && avgLatency <= CUSTOMER360_PREFETCH_HIGH_LATENCY) {
            next.delete(moduleName)
          } else if (hitRate >= CUSTOMER360_PREFETCH_HIGH_HIT || avgLatency >= CUSTOMER360_PREFETCH_HIGH_LATENCY) {
            next.add(moduleName)
          }
          adaptiveModuleStatsRef.current[moduleName] = { total: 0, hits: 0, latencyTotal: 0, latencyCount: 0 }
        })
        const normalized = CUSTOMER360_PREFETCH_MODULES.filter((moduleName) => next.has(moduleName))
        adaptivePrefetchModulesRef.current = normalized.length ? normalized : [...CUSTOMER360_PREFETCH_MODULES]
        markCustomer360PrefetchModules?.(adaptivePrefetchModulesRef.current)
      }
    } finally {
      const isLatestRequest = detailRequestSeqRef.current === requestSeq
      if ((fullRefresh || shouldReset) && isLatestRequest) setDetailLoading(false)
      if (markCustomer360ModuleRefreshLatency && isLatestRequest) {
        markCustomer360ModuleRefreshLatency('all', Math.max(Date.now() - startedAt, 0))
      }
    }
  }, [lang, markCustomer360ModuleCacheHit, markCustomer360ModuleRefreshLatency, markCustomer360PrefetchHit, markCustomer360PrefetchModules, t, token])

  const refreshCustomer360Modules = useCallback(async (modules = []) => {
    if (!detail?.id) return
    await loadCustomer360(detail, {
      modules: Array.isArray(modules) && modules.length ? modules : CUSTOMER360_MODULE_KEYS,
      resetModules: false,
      force: true,
      source: 'action',
    })
  }, [detail, loadCustomer360])

  const executeCustomer360Action = useCallback(async (actionType) => {
    if (!detail) return
    const modules = CUSTOMER360_ACTION_MODULES[actionType] || []
    const shouldRefreshTimeline = modules.includes('timeline')
    const moduleOnly = modules.filter((moduleName) => moduleName !== 'timeline')
    if (shouldRefreshTimeline && loadTimeline) await loadTimeline(detail.id)
    if (moduleOnly.length) await refreshCustomer360Modules(moduleOnly)
    markCustomer360ActionResult?.(true)
  }, [detail, loadTimeline, markCustomer360ActionResult, refreshCustomer360Modules])

  const prefetchNeighborModules = useCallback(async (centerRow) => {
    if (!centerRow?.id || activePage !== 'customers' || detailMode !== 'drawer' || !token) return
    if (currentDetailIdRef.current && currentDetailIdRef.current !== String(centerRow.id)) return
    const now = Date.now()
    if (now - lastPrefetchAtRef.current < 200) return
    lastPrefetchAtRef.current = now
    const idx = rowIndexById.get(String(centerRow.id))
    if (idx === undefined) return
    const neighbors = [rows[idx - 1], rows[idx + 1]].filter(Boolean)
    if (!neighbors.length) return

    abortPrefetchRequests()
    const controller = new AbortController()
    prefetchAbortRef.current = controller

    const loadList = async (path, fallbackPath = '') => {
      try {
        const data = await api(path, { signal: controller.signal }, token, lang)
        return toList(data)
      } catch {
        if (!fallbackPath) return []
        try {
          const fallbackData = await api(fallbackPath, { signal: controller.signal }, token, lang)
          return toList(fallbackData)
        } catch {
          return []
        }
      }
    }

    const prefetchModule = async (customerIdText, moduleName) => {
      activePrefetchModulesRef.current.add(moduleName)
      try {
        const signature = buildCustomer360ModuleSignature(
          customerIdText,
          moduleName,
          moduleName === 'notifications'
            ? 'status=ALL:size=30'
            : moduleName === 'approvals'
              ? 'limit=30'
              : `size=${MAX_ASSOC_ITEMS}`,
        )
        const cacheEntry = moduleCacheRef.current.get(signature)
        const now = Date.now()
        if (cacheEntry && cacheEntry.signature === signature && (now - cacheEntry.at) <= CUSTOMER360_MODULE_TTL_MS) return
        let data = []
        if (moduleName === 'contacts') {
          data = toArray(await loadList(`/contacts/search?customerId=${encodeURIComponent(customerIdText)}&page=1&size=${MAX_ASSOC_ITEMS}`)).slice(0, MAX_ASSOC_ITEMS)
        } else if (moduleName === 'opportunities') {
          data = toArray(await loadList(`/opportunities/search?customerId=${encodeURIComponent(customerIdText)}&page=1&size=${MAX_ASSOC_ITEMS}`)).slice(0, MAX_ASSOC_ITEMS)
        } else if (moduleName === 'quotes') {
          const list = await loadList(`/v1/quotes?page=1&size=${MAX_ASSOC_ITEMS}&customerId=${encodeURIComponent(customerIdText)}`, '/v1/quotes?page=1&size=30')
          data = toArray(list).filter((item) => String(item.customerId || item.customer?.id || '') === customerIdText).slice(0, MAX_ASSOC_ITEMS)
        } else if (moduleName === 'orders') {
          const list = await loadList(`/v1/orders?page=1&size=${MAX_ASSOC_ITEMS}&customerId=${encodeURIComponent(customerIdText)}`, '/v1/orders?page=1&size=30')
          data = toArray(list).filter((item) => String(item.customerId || item.customer?.id || '') === customerIdText).slice(0, MAX_ASSOC_ITEMS)
        }
        if (controller.signal.aborted) return
        moduleCacheRef.current.set(signature, {
          at: Date.now(),
          signature,
          data,
        })
        prefetchedSignaturesRef.current.add(signature)
      } finally {
        activePrefetchModulesRef.current.delete(moduleName)
      }
    }

    try {
      if (import.meta.env.DEV) {
        console.debug('[customers360] prefetch neighbors', {
          center: centerRow.id,
          neighbors: neighbors.map((row) => row.id),
          modules: adaptivePrefetchModulesRef.current,
        })
      }
      await Promise.all(
        neighbors.flatMap((row) => {
          const customerIdText = String(row.id)
          return adaptivePrefetchModulesRef.current.map((moduleName) => prefetchModule(customerIdText, moduleName))
        }),
      )
    } catch {
      // prefetch failures are non-blocking
    }
  }, [abortPrefetchRequests, activePage, detailMode, lang, rowIndexById, rows, token])

  const scheduleNeighborPrefetch = useCallback((row) => {
    if (!row?.id || detailMode !== 'drawer') return
    if (prefetchTimerRef.current) {
      clearTimeout(prefetchTimerRef.current)
    }
    prefetchTimerRef.current = setTimeout(() => {
      prefetchTimerRef.current = null
      prefetchNeighborModules(row)
    }, 180)
  }, [detailMode, prefetchNeighborModules])

  const openDetail = useCallback(async (row, mode = 'drawer') => {
    setDetail(row)
    setDetailMode(mode)
    if (!row?.id) return
    setTimelineLoading(true)
    try {
      await Promise.all([
        loadTimeline ? loadTimeline(row.id) : Promise.resolve(),
        loadCustomer360(row, { resetModules: true, force: false, source: 'open' }),
      ])
    } catch {
      // global error banner handles message
    } finally {
      setTimelineLoading(false)
    }
    if (mode === 'drawer') {
      scheduleNeighborPrefetch(row)
    }
  }, [loadTimeline, loadCustomer360, scheduleNeighborPrefetch])

  const openFullDetail = useCallback(async (row) => {
    if (!row?.id) return
    const query = new URLSearchParams(location.search)
    query.set('view', 'detail')
    query.set('customerId', String(row.id))
    navigate(`${location.pathname}?${query.toString()}`, { replace: false })
    await openDetail(row, 'page')
  }, [location.pathname, location.search, navigate, openDetail])

  const openNeighbor = useCallback(async (step) => {
    if (!detail?.id) return
    const idx = rowIndexById.get(String(detail.id))
    if (idx === undefined) return
    const next = rows[idx + step]
    if (!next) return
    await openDetail(next, detailMode)
  }, [detail, detailMode, openDetail, rowIndexById, rows])

  const closeFullDetail = useCallback(() => {
    const query = new URLSearchParams(location.search)
    query.delete('view')
    query.delete('customerId')
    const suffix = query.toString()
    navigate(`${location.pathname}${suffix ? `?${suffix}` : ''}`, { replace: false })
    setDetailMode('drawer')
    setDetail(null)
    abortPrefetchRequests()
  }, [abortPrefetchRequests, location.pathname, location.search, navigate])

  const closeDrawer = () => {
    setDetail(null)
    setDetailMode('drawer')
    abortPrefetchRequests()
  }

  const updateOne = async (id, patch) => {
    const row = byId.get(id)
    if (!row) return
    const payload = {
      name: String(row.name || '').trim(),
      owner: String(patch.owner ?? row.owner ?? '').trim(),
      status: String(patch.status ?? row.status ?? '').trim(),
      tag: String(row.tag || '').trim(),
      value: Number(row.value || 0),
    }
    await api('/customers/' + id, { method: 'PATCH', body: JSON.stringify(payload) }, token, lang)
  }

  const batchDelete = async () => {
    if (!canDeleteCustomer) return
    const result = await runBatch({
      ids: [...selectedIds],
      worker: (id) => api('/customers/' + id, { method: 'DELETE' }, token, lang),
      batch: { path: '/v1/customers/batch-actions', action: 'DELETE', token, lang },
      canRun: canDeleteCustomer,
    })
    if (result?.failed > 0) setBatchModalOpen(true)
    clearSelection()
    if (onRefresh) await onRefresh()
  }

  const batchAssign = async () => {
    if (!batchOwner.trim()) return
    const result = await runBatch({
      ids: [...selectedIds],
      worker: (id) => updateOne(id, { owner: batchOwner.trim() }),
      batch: { path: '/v1/customers/batch-actions', action: 'ASSIGN_OWNER', payload: { owner: batchOwner.trim() }, token, lang },
    })
    if (result?.failed > 0) setBatchModalOpen(true)
    clearSelection()
    if (onRefresh) await onRefresh()
  }

  const batchChangeStatus = async () => {
    if (!batchStatus) return
    const result = await runBatch({
      ids: [...selectedIds],
      worker: (id) => updateOne(id, { status: batchStatus }),
      batch: { path: '/v1/customers/batch-actions', action: 'UPDATE_STATUS', payload: { status: batchStatus }, token, lang },
    })
    if (result?.failed > 0) setBatchModalOpen(true)
    clearSelection()
    if (onRefresh) await onRefresh()
  }

  const applyFilters = () => {
    if (customerQ !== customerQDraft) setCustomerQ(customerQDraft)
    if (customerStatus !== customerStatusDraft) setCustomerStatus(customerStatusDraft)
    onPageChange(1)
  }

  const resetFilters = () => {
    setCustomerQDraft('')
    setCustomerStatusDraft('')
    if (customerQ !== '') setCustomerQ('')
    if (customerStatus !== '') setCustomerStatus('')
    onPageChange(1)
  }

  const customer360ViewModel = useMemo(() => {
    const now = Date.now()
    const timelineRows = toArray(timeline)
    const followUpsRecent = timelineRows.filter((item) => {
      const dt = new Date(item.time || item.createdAt || '').getTime()
      if (!dt || Number.isNaN(dt)) return false
      return now - dt <= THIRTY_DAYS_MS
    })
    const inFlightAmount = toArray(detailModules.opportunities)
      .filter((item) => !String(item.stage || '').toLowerCase().includes('closed'))
      .reduce((sum, item) => sum + toNumber(item.amount), 0)
    const orderAmount = toArray(detailModules.orders).reduce((sum, item) => sum + toNumber(item.amount), 0)
    const paymentReceived = toArray(detailModules.payments)
      .filter((item) => ['RECEIVED', 'COMPLETED'].includes(String(item.status || '').toUpperCase()))
      .reduce((sum, item) => sum + toNumber(item.amount), 0)
    const pendingApprovals = toArray(detailModules.approvals).filter((item) => ['PENDING', 'WAITING', 'SUBMITTED'].includes(String(item.status || '').toUpperCase())).length
    const paymentOutstanding = Math.max(orderAmount - paymentReceived, 0)
    const riskTags = []
    if (paymentOutstanding > 0) riskTags.push(t('paymentWarnings'))
    if (pendingApprovals > 0) riskTags.push(t('pendingApprovals'))
    if (followUpsRecent.length === 0) riskTags.push(t('overdueFollowUps'))
    return {
      related: detailModules,
      timeline: timelineRows,
      metrics: {
        recentFollowUps30d: followUpsRecent.length,
        inFlightAmount,
        orderAmount,
        paymentReceived,
        paymentOutstanding,
        pendingApprovals,
      },
      riskTags,
    }
  }, [detailModules, timeline, t])

  const navigateTo = useCallback((targetPage, payload = {}) => {
    if (!onWorkbenchNavigate) {
      markCustomer360ActionResult?.(false)
      return
    }
    const signature = buildWorkbenchFilterSignature
      ? buildWorkbenchFilterSignature(targetPage, payload || {})
      : `${targetPage}:${JSON.stringify(payload || {})}`
    const hit = customer360JumpRef.current.signature === signature
    customer360JumpRef.current.signature = signature
    markCustomer360JumpHit?.(hit)
    onWorkbenchNavigate(targetPage, payload)
    markCustomer360ActionResult?.(true)
  }, [buildWorkbenchFilterSignature, markCustomer360ActionResult, markCustomer360JumpHit, onWorkbenchNavigate])

  const isFullDetail = activePage === 'customers' && detailMode === 'page' && !!detail

  if (activePage !== 'customers') return null

  if (isFullDetail) {
    return (
      <section className="panel">
        <div className="panel-head"><h2>{t('customer360FullPage')}</h2></div>
        <Customer360View
          t={t}
          customer={detail}
          vm={customer360ViewModel}
          loading={timelineLoading || detailLoading}
          moduleMeta={detailModuleMeta}
          mode="page"
          onBackToList={closeFullDetail}
          onOpenFullDetail={() => null}
          onQuickCreateFollowUp={() => {
            if (!detail || !quickCreateFollowUp) return
            quickCreateFollowUp(detail)
            executeCustomer360Action('followup')
          }}
          onQuickCreateTask={() => {
            if (!detail || !quickCreateTask) return
            quickCreateTask(detail)
            executeCustomer360Action('task')
          }}
          onQuickCreateQuote={() => {
            if (!detail) return
            navigateTo('quotes', { customerId: detail.id, owner: detail.owner || '', page: 1 })
            executeCustomer360Action('quote')
          }}
          onQuickViewOrders={() => detail && navigateTo('orders', { customerId: detail.id, owner: detail.owner || '', page: 1 })}
          onQuickUrgeApproval={() => {
            if (!detail) return
            navigateTo('approvals', { status: 'PENDING', customerId: detail.id, page: 1 })
            executeCustomer360Action('urgeApproval')
          }}
          onNavigatePrev={() => openNeighbor(-1)}
          onNavigateNext={() => openNeighbor(1)}
          onNavigateTarget={navigateTo}
          onRefreshModules={refreshCustomer360Modules}
          onRefresh={() => {
            if (!detail) return
            refreshCustomer360Modules(CUSTOMER360_MODULE_KEYS)
            markCustomer360ActionResult?.(true)
          }}
        />
      </section>
    )
  }

  return (
    <section className="panel" data-testid="customers-page">
      <div className="panel-head"><h2 data-testid="customers-heading">{t('customers')}</h2></div>
      <div className="inline-tools filter-row" style={{ marginBottom: 10 }}>
        <input data-testid="customer-form-name" className={fieldErrors?.name ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('companyName')} value={customerForm.name} onChange={(e) => setCustomerForm((p) => ({ ...p, name: e.target.value }))} />
        <input data-testid="customer-form-owner" className={fieldErrors?.owner ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('owner')} value={customerForm.owner} onChange={(e) => setCustomerForm((p) => ({ ...p, owner: e.target.value }))} />
        <select data-testid="customer-form-status" className={fieldErrors?.status ? 'tool-input input-invalid' : 'tool-input'} value={customerForm.status} onChange={(e) => setCustomerForm((p) => ({ ...p, status: e.target.value }))}>
          <option value="">{t('selectPlaceholder')}</option>
          {CUSTOMER_STATUS_OPTIONS.map((status) => <option key={status} value={status}>{translateStatus(t, status)}</option>)}
        </select>
        <input data-testid="customer-form-value" className={fieldErrors?.value ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('amount')} value={customerForm.value} onChange={(e) => setCustomerForm((p) => ({ ...p, value: e.target.value }))} />
        <button className="mini-btn" data-testid="customer-form-submit" disabled={!canWrite} onClick={saveCustomer}>{customerForm.id ? t('save') : t('create')}</button>
        <button className="mini-btn" data-testid="customer-form-reset" onClick={() => setCustomerForm({ id: '', name: '', owner: '', status: '', tag: '', value: '' })}>{t('reset')}</button>
      </div>
      {(fieldErrors?.name || fieldErrors?.owner || fieldErrors?.status || fieldErrors?.value) && (
        <div className="field-error" style={{ marginBottom: 8 }}>
          {fieldErrors?.name || fieldErrors?.owner || fieldErrors?.status || fieldErrors?.value}
        </div>
      )}
      {formError && <div className="field-error" style={{ marginBottom: 8 }}>{formError}</div>}
      <div className="inline-tools filter-row" style={{ marginBottom: 8 }}>
        <input data-testid="customers-search-input" className="tool-input" placeholder={t('search')} value={customerQDraft} onChange={(e) => setCustomerQDraft(e.target.value)} />
        <select data-testid="customers-search-status" className="tool-input" value={customerStatusDraft} onChange={(e) => setCustomerStatusDraft(e.target.value)}>
          <option value="">{t('allStatuses')}</option>
          {CUSTOMER_STATUS_OPTIONS.map((status) => <option key={status} value={status}>{translateStatus(t, status)}</option>)}
        </select>
      </div>
      <div className="inline-tools filter-bar" style={{ marginBottom: 10 }}>
        <button className="mini-btn" data-testid="customers-search-submit" onClick={applyFilters}>{t('search')}</button>
        <button className="mini-btn" data-testid="customers-search-reset" onClick={resetFilters}>{t('reset')}</button>
        <button className="mini-btn" data-testid="customers-refresh" onClick={() => onRefresh && onRefresh()}>{t('refresh')}</button>
      </div>
      <div className="inline-tools filter-bar" style={{ marginBottom: 10 }}>
        <span className="muted-filter">{t('batchSelectedCount')}: {selectedCount}</span>
        <button className="mini-btn" onClick={selectPage}>{t('selectPage')}</button>
        <button className="mini-btn" onClick={clearSelection}>{t('clearSelection')}</button>
        <input className="tool-input" placeholder={t('batchOwnerPlaceholder')} value={batchOwner} onChange={(e) => setBatchOwner(e.target.value)} />
        <select className="tool-input" value={batchStatus} onChange={(e) => setBatchStatus(e.target.value)}>
          <option value="">{t('batchSetStatus')}</option>
          {CUSTOMER_STATUS_OPTIONS.map((status) => <option key={status} value={status}>{translateStatus(t, status)}</option>)}
        </select>
        <button className="mini-btn" disabled={!canWrite} onClick={batchAssign}>{t('batchAssignOwner')}</button>
        <button className="mini-btn" disabled={!canWrite} onClick={batchChangeStatus}>{t('batchSetStatus')}</button>
        {canDeleteCustomer && <button className="danger-btn" onClick={batchDelete}>{t('batchDelete')}</button>}
        {batchSummary?.failed > 0 && <button className="mini-btn" onClick={() => setBatchModalOpen(true)}>{t('batchResultTitle')}</button>}
      </div>
      {batchMessage && <div className="info-banner" style={{ marginBottom: 8 }}>{batchMessage}</div>}
      <div className="table-row table-head-row table-row-6">
        <span><input type="checkbox" checked={allChecked} onChange={(e) => toggleAll(e.target.checked)} /></span>
        <button className="table-head-btn" onClick={() => setSortBy((prev) => (prev === 'nameAsc' ? 'nameDesc' : 'nameAsc'))}>{t('companyName')}</button>
        <span>{t('owner')}</span>
        <span>{t('status')}</span>
        <button className="table-head-btn" onClick={() => setSortBy((prev) => (prev === 'valueAsc' ? 'valueDesc' : 'valueAsc'))}>{t('amount')}</button>
        <span>{t('action')}</span>
      </div>
      <ListState loading={loading} empty={!loading && rows.length === 0} emptyText={t('noData')} />
      {!loading && rows.length > 0 && (
        <VirtualListTable
          rows={rows}
          viewportHeight={460}
          getRowKey={(row) => row.id}
          renderRow={(row) => (
            <CustomerRow
              key={row.id}
              row={row}
              checked={selectedIds.has(row.id)}
              onToggle={(e) => toggleOne(row.id, e.target.checked)}
              t={t}
              openDetail={(item) => openDetail(item, 'drawer')}
              editCustomer={editCustomer}
              canDeleteCustomer={canDeleteCustomer}
              removeCustomer={removeCustomer}
            />
          )}
        />
      )}
      {!loading && rows.length > 0 && <ServerPager t={t} page={page} totalPages={totalPages} size={pagination?.size || 8} onPageChange={onPageChange} onSizeChange={onSizeChange} />}
      <RowDetailDrawer
        open={!!detail}
        title={t('customer360QuickView')}
        t={t}
        onClose={closeDrawer}
        rows={[
          { label: t('idLabel'), value: detail?.id },
          { label: t('companyName'), value: detail?.name },
          { label: t('owner'), value: detail?.owner },
          { label: t('status'), value: translateStatus(t, detail?.status) },
          { label: t('amount'), value: detail ? formatMoney(detail.value) : '-' },
        ]}
        actions={[
          { label: t('customer360OpenPage'), onClick: () => detail && openFullDetail(detail) },
          { label: t('quickCreateFollowUp'), onClick: () => detail && quickCreateFollowUp && quickCreateFollowUp(detail) },
          { label: t('quickCreateTask'), onClick: () => detail && quickCreateTask && quickCreateTask(detail) },
        ]}
        extra={
          detail ? (
            <Customer360View
              t={t}
              customer={detail}
              vm={customer360ViewModel}
              loading={timelineLoading || detailLoading}
              moduleMeta={detailModuleMeta}
              mode="drawer"
              onBackToList={() => null}
              onOpenFullDetail={() => openFullDetail(detail)}
              onQuickCreateFollowUp={() => {
                if (!detail || !quickCreateFollowUp) return
                quickCreateFollowUp(detail)
                executeCustomer360Action('followup')
              }}
              onQuickCreateTask={() => {
                if (!detail || !quickCreateTask) return
                quickCreateTask(detail)
                executeCustomer360Action('task')
              }}
              onQuickCreateQuote={() => {
                if (!detail) return
                navigateTo('quotes', { customerId: detail.id, owner: detail.owner || '', page: 1 })
                executeCustomer360Action('quote')
              }}
              onQuickViewOrders={() => detail && navigateTo('orders', { customerId: detail.id, owner: detail.owner || '', page: 1 })}
              onQuickUrgeApproval={() => {
                if (!detail) return
                navigateTo('approvals', { status: 'PENDING', customerId: detail.id, page: 1 })
                executeCustomer360Action('urgeApproval')
              }}
              onNavigatePrev={() => openNeighbor(-1)}
              onNavigateNext={() => openNeighbor(1)}
              onNavigateTarget={navigateTo}
              onRefreshModules={refreshCustomer360Modules}
              onRefresh={() => {
                if (!detail) return
                executeCustomer360Action('manualRefresh')
              }}
            />
          ) : null
        }
      />
      <BatchResultModal t={t} open={batchModalOpen} summary={batchSummary} onClose={() => { setBatchModalOpen(false); clearSummary() }} />
    </section>
  )
}

export default memo(CustomersPanel)

