import { useCallback, useRef } from 'react'

const CUSTOMER360_WARN_THRESHOLDS = {
  jumpHitRate: 70,
  moduleCacheHitRate: 40,
  prefetchHitRate: 35,
  refreshAnomalyCount: 5,
}
const CUSTOMER360_MODULES = [
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

function createRatioTracker() {
  return { hits: 0, total: 0 }
}

function trackRatio(tracker, hit) {
  tracker.total += 1
  if (hit) tracker.hits += 1
  return tracker.total > 0 ? Math.round((tracker.hits / tracker.total) * 100) : 0
}

function createCustomer360ModuleCounters() {
  return CUSTOMER360_MODULES.reduce((acc, moduleName) => {
    acc[moduleName] = {
      cacheHits: 0,
      cacheTotal: 0,
      prefetchHits: 0,
      prefetchTotal: 0,
      prefetchAbortCount: 0,
      refreshLatencyMs: 0,
    }
    return acc
  }, {})
}

export function useNavPerf(logPrefix = '[perf]') {
  const navPerfRef = useRef({ from: '', to: '', startedAt: 0 })
  const abortCountRef = useRef(0)
  const metricsRef = useRef({
    nav_click_to_first_paint: 0,
    active_page_fetch_ms: 0,
    cache_hit_rate: 0,
    api_abort_count: 0,
    chunk_preload_hit: 0,
    workbench_jump_hit_rate: 0,
    workbench_jump_duplicate_blocked_count: 0,
    workbench_jump_anomaly_count: 0,
    workbench_action_success_rate: 0,
    customer360_action_success_rate: 0,
    customer360_module_refresh_latency: 0,
    customer360_jump_hit_rate: 0,
    customer360_module_cache_hit_rate: 0,
    customer360_prefetch_hit_rate: 0,
    customer360_prefetch_abort_count: 0,
    customer360_regression_risk: 'OK',
    customer360_warning_flags: [],
    customer360_prefetch_modules: [],
    customer360_module_metrics: {},
    customer360_module_cache_hit_lowest: [],
    customer360_module_refresh_latency_highest: [],
    page_render_commit_count: 0,
    page_render_commit_by_page: {},
    page_first_interactive_ms: 0,
    duplicate_fetch_blocked_count: 0,
    polling_active_instances: 0,
    login_error_leak_blocked_count: 0,
    loader_fallback_used_count: 0,
    auth_channel_misroute_count: 0,
    refresh_source_anomaly_count: 0,
    cacheTracker: createRatioTracker(),
    workbenchJumpTracker: createRatioTracker(),
    workbenchActionTracker: createRatioTracker(),
    customer360ActionTracker: createRatioTracker(),
    customer360JumpTracker: createRatioTracker(),
    customer360ModuleCacheTracker: createRatioTracker(),
    customer360PrefetchTracker: createRatioTracker(),
    customer360ModuleCounters: createCustomer360ModuleCounters(),
  })

  const syncCustomer360ModuleSummaries = useCallback(() => {
    const counters = metricsRef.current.customer360ModuleCounters || {}
    const moduleMetrics = Object.keys(counters).reduce((acc, moduleName) => {
      const row = counters[moduleName] || {}
      const cacheHitRate = Number(row.cacheTotal || 0) > 0
        ? Math.round((Number(row.cacheHits || 0) / Number(row.cacheTotal || 0)) * 100)
        : 0
      const prefetchHitRate = Number(row.prefetchTotal || 0) > 0
        ? Math.round((Number(row.prefetchHits || 0) / Number(row.prefetchTotal || 0)) * 100)
        : 0
      acc[moduleName] = {
        cache_hit_rate: cacheHitRate,
        prefetch_hit_rate: prefetchHitRate,
        prefetch_abort_count: Number(row.prefetchAbortCount || 0),
        refresh_latency_ms: Number(row.refreshLatencyMs || 0),
      }
      return acc
    }, {})
    metricsRef.current.customer360_module_metrics = moduleMetrics
    const lowestCache = Object.entries(moduleMetrics)
      .sort((a, b) => a[1].cache_hit_rate - b[1].cache_hit_rate)
      .slice(0, 3)
      .map(([moduleName, row]) => ({ module: moduleName, value: row.cache_hit_rate }))
    const highestLatency = Object.entries(moduleMetrics)
      .sort((a, b) => b[1].refresh_latency_ms - a[1].refresh_latency_ms)
      .slice(0, 3)
      .map(([moduleName, row]) => ({ module: moduleName, value: row.refresh_latency_ms }))
    metricsRef.current.customer360_module_cache_hit_lowest = lowestCache
    metricsRef.current.customer360_module_refresh_latency_highest = highestLatency
  }, [])

  const recomputeCustomer360Risk = useCallback(() => {
    const jumpHitRate = Number(metricsRef.current.customer360_jump_hit_rate || 0)
    const moduleCacheHitRate = Number(metricsRef.current.customer360_module_cache_hit_rate || 0)
    const prefetchHitRate = Number(metricsRef.current.customer360_prefetch_hit_rate || 0)
    const refreshAnomalyCount = Number(metricsRef.current.refresh_source_anomaly_count || 0)
    const flags = []
    if (jumpHitRate < CUSTOMER360_WARN_THRESHOLDS.jumpHitRate) flags.push('jump_hit_low')
    if (moduleCacheHitRate < CUSTOMER360_WARN_THRESHOLDS.moduleCacheHitRate) flags.push('module_cache_low')
    if (prefetchHitRate < CUSTOMER360_WARN_THRESHOLDS.prefetchHitRate) flags.push('prefetch_low')
    if (refreshAnomalyCount > CUSTOMER360_WARN_THRESHOLDS.refreshAnomalyCount) flags.push('refresh_anomaly_high')
    metricsRef.current.customer360_warning_flags = flags
    metricsRef.current.customer360_regression_risk = flags.length ? 'WARN' : 'OK'
  }, [])

  const markNavStart = useCallback((from, to) => {
    navPerfRef.current = {
      from: from || '',
      to: to || '',
      startedAt: typeof performance !== 'undefined' ? performance.now() : Date.now(),
    }
  }, [])

  const markNavEnd = useCallback((activePage) => {
    const current = navPerfRef.current
    if (!current?.to || current.to !== activePage) return null
    const end = typeof performance !== 'undefined' ? performance.now() : Date.now()
    const durationMs = Math.round(end - (current.startedAt || end))
    metricsRef.current.nav_click_to_first_paint = durationMs
    if (typeof window !== 'undefined' && window.console?.debug) {
      window.console.debug(`${logPrefix} nav_click_to_first_paint`, {
        from: current.from,
        to: activePage,
        durationMs,
      })
    }
    navPerfRef.current = { from: '', to: '', startedAt: 0 }
    return durationMs
  }, [logPrefix])

  const markAbort = useCallback((page) => {
    abortCountRef.current += 1
    metricsRef.current.api_abort_count = abortCountRef.current
    if (typeof window !== 'undefined' && window.console?.debug) {
      window.console.debug(`${logPrefix} api_abort_count`, {
        count: abortCountRef.current,
        page,
      })
    }
  }, [logPrefix])

  const markFetchLatency = useCallback((ms) => {
    metricsRef.current.active_page_fetch_ms = Math.max(0, Math.round(Number(ms || 0)))
  }, [])

  const markRenderCommit = useCallback((page) => {
    metricsRef.current.page_render_commit_count += 1
    const pageKey = String(page || 'unknown')
    const byPage = { ...(metricsRef.current.page_render_commit_by_page || {}) }
    byPage[pageKey] = Number(byPage[pageKey] || 0) + 1
    metricsRef.current.page_render_commit_by_page = byPage
  }, [])

  const markFirstInteractive = useCallback((ms) => {
    const value = Math.max(0, Math.round(Number(ms || 0)))
    if (value === 0) return
    metricsRef.current.page_first_interactive_ms = value
  }, [])

  const markCacheDecision = useCallback((hit) => {
    metricsRef.current.cache_hit_rate = trackRatio(metricsRef.current.cacheTracker, hit)
  }, [])

  const markChunkPreloadHit = useCallback(() => {
    metricsRef.current.chunk_preload_hit += 1
  }, [])

  const markDuplicateFetchBlocked = useCallback((reason = '') => {
    metricsRef.current.duplicate_fetch_blocked_count += 1
    if (String(reason || '') === 'workbench_jump') {
      metricsRef.current.workbench_jump_duplicate_blocked_count += 1
    }
  }, [])

  const markWorkbenchJumpDecision = useCallback((hit) => {
    metricsRef.current.workbench_jump_hit_rate = trackRatio(metricsRef.current.workbenchJumpTracker, hit)
  }, [])

  const markWorkbenchActionResult = useCallback((success) => {
    metricsRef.current.workbench_action_success_rate = trackRatio(metricsRef.current.workbenchActionTracker, success)
  }, [])

  const markPollingActiveInstances = useCallback((count) => {
    metricsRef.current.polling_active_instances = Math.max(0, Number(count || 0))
  }, [])

  const markLoginErrorLeakBlocked = useCallback(() => {
    metricsRef.current.login_error_leak_blocked_count += 1
  }, [])

  const markLoaderFallbackUsed = useCallback(() => {
    metricsRef.current.loader_fallback_used_count += 1
  }, [])

  const markAuthChannelMisroute = useCallback(() => {
    metricsRef.current.auth_channel_misroute_count += 1
  }, [])

  const markRefreshSourceAnomaly = useCallback((reason = '') => {
    metricsRef.current.refresh_source_anomaly_count += 1
    if (String(reason || '') === 'workbench_jump') {
      metricsRef.current.workbench_jump_anomaly_count += 1
    }
    recomputeCustomer360Risk()
  }, [recomputeCustomer360Risk])

  const markCustomer360ActionResult = useCallback((success) => {
    metricsRef.current.customer360_action_success_rate = trackRatio(metricsRef.current.customer360ActionTracker, success)
  }, [])

  const markCustomer360ModuleRefreshLatency = useCallback((moduleName, ms) => {
    const value = Math.max(0, Math.round(Number(ms || 0)))
    metricsRef.current.customer360_module_refresh_latency = value
    if (moduleName && metricsRef.current.customer360ModuleCounters[moduleName]) {
      metricsRef.current.customer360ModuleCounters[moduleName].refreshLatencyMs = value
      syncCustomer360ModuleSummaries()
    }
  }, [syncCustomer360ModuleSummaries])

  const markCustomer360JumpHit = useCallback((hit) => {
    metricsRef.current.customer360_jump_hit_rate = trackRatio(metricsRef.current.customer360JumpTracker, hit)
    recomputeCustomer360Risk()
  }, [recomputeCustomer360Risk])

  const markCustomer360ModuleCacheHit = useCallback((hit, moduleName = '') => {
    metricsRef.current.customer360_module_cache_hit_rate = trackRatio(metricsRef.current.customer360ModuleCacheTracker, hit)
    if (moduleName && metricsRef.current.customer360ModuleCounters[moduleName]) {
      metricsRef.current.customer360ModuleCounters[moduleName].cacheTotal += 1
      if (hit) metricsRef.current.customer360ModuleCounters[moduleName].cacheHits += 1
      syncCustomer360ModuleSummaries()
    }
    recomputeCustomer360Risk()
  }, [recomputeCustomer360Risk, syncCustomer360ModuleSummaries])

  const markCustomer360PrefetchHit = useCallback((hit, moduleName = '') => {
    metricsRef.current.customer360_prefetch_hit_rate = trackRatio(metricsRef.current.customer360PrefetchTracker, hit)
    if (moduleName && metricsRef.current.customer360ModuleCounters[moduleName]) {
      metricsRef.current.customer360ModuleCounters[moduleName].prefetchTotal += 1
      if (hit) metricsRef.current.customer360ModuleCounters[moduleName].prefetchHits += 1
      syncCustomer360ModuleSummaries()
    }
    recomputeCustomer360Risk()
  }, [recomputeCustomer360Risk, syncCustomer360ModuleSummaries])

  const markCustomer360PrefetchAbort = useCallback((moduleName = '') => {
    metricsRef.current.customer360_prefetch_abort_count += 1
    if (moduleName && metricsRef.current.customer360ModuleCounters[moduleName]) {
      metricsRef.current.customer360ModuleCounters[moduleName].prefetchAbortCount += 1
      syncCustomer360ModuleSummaries()
    }
  }, [syncCustomer360ModuleSummaries])

  const markCustomer360PrefetchModules = useCallback((modules) => {
    metricsRef.current.customer360_prefetch_modules = Array.isArray(modules) ? [...modules] : []
  }, [])

  const getMetrics = useCallback(() => ({ ...metricsRef.current }), [])

  return {
    markNavStart,
    markNavEnd,
    markAbort,
    markFetchLatency,
    markRenderCommit,
    markFirstInteractive,
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
  }
}
