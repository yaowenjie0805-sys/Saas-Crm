import { useCallback, useRef } from 'react'

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
  markChunkPreloadHit,
  markNavStart,
  activePage,
  loadReasonRef,
  setActivePage,
  navigate,
  pageToPath,
  pageChunkPreloaders,
}) {
  const preloadedPagesRef = useRef(new Set())

  const prefetchPageChunk = useCallback((pageKey) => {
    const loader = pageChunkPreloaders?.[pageKey]
    if (!loader || preloadedPagesRef.current.has(pageKey)) return
    preloadedPagesRef.current.add(pageKey)
    loader().then(() => {
      markChunkPreloadHit()
      if (typeof window !== 'undefined' && window.console?.debug) {
        window.console.debug('[perf] chunk_preload_hit', { page: pageKey })
      }
    }).catch(() => {
      preloadedPagesRef.current.delete(pageKey)
    })
  }, [markChunkPreloadHit, pageChunkPreloaders])

  const prefetchPageData = useCallback((pageKey) => {
    if (!authToken) return
    if (pageKey === 'customers') {
      const signature = pageSignature('customers', { q: customerQ, status: customerStatus }, 1, customerSize)
      if (canSkipFetch('customers', signature)) return
      loadCustomers(1, customerSize).then(() => markFetched('customers', signature)).catch(() => {})
      return
    }
    if (pageKey === 'approvals') {
      Promise.all([loadApprovalTemplates(), loadApprovalStats()]).catch(() => {})
    }
  }, [authToken, pageSignature, customerQ, customerStatus, customerSize, canSkipFetch, loadCustomers, markFetched, loadApprovalTemplates, loadApprovalStats])

  const onPrefetch = useCallback((pageKey) => {
    prefetchPageChunk(pageKey)
    prefetchPageData(pageKey)
  }, [prefetchPageChunk, prefetchPageData])

  const onNavigate = useCallback((key) => {
    markNavStart(activePage, key)
    loadReasonRef.current = 'sidebar_nav'
    setActivePage(key)
    navigate(pageToPath[key] || pageToPath.dashboard)
  }, [markNavStart, activePage, loadReasonRef, setActivePage, navigate, pageToPath])

  return {
    onPrefetch,
    onNavigate,
  }
}

