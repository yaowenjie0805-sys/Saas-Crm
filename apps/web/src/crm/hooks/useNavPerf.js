import { useCallback, useRef } from 'react'
import {
  createNavPerfMetrics,
  recomputeCustomer360Risk,
  syncCustomer360ModuleSummaries,
  trackRatio,
} from './navPerfHelpers'

export function useNavPerf(logPrefix = '[perf]') {
  const navPerfRef = useRef({ from: '', to: '', startedAt: 0 })
  const abortCountRef = useRef(0)
  const metricsRef = useRef(createNavPerfMetrics())

  const syncCustomer360ModuleSummariesRef = useCallback(() => {
    syncCustomer360ModuleSummaries(metricsRef.current)
  }, [])

  const recomputeCustomer360RiskRef = useCallback(() => {
    recomputeCustomer360Risk(metricsRef.current)
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
    recomputeCustomer360RiskRef()
  }, [recomputeCustomer360RiskRef])

  const markCustomer360ActionResult = useCallback((success) => {
    metricsRef.current.customer360_action_success_rate = trackRatio(metricsRef.current.customer360ActionTracker, success)
  }, [])

  const markCustomer360ModuleRefreshLatency = useCallback((moduleName, ms) => {
    const value = Math.max(0, Math.round(Number(ms || 0)))
    metricsRef.current.customer360_module_refresh_latency = value
    if (moduleName && metricsRef.current.customer360ModuleCounters[moduleName]) {
      metricsRef.current.customer360ModuleCounters[moduleName].refreshLatencyMs = value
      syncCustomer360ModuleSummariesRef()
    }
  }, [syncCustomer360ModuleSummariesRef])

  const markCustomer360JumpHit = useCallback((hit) => {
    metricsRef.current.customer360_jump_hit_rate = trackRatio(metricsRef.current.customer360JumpTracker, hit)
    recomputeCustomer360RiskRef()
  }, [recomputeCustomer360RiskRef])

  const markCustomer360ModuleCacheHit = useCallback((hit, moduleName = '') => {
    metricsRef.current.customer360_module_cache_hit_rate = trackRatio(metricsRef.current.customer360ModuleCacheTracker, hit)
    if (moduleName && metricsRef.current.customer360ModuleCounters[moduleName]) {
      metricsRef.current.customer360ModuleCounters[moduleName].cacheTotal += 1
      if (hit) metricsRef.current.customer360ModuleCounters[moduleName].cacheHits += 1
      syncCustomer360ModuleSummariesRef()
    }
    recomputeCustomer360RiskRef()
  }, [recomputeCustomer360RiskRef, syncCustomer360ModuleSummariesRef])

  const markCustomer360PrefetchHit = useCallback((hit, moduleName = '') => {
    metricsRef.current.customer360_prefetch_hit_rate = trackRatio(metricsRef.current.customer360PrefetchTracker, hit)
    if (moduleName && metricsRef.current.customer360ModuleCounters[moduleName]) {
      metricsRef.current.customer360ModuleCounters[moduleName].prefetchTotal += 1
      if (hit) metricsRef.current.customer360ModuleCounters[moduleName].prefetchHits += 1
      syncCustomer360ModuleSummariesRef()
    }
    recomputeCustomer360RiskRef()
  }, [recomputeCustomer360RiskRef, syncCustomer360ModuleSummariesRef])

  const markCustomer360PrefetchAbort = useCallback((moduleName = '') => {
    metricsRef.current.customer360_prefetch_abort_count += 1
    if (moduleName && metricsRef.current.customer360ModuleCounters[moduleName]) {
      metricsRef.current.customer360ModuleCounters[moduleName].prefetchAbortCount += 1
      syncCustomer360ModuleSummariesRef()
    }
  }, [syncCustomer360ModuleSummariesRef])

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
