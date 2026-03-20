import { useCallback, useEffect } from 'react'

export function usePerfPanel({
  activePage,
  markNavEnd,
  markRenderCommit,
  markFirstInteractive,
  devEnabled,
  getMetrics,
  setPerfMetrics,
  currentLoaderKey,
  lastRefreshReason,
  currentPageSignature,
  domainLoadSource,
  setLastPerfSnapshotAt,
}) {
  useEffect(() => {
    markNavEnd(activePage)
  }, [activePage, markNavEnd])

  useEffect(() => {
    if (typeof markRenderCommit === 'function') {
      markRenderCommit(activePage)
    }
  }, [activePage, markRenderCommit])

  useEffect(() => {
    const startedAt = (typeof performance !== 'undefined' && typeof performance.timeOrigin === 'number')
      ? performance.timeOrigin
      : Date.now()
    if (typeof markFirstInteractive === 'function') {
      markFirstInteractive(Date.now() - startedAt)
    }
  }, [activePage, markFirstInteractive])

  useEffect(() => {
    if (!devEnabled) return undefined
    const timer = setInterval(() => setPerfMetrics(getMetrics()), 1000)
    return () => clearInterval(timer)
  }, [devEnabled, getMetrics, setPerfMetrics])

  const copyPerfSnapshot = useCallback(async () => {
    if (typeof navigator === 'undefined' || !navigator.clipboard?.writeText) return
    const metrics = getMetrics()
    const snapshot = {
      at: new Date().toISOString(),
      activePage,
      currentLoaderKey,
      lastRefreshReason,
      currentPageSignature,
      domainLoadSource,
      metrics,
      customer360: {
        modules: metrics.customer360_module_metrics || {},
        prefetchModules: metrics.customer360_prefetch_modules || [],
      },
    }
    await navigator.clipboard.writeText(JSON.stringify(snapshot, null, 2))
    setLastPerfSnapshotAt(snapshot.at)
  }, [activePage, currentLoaderKey, lastRefreshReason, currentPageSignature, domainLoadSource, getMetrics, setLastPerfSnapshotAt])

  return {
    copyPerfSnapshot,
  }
}
