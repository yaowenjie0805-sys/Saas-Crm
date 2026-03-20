import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { api } from '../../../../shared'
import {
  MAX_ASSOC_ITEMS,
  THIRTY_DAYS_MS,
  CUSTOMER360_MODULE_KEYS,
  CUSTOMER360_PRIMARY_MODULES,
  CUSTOMER360_SECONDARY_MODULES,
  CUSTOMER360_PREFETCH_MODULES,
  CUSTOMER360_MODULE_TTL_MS,
  CUSTOMER360_ADAPTIVE_WINDOW,
  CUSTOMER360_PREFETCH_LOW_HIT,
  CUSTOMER360_PREFETCH_HIGH_HIT,
  CUSTOMER360_PREFETCH_HIGH_LATENCY,
  CUSTOMER360_ACTION_MODULES,
  createCustomer360ModuleMeta,
  buildCustomer360ModuleSignature,
  toArray,
  toNumber,
  toList,
} from './shared'

const EMPTY_MODULES = {
  contacts: [],
  opportunities: [],
  quotes: [],
  orders: [],
  contracts: [],
  payments: [],
  approvals: [],
  audits: [],
  notifications: [],
}

export default function useCustomer360Data({
  t,
  token,
  lang,
  activePage,
  detailMode,
  detail,
  timeline,
  rows,
  rowIndexById,
  loadTimeline,
  onWorkbenchNavigate,
  buildWorkbenchFilterSignature,
  customer360Metrics,
}) {
  const [timelineLoading, setTimelineLoading] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailModules, setDetailModules] = useState(EMPTY_MODULES)
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
  const markCustomer360ActionResult = customer360Metrics?.markActionResult
  const markCustomer360ModuleRefreshLatency = customer360Metrics?.markModuleRefreshLatency
  const markCustomer360JumpHit = customer360Metrics?.markJumpHit
  const markCustomer360ModuleCacheHit = customer360Metrics?.markModuleCacheHit
  const markCustomer360PrefetchHit = customer360Metrics?.markPrefetchHit
  const markCustomer360PrefetchAbort = customer360Metrics?.markPrefetchAbort
  const markCustomer360PrefetchModules = customer360Metrics?.markPrefetchModules

  useEffect(() => { detailModulesRef.current = detailModules }, [detailModules])
  useEffect(() => { detailModuleMetaRef.current = detailModuleMeta }, [detailModuleMeta])
  useEffect(() => { currentDetailIdRef.current = String(detail?.id || '') }, [detail?.id])
  useEffect(() => { markCustomer360PrefetchModules?.(adaptivePrefetchModulesRef.current) }, [markCustomer360PrefetchModules])

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

  useEffect(() => {
    if (activePage === 'customers') return
    if (detailsAbortRef.current) detailsAbortRef.current.abort()
    abortPrefetchRequests()
  }, [abortPrefetchRequests, activePage])

  useEffect(() => () => {
    if (detailsAbortRef.current) detailsAbortRef.current.abort()
    abortPrefetchRequests()
    if (prefetchTimerRef.current) {
      clearTimeout(prefetchTimerRef.current)
      prefetchTimerRef.current = null
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
      setDetailModules(EMPTY_MODULES)
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
        try { return toList(await api(path, { signal: controller.signal }, token, lang)) } catch {
          if (!fallbackPath) return []
          try { return toList(await api(fallbackPath, { signal: controller.signal }, token, lang)) } catch { return [] }
        }
      }
      const timedLoad = async (moduleName, path, fallbackPath = '') => {
        const moduleStartedAt = Date.now()
        const list = await loadList(path, fallbackPath)
        const latency = Math.max(Date.now() - moduleStartedAt, 0)
        markCustomer360ModuleRefreshLatency?.(moduleName, latency)
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
        quotes: async () => toArray(await timedLoad('quotes', `/v1/quotes?page=1&size=${MAX_ASSOC_ITEMS}&customerId=${encodeURIComponent(customerIdText)}`, '/v1/quotes?page=1&size=30')).filter((item) => String(item.customerId || item.customer?.id || '') === customerIdText).slice(0, MAX_ASSOC_ITEMS),
        orders: async () => toArray(await timedLoad('orders', `/v1/orders?page=1&size=${MAX_ASSOC_ITEMS}&customerId=${encodeURIComponent(customerIdText)}`, '/v1/orders?page=1&size=30')).filter((item) => String(item.customerId || item.customer?.id || '') === customerIdText).slice(0, MAX_ASSOC_ITEMS),
        approvals: async () => toArray(await timedLoad('approvals', '/v1/approval/instances?limit=30')).filter((item) => {
          const bizId = String(item.bizId || '')
          const ref = String(item.refId || '')
          return bizId === customerIdText || ref === customerIdText || bizId.includes(customerIdText) || ref.includes(customerIdText)
        }).slice(0, MAX_ASSOC_ITEMS),
        audits: async () => toArray(await timedLoad('audits', `/audit-logs/search?q=${encodeURIComponent(customerIdText)}&page=1&size=${MAX_ASSOC_ITEMS}`)).slice(0, MAX_ASSOC_ITEMS),
        notifications: async () => toArray(await timedLoad('notifications', '/v1/integrations/notifications/jobs?status=ALL&page=1&size=30')).filter((item) => JSON.stringify(item).includes(customerIdText)).slice(0, MAX_ASSOC_ITEMS),
      }
      const moduleServerFilterSignature = (moduleName) => ({
        contacts: `size=${MAX_ASSOC_ITEMS}`,
        opportunities: `size=${MAX_ASSOC_ITEMS}`,
        contracts: `size=${MAX_ASSOC_ITEMS}`,
        payments: `size=${MAX_ASSOC_ITEMS}`,
        quotes: `size=${MAX_ASSOC_ITEMS}`,
        orders: `size=${MAX_ASSOC_ITEMS}`,
        approvals: 'limit=30',
        audits: `size=${MAX_ASSOC_ITEMS}`,
        notifications: 'status=ALL:size=30',
      }[moduleName] || `size=${MAX_ASSOC_ITEMS}`)
      const loadOneModule = async (moduleName) => {
        const now = Date.now()
        const signature = buildCustomer360ModuleSignature(customerIdText, moduleName, moduleServerFilterSignature(moduleName))
        const cacheEntry = moduleCacheRef.current.get(signature)
        const metaEntry = detailModuleMetaRef.current?.[moduleName]
        const moduleData = detailModulesRef.current?.[moduleName]
        const metaHit = !force && metaEntry && metaEntry.signature === signature && (now - Number(metaEntry.lastLoadedAt || 0)) <= CUSTOMER360_MODULE_TTL_MS && Array.isArray(moduleData)
        const cacheHit = !force && cacheEntry && cacheEntry.signature === signature && (now - cacheEntry.at) <= CUSTOMER360_MODULE_TTL_MS
        const reusedData = cacheHit ? cacheEntry.data : (metaHit ? moduleData : null)
        if (reusedData) {
          setDetailModules((prev) => ({ ...prev, [moduleName]: reusedData }))
          setDetailModuleMeta((prev) => ({ ...prev, [moduleName]: { ...(prev[moduleName] || {}), loading: false, error: '', signature, lastLoadedAt: cacheHit ? cacheEntry.at : Number(metaEntry?.lastLoadedAt || now) } }))
          markCustomer360ModuleCacheHit?.(true, moduleName)
          const prefetchedHit = cacheHit && prefetchedSignaturesRef.current.has(signature)
          if (source === 'open' && adaptivePrefetchModulesRef.current.includes(moduleName)) {
            markCustomer360PrefetchHit?.(prefetchedHit, moduleName)
            if (prefetchedHit) prefetchedSignaturesRef.current.delete(signature)
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
        setDetailModuleMeta((prev) => ({ ...prev, [moduleName]: { ...(prev[moduleName] || {}), loading: true, error: '', signature } }))
        try {
          const data = await moduleLoaders[moduleName]()
          if (controller.signal.aborted) return
          moduleCacheRef.current.set(signature, { at: Date.now(), signature, data })
          setDetailModules((prev) => ({ ...prev, [moduleName]: data }))
          setDetailModuleMeta((prev) => ({ ...prev, [moduleName]: { ...(prev[moduleName] || {}), loading: false, error: '', signature, lastLoadedAt: Date.now() } }))
        } catch (error) {
          if (controller.signal.aborted) return
          const message = error?.requestId ? `${error?.message || t('loadFailed')} [${error.requestId}]` : (error?.message || t('loadFailed'))
          setDetailModuleMeta((prev) => ({ ...prev, [moduleName]: { ...(prev[moduleName] || {}), loading: false, error: message, signature, lastLoadedAt: Date.now() } }))
        }
      }
      const primaryModules = requestedModules.filter((moduleName) => CUSTOMER360_PRIMARY_MODULES.includes(moduleName))
      const secondaryModules = requestedModules.filter((moduleName) => CUSTOMER360_SECONDARY_MODULES.includes(moduleName))
      const remainingModules = requestedModules.filter((moduleName) => !primaryModules.includes(moduleName) && !secondaryModules.includes(moduleName))
      await Promise.all([...primaryModules, ...secondaryModules, ...remainingModules].map((moduleName) => loadOneModule(moduleName)))
      if (import.meta.env.DEV) {
        const next = new Set(adaptivePrefetchModulesRef.current)
        CUSTOMER360_PREFETCH_MODULES.forEach((moduleName) => {
          const stats = adaptiveModuleStatsRef.current[moduleName]
          if (!stats || stats.total < CUSTOMER360_ADAPTIVE_WINDOW) return
          const hitRate = stats.total > 0 ? Math.round((stats.hits / stats.total) * 100) : 0
          const avgLatency = stats.latencyCount > 0 ? Math.round(stats.latencyTotal / stats.latencyCount) : 0
          if (hitRate <= CUSTOMER360_PREFETCH_LOW_HIT && avgLatency <= CUSTOMER360_PREFETCH_HIGH_LATENCY) next.delete(moduleName)
          else if (hitRate >= CUSTOMER360_PREFETCH_HIGH_HIT || avgLatency >= CUSTOMER360_PREFETCH_HIGH_LATENCY) next.add(moduleName)
          adaptiveModuleStatsRef.current[moduleName] = { total: 0, hits: 0, latencyTotal: 0, latencyCount: 0 }
        })
        const normalized = CUSTOMER360_PREFETCH_MODULES.filter((moduleName) => next.has(moduleName))
        adaptivePrefetchModulesRef.current = normalized.length ? normalized : [...CUSTOMER360_PREFETCH_MODULES]
        markCustomer360PrefetchModules?.(adaptivePrefetchModulesRef.current)
      }
    } finally {
      const isLatestRequest = detailRequestSeqRef.current === requestSeq
      if ((fullRefresh || shouldReset) && isLatestRequest) setDetailLoading(false)
      if (isLatestRequest) markCustomer360ModuleRefreshLatency?.('all', Math.max(Date.now() - startedAt, 0))
    }
  }, [lang, markCustomer360ModuleCacheHit, markCustomer360ModuleRefreshLatency, markCustomer360PrefetchHit, markCustomer360PrefetchModules, t, token])

  const refreshCustomer360Modules = useCallback(async (modules = []) => {
    if (!detail?.id) return
    await loadCustomer360(detail, { modules: Array.isArray(modules) && modules.length ? modules : CUSTOMER360_MODULE_KEYS, resetModules: false, force: true, source: 'action' })
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
      try { return toList(await api(path, { signal: controller.signal }, token, lang)) } catch {
        if (!fallbackPath) return []
        try { return toList(await api(fallbackPath, { signal: controller.signal }, token, lang)) } catch { return [] }
      }
    }
    const prefetchModule = async (customerIdText, moduleName) => {
      activePrefetchModulesRef.current.add(moduleName)
      try {
        const signature = buildCustomer360ModuleSignature(customerIdText, moduleName, moduleName === 'notifications' ? 'status=ALL:size=30' : moduleName === 'approvals' ? 'limit=30' : `size=${MAX_ASSOC_ITEMS}`)
        const cacheEntry = moduleCacheRef.current.get(signature)
        const nowAt = Date.now()
        if (cacheEntry && cacheEntry.signature === signature && (nowAt - cacheEntry.at) <= CUSTOMER360_MODULE_TTL_MS) return
        let data = []
        if (moduleName === 'contacts') data = toArray(await loadList(`/contacts/search?customerId=${encodeURIComponent(customerIdText)}&page=1&size=${MAX_ASSOC_ITEMS}`)).slice(0, MAX_ASSOC_ITEMS)
        else if (moduleName === 'opportunities') data = toArray(await loadList(`/opportunities/search?customerId=${encodeURIComponent(customerIdText)}&page=1&size=${MAX_ASSOC_ITEMS}`)).slice(0, MAX_ASSOC_ITEMS)
        else if (moduleName === 'quotes') data = toArray(await loadList(`/v1/quotes?page=1&size=${MAX_ASSOC_ITEMS}&customerId=${encodeURIComponent(customerIdText)}`, '/v1/quotes?page=1&size=30')).filter((item) => String(item.customerId || item.customer?.id || '') === customerIdText).slice(0, MAX_ASSOC_ITEMS)
        else if (moduleName === 'orders') data = toArray(await loadList(`/v1/orders?page=1&size=${MAX_ASSOC_ITEMS}&customerId=${encodeURIComponent(customerIdText)}`, '/v1/orders?page=1&size=30')).filter((item) => String(item.customerId || item.customer?.id || '') === customerIdText).slice(0, MAX_ASSOC_ITEMS)
        if (controller.signal.aborted) return
        moduleCacheRef.current.set(signature, { at: Date.now(), signature, data })
        prefetchedSignaturesRef.current.add(signature)
      } finally {
        activePrefetchModulesRef.current.delete(moduleName)
      }
    }
    try {
      await Promise.all(neighbors.flatMap((row) => {
        const customerIdText = String(row.id)
        return adaptivePrefetchModulesRef.current.map((moduleName) => prefetchModule(customerIdText, moduleName))
      }))
    } catch {
      // prefetch failures are non-blocking
    }
  }, [abortPrefetchRequests, activePage, detailMode, lang, rowIndexById, rows, token])

  const scheduleNeighborPrefetch = useCallback((row) => {
    if (!row?.id || detailMode !== 'drawer') return
    if (prefetchTimerRef.current) clearTimeout(prefetchTimerRef.current)
    prefetchTimerRef.current = setTimeout(() => {
      prefetchTimerRef.current = null
      prefetchNeighborModules(row)
    }, 180)
  }, [detailMode, prefetchNeighborModules])

  const customer360ViewModel = useMemo(() => {
    const now = Date.now()
    const timelineRows = toArray(timeline)
    const followUpsRecent = timelineRows.filter((item) => {
      const dt = new Date(item.time || item.createdAt || '').getTime()
      if (!dt || Number.isNaN(dt)) return false
      return now - dt <= THIRTY_DAYS_MS
    })
    const inFlightAmount = toArray(detailModules.opportunities).filter((item) => !String(item.stage || '').toLowerCase().includes('closed')).reduce((sum, item) => sum + toNumber(item.amount), 0)
    const orderAmount = toArray(detailModules.orders).reduce((sum, item) => sum + toNumber(item.amount), 0)
    const paymentReceived = toArray(detailModules.payments).filter((item) => ['RECEIVED', 'COMPLETED'].includes(String(item.status || '').toUpperCase())).reduce((sum, item) => sum + toNumber(item.amount), 0)
    const pendingApprovals = toArray(detailModules.approvals).filter((item) => ['PENDING', 'WAITING', 'SUBMITTED'].includes(String(item.status || '').toUpperCase())).length
    const paymentOutstanding = Math.max(orderAmount - paymentReceived, 0)
    const riskTags = []
    if (paymentOutstanding > 0) riskTags.push(t('paymentWarnings'))
    if (pendingApprovals > 0) riskTags.push(t('pendingApprovals'))
    if (followUpsRecent.length === 0) riskTags.push(t('overdueFollowUps'))
    return { related: detailModules, timeline: timelineRows, metrics: { recentFollowUps30d: followUpsRecent.length, inFlightAmount, orderAmount, paymentReceived, paymentOutstanding, pendingApprovals }, riskTags }
  }, [detailModules, timeline, t])

  const navigateTo = useCallback((targetPage, payload = {}) => {
    if (!onWorkbenchNavigate) {
      markCustomer360ActionResult?.(false)
      return
    }
    const signature = buildWorkbenchFilterSignature ? buildWorkbenchFilterSignature(targetPage, payload || {}) : `${targetPage}:${JSON.stringify(payload || {})}`
    const hit = customer360JumpRef.current.signature === signature
    customer360JumpRef.current.signature = signature
    markCustomer360JumpHit?.(hit)
    onWorkbenchNavigate(targetPage, payload)
    markCustomer360ActionResult?.(true)
  }, [buildWorkbenchFilterSignature, markCustomer360ActionResult, markCustomer360JumpHit, onWorkbenchNavigate])

  return {
    timelineLoading,
    setTimelineLoading,
    detailLoading,
    detailModules,
    detailModuleMeta,
    loadCustomer360,
    refreshCustomer360Modules,
    executeCustomer360Action,
    scheduleNeighborPrefetch,
    abortPrefetchRequests,
    customer360ViewModel,
    navigateTo,
  }
}
