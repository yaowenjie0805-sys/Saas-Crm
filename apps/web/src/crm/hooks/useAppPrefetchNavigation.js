import { startTransition, useCallback, useEffect, useRef } from 'react'

export function useAppPrefetchNavigation({
  authToken,
  pageSignature,
  customerQ,
  customerStatus,
  customerSize,
  canSkipFetch,
  loadCustomers,
  markFetched,
  loadApprovalTemplates,
  loadApprovalStats,
  loadLeads,
  leadQ,
  leadStatus,
  leadSize,
  loadOpportunities,
  loadTasks,
  loadWorkbenchToday,
  loadReports,
  markChunkPreloadHit,
  markNavStart,
  activePage,
  loadReasonRef,
  setActivePage,
  navigate,
  pageToPath,
  pageChunkPreloaders,
  pageDomainPreloaders,
  prefetchI18nNamespaces,
}) {
  const preloadedPagesRef = useRef(new Set())
  const prefetchTimersRef = useRef(new Map())
  const inFlightPrefetchRef = useRef(new Set())
  const idlePrefetchBootstrappedRef = useRef(false)

  useEffect(() => () => {
    for (const timer of prefetchTimersRef.current.values()) {
      clearTimeout(timer)
    }
    prefetchTimersRef.current.clear()
  }, [])

  const prefetchPageChunk = useCallback((pageKey) => {
    const loader = pageChunkPreloaders?.[pageKey]
    if (!loader || preloadedPagesRef.current.has(pageKey) || inFlightPrefetchRef.current.has(pageKey)) return
    inFlightPrefetchRef.current.add(pageKey)
    preloadedPagesRef.current.add(pageKey)
    loader().then(() => {
      markChunkPreloadHit()
      if (typeof window !== 'undefined' && window.console?.debug) {
        window.console.debug('[perf] chunk_preload_hit', { page: pageKey })
      }
    }).catch(() => {
      preloadedPagesRef.current.delete(pageKey)
    }).finally(() => {
      inFlightPrefetchRef.current.delete(pageKey)
    })
  }, [markChunkPreloadHit, pageChunkPreloaders])

  const prefetchPageDomain = useCallback((pageKey) => {
    const loader = pageDomainPreloaders?.[pageKey]
    if (!loader) return
    loader().catch(() => {})
  }, [pageDomainPreloaders])

  const prefetchPageData = useCallback((pageKey) => {
    if (!authToken) return
    if (pageKey === 'customers') {
      const signature = pageSignature('customers', { q: customerQ, status: customerStatus }, 1, customerSize)
      if (canSkipFetch('customers', signature)) return
      loadCustomers(1, customerSize).then(() => markFetched('customers', signature)).catch(() => {})
      return
    }
    if (pageKey === 'leads') {
      const signature = pageSignature('leads', { q: leadQ, status: leadStatus }, 1, leadSize)
      if (canSkipFetch('leads', signature)) return
      loadLeads(1, leadSize).then(() => markFetched('leads', signature)).catch(() => {})
      return
    }
    if (pageKey === 'dashboard') {
      Promise.all([loadTasks(), loadWorkbenchToday()]).catch(() => {})
      return
    }
    if (pageKey === 'pipeline') {
      loadOpportunities(1, 10).catch(() => {})
      return
    }
    if (pageKey === 'reports') {
      loadReports().catch(() => {})
      return
    }
    if (pageKey === 'approvals') {
      Promise.all([loadApprovalTemplates(), loadApprovalStats()]).catch(() => {})
    }
  }, [authToken, pageSignature, customerQ, customerStatus, customerSize, canSkipFetch, loadCustomers, markFetched, loadApprovalTemplates, loadApprovalStats, leadQ, leadStatus, leadSize, loadLeads, loadOpportunities, loadTasks, loadWorkbenchToday, loadReports])

  const onPrefetch = useCallback((pageKey) => {
    if (!pageKey || pageKey === activePage) return
    const timer = prefetchTimersRef.current.get(pageKey)
    if (timer) clearTimeout(timer)
    const nextTimer = setTimeout(() => {
      prefetchTimersRef.current.delete(pageKey)
      prefetchPageChunk(pageKey)
      prefetchPageDomain(pageKey)
      prefetchPageData(pageKey)
      prefetchI18nNamespaces?.(pageKey)
    }, 120)
    prefetchTimersRef.current.set(pageKey, nextTimer)
  }, [activePage, prefetchPageChunk, prefetchPageDomain, prefetchPageData, prefetchI18nNamespaces])

  const onNavigate = useCallback((key) => {
    markNavStart(activePage, key)
    loadReasonRef.current = 'sidebar_nav'
    startTransition(() => {
      setActivePage(key)
    })
    navigate(pageToPath[key] || pageToPath.dashboard)
  }, [markNavStart, activePage, loadReasonRef, setActivePage, navigate, pageToPath])

  useEffect(() => {
    if (!authToken || idlePrefetchBootstrappedRef.current) return
    idlePrefetchBootstrappedRef.current = true
    const pages = ['dashboard', 'customers', 'pipeline', 'reports', 'reportDesigner']
    let canceled = false
    const run = () => {
      if (canceled) return
      pages.forEach((pageKey, index) => {
        const timer = window.setTimeout(() => {
          if (canceled) return
          prefetchPageChunk(pageKey)
          prefetchPageDomain(pageKey)
          if (index < 2) prefetchPageData(pageKey)
          prefetchI18nNamespaces?.(pageKey)
        }, 150 * (index + 1))
        prefetchTimersRef.current.set(`idle:${pageKey}`, timer)
      })
    }
    if (typeof window !== 'undefined' && typeof window.requestIdleCallback === 'function') {
      const idleId = window.requestIdleCallback(run, { timeout: 800 })
      return () => {
        canceled = true
        window.cancelIdleCallback(idleId)
      }
    }
    const fallback = window.setTimeout(run, 260)
    return () => {
      canceled = true
      window.clearTimeout(fallback)
    }
  }, [authToken, prefetchPageChunk, prefetchPageDomain, prefetchPageData, prefetchI18nNamespaces])

  return {
    onPrefetch,
    onNavigate,
  }
}
