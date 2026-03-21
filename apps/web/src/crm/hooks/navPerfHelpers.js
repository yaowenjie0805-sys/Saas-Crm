export const CUSTOMER360_WARN_THRESHOLDS = {
  jumpHitRate: 70,
  moduleCacheHitRate: 40,
  prefetchHitRate: 35,
  refreshAnomalyCount: 5,
}

export const CUSTOMER360_MODULES = [
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

export function createRatioTracker() {
  return { hits: 0, total: 0 }
}

export function trackRatio(tracker, hit) {
  tracker.total += 1
  if (hit) tracker.hits += 1
  return tracker.total > 0 ? Math.round((tracker.hits / tracker.total) * 100) : 0
}

export function createCustomer360ModuleCounters() {
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

export function createNavPerfMetrics() {
  return {
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
  }
}

export function syncCustomer360ModuleSummaries(metrics) {
  const counters = metrics.customer360ModuleCounters || {}
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
  metrics.customer360_module_metrics = moduleMetrics
  metrics.customer360_module_cache_hit_lowest = Object.entries(moduleMetrics)
    .sort((a, b) => a[1].cache_hit_rate - b[1].cache_hit_rate)
    .slice(0, 3)
    .map(([moduleName, row]) => ({ module: moduleName, value: row.cache_hit_rate }))
  metrics.customer360_module_refresh_latency_highest = Object.entries(moduleMetrics)
    .sort((a, b) => b[1].refresh_latency_ms - a[1].refresh_latency_ms)
    .slice(0, 3)
    .map(([moduleName, row]) => ({ module: moduleName, value: row.refresh_latency_ms }))
}

export function recomputeCustomer360Risk(metrics) {
  const jumpHitRate = Number(metrics.customer360_jump_hit_rate || 0)
  const moduleCacheHitRate = Number(metrics.customer360_module_cache_hit_rate || 0)
  const prefetchHitRate = Number(metrics.customer360_prefetch_hit_rate || 0)
  const refreshAnomalyCount = Number(metrics.refresh_source_anomaly_count || 0)
  const flags = []
  if (jumpHitRate < CUSTOMER360_WARN_THRESHOLDS.jumpHitRate) flags.push('jump_hit_low')
  if (moduleCacheHitRate < CUSTOMER360_WARN_THRESHOLDS.moduleCacheHitRate) flags.push('module_cache_low')
  if (prefetchHitRate < CUSTOMER360_WARN_THRESHOLDS.prefetchHitRate) flags.push('prefetch_low')
  if (refreshAnomalyCount > CUSTOMER360_WARN_THRESHOLDS.refreshAnomalyCount) flags.push('refresh_anomaly_high')
  metrics.customer360_warning_flags = flags
  metrics.customer360_regression_risk = flags.length ? 'WARN' : 'OK'
}
