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
import {
  buildCustomer360ViewModel,
  createCustomer360PrefetchLoaders,
  createResilientListLoader,
  EMPTY_CUSTOMER360_MODULES,
  createCustomer360ModuleLoaders,
  filterRequestedModules,
  moduleServerFilterSignature,
  getCustomer360PrefetchSignature,
  orderCustomer360Modules,
  rebalanceAdaptivePrefetchModules,
} from './customer360DataHelpers'

const MODULE_CACHE_MAX_ENTRIES = CUSTOMER360_MODULE_KEYS.length * 20
const PREFETCH_SIGNATURE_MAX_ENTRIES = CUSTOMER360_PREFETCH_MODULES.length * 20
const buildPrefetchTrackingKey = (customerIdText, moduleName) => `${customerIdText}:${moduleName}`

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
  const [detailModules, setDetailModules] = useState(EMPTY_CUSTOMER360_MODULES)
  const [detailModuleMeta, setDetailModuleMeta] = useState(() => createCustomer360ModuleMeta())
  const detailsAbortRef = useRef(null)
  const prefetchAbortRef = useRef(null)
  const activePrefetchModulesRef = useRef(new Map())
  const moduleCacheRef = useRef(new Map())
  const prefetchedSignaturesRef = useRef(new Map())
  const pruneCacheEntries = useCallback((store, ttlMs, maxEntries, now = Date.now()) => {
    if (!store.size) return
    for (const [key, entry] of store) {
      if (now - entry.at > ttlMs) {
        store.delete(key)
      }
    }
    if (store.size <= maxEntries) return
    const sortedEntries = [...store.entries()].sort(([, a], [, b]) => (a.at || 0) - (b.at || 0))
    let index = 0
    while (store.size > maxEntries && index < sortedEntries.length) {
      store.delete(sortedEntries[index][0])
      index += 1
    }
  }, [])
  const cleanupModuleCache = useCallback((now = Date.now()) => {
    pruneCacheEntries(moduleCacheRef.current, CUSTOMER360_MODULE_TTL_MS, MODULE_CACHE_MAX_ENTRIES, now)
  }, [pruneCacheEntries])
  const cleanupPrefetchedSignatures = useCallback((now = Date.now()) => {
    pruneCacheEntries(prefetchedSignaturesRef.current, CUSTOMER360_MODULE_TTL_MS, PREFETCH_SIGNATURE_MAX_ENTRIES, now)
  }, [pruneCacheEntries])
  const isPrefetchedSignatureFresh = (signature, now = Date.now()) => {
    const entry = prefetchedSignaturesRef.current.get(signature)
    if (!entry) return false
    if (now - entry.at > CUSTOMER360_MODULE_TTL_MS) {
      prefetchedSignaturesRef.current.delete(signature)
      return false
    }
    return true
  }
  const trackPrefetchedSignature = useCallback((signature, now = Date.now()) => {
    prefetchedSignaturesRef.current.set(signature, { at: now })
    cleanupPrefetchedSignatures(now)
  }, [cleanupPrefetchedSignatures])
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
    const moduleNamesToAbort = new Set(activePrefetchModulesRef.current.values())
    moduleNamesToAbort.forEach((moduleName) => markCustomer360PrefetchAbort?.(moduleName))
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
    const requestedModules = filterRequestedModules(options.modules, CUSTOMER360_MODULE_KEYS)
    const fullRefresh = requestedModules.length === CUSTOMER360_MODULE_KEYS.length
    const shouldReset = !!options.resetModules
    const force = !!options.force
    const source = options.source || 'open'
    const customerIdText = String(customerId)
    const customerChanged = detailsCustomerIdRef.current !== customerIdText
    if (shouldReset || customerChanged) {
      detailsCustomerIdRef.current = customerIdText
      setDetailModules(EMPTY_CUSTOMER360_MODULES)
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
      const loadList = createResilientListLoader({ api, controller, token, lang, toList })
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
      const moduleLoaders = createCustomer360ModuleLoaders({
        customerIdText,
        maxAssocItems: MAX_ASSOC_ITEMS,
        timedLoad,
        toArray,
      })
      const loadOneModule = async (moduleName) => {
        const now = Date.now()
        const signature = buildCustomer360ModuleSignature(
          customerIdText,
          moduleName,
          moduleServerFilterSignature(moduleName, MAX_ASSOC_ITEMS),
        )
        cleanupModuleCache(now)
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
          const prefetchedHit = cacheHit && isPrefetchedSignatureFresh(signature)
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
          const loadedAt = Date.now()
          moduleCacheRef.current.set(signature, { at: loadedAt, signature, data })
          cleanupModuleCache(loadedAt)
          setDetailModules((prev) => ({ ...prev, [moduleName]: data }))
          setDetailModuleMeta((prev) => ({ ...prev, [moduleName]: { ...(prev[moduleName] || {}), loading: false, error: '', signature, lastLoadedAt: loadedAt } }))
        } catch (error) {
          if (controller.signal.aborted) return
          const message = error?.requestId ? `${error?.message || t('loadFailed')} [${error.requestId}]` : (error?.message || t('loadFailed'))
          setDetailModuleMeta((prev) => ({ ...prev, [moduleName]: { ...(prev[moduleName] || {}), loading: false, error: message, signature, lastLoadedAt: Date.now() } }))
        }
      }
      const orderedModules = orderCustomer360Modules(
        requestedModules,
        CUSTOMER360_PRIMARY_MODULES,
        CUSTOMER360_SECONDARY_MODULES,
      )
      await Promise.all(orderedModules.map((moduleName) => loadOneModule(moduleName)))
      if (import.meta.env.DEV) {
        adaptivePrefetchModulesRef.current = rebalanceAdaptivePrefetchModules({
          currentModules: adaptivePrefetchModulesRef.current,
          preferredModules: CUSTOMER360_PREFETCH_MODULES,
          statsRef: adaptiveModuleStatsRef,
          adaptiveWindow: CUSTOMER360_ADAPTIVE_WINDOW,
          lowHitThreshold: CUSTOMER360_PREFETCH_LOW_HIT,
          highHitThreshold: CUSTOMER360_PREFETCH_HIGH_HIT,
          highLatencyThreshold: CUSTOMER360_PREFETCH_HIGH_LATENCY,
        })
        markCustomer360PrefetchModules?.(adaptivePrefetchModulesRef.current)
      }
    } finally {
      const isLatestRequest = detailRequestSeqRef.current === requestSeq
      if ((fullRefresh || shouldReset) && isLatestRequest) setDetailLoading(false)
      if (isLatestRequest) markCustomer360ModuleRefreshLatency?.('all', Math.max(Date.now() - startedAt, 0))
    }
  }, [cleanupModuleCache, lang, markCustomer360ModuleCacheHit, markCustomer360ModuleRefreshLatency, markCustomer360PrefetchHit, markCustomer360PrefetchModules, t, token])

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
    const loadList = createResilientListLoader({ api, controller, token, lang, toList })
    const prefetchLoadersByModule = (customerIdText) => createCustomer360PrefetchLoaders({
      customerIdText,
      maxAssocItems: MAX_ASSOC_ITEMS,
      loadList,
      toArray,
    })
    const prefetchModule = async (customerIdText, moduleName) => {
      const trackingKey = buildPrefetchTrackingKey(customerIdText, moduleName)
      activePrefetchModulesRef.current.set(trackingKey, moduleName)
      try {
        const signature = buildCustomer360ModuleSignature(customerIdText, moduleName, getCustomer360PrefetchSignature(moduleName, MAX_ASSOC_ITEMS))
        const nowAt = Date.now()
        cleanupModuleCache(nowAt)
        const cacheEntry = moduleCacheRef.current.get(signature)
        if (cacheEntry && cacheEntry.signature === signature && (nowAt - cacheEntry.at) <= CUSTOMER360_MODULE_TTL_MS) return
        const prefetchLoaders = prefetchLoadersByModule(customerIdText)
        const loadModule = prefetchLoaders[moduleName]
        if (!loadModule) return
        let data = []
        data = await loadModule()
        if (controller.signal.aborted) return
        const cachedAt = Date.now()
        moduleCacheRef.current.set(signature, { at: cachedAt, signature, data })
        cleanupModuleCache(cachedAt)
        trackPrefetchedSignature(signature, cachedAt)
      } finally {
        activePrefetchModulesRef.current.delete(trackingKey)
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
  }, [abortPrefetchRequests, activePage, cleanupModuleCache, detailMode, lang, rowIndexById, rows, token, trackPrefetchedSignature])

  const scheduleNeighborPrefetch = useCallback((row) => {
    if (!row?.id || detailMode !== 'drawer') return
    if (prefetchTimerRef.current) clearTimeout(prefetchTimerRef.current)
    prefetchTimerRef.current = setTimeout(() => {
      prefetchTimerRef.current = null
      prefetchNeighborModules(row)
    }, 180)
  }, [detailMode, prefetchNeighborModules])

  const customer360ViewModel = useMemo(() => {
    return buildCustomer360ViewModel({
      detailModules,
      timeline,
      t,
      toArray,
      toNumber,
      thirtyDaysMs: THIRTY_DAYS_MS,
    })
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
