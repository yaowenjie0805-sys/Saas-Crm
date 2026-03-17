import { useCallback, useEffect } from 'react'

export function useLoaderOrchestrator({
  authToken,
  activePage,
  commonPageLoaders,
  keyPageLoaders,
  loadReasonRef,
  beginPageRequest,
  canSkipFetch,
  isInFlight,
  markInFlight,
  clearInFlight,
  markCacheDecision,
  markDuplicateFetchBlocked,
  markWorkbenchJumpDecision,
  markFetched,
  markFetchLatency,
  markAbort,
  markLoaderFallbackUsed,
  handleError,
  setLastRefreshReason,
  setCurrentLoaderKey,
  setCurrentPageSignature,
  setCurrentSignatureHit,
  refreshReasons,
  onLoaderLifecycle,
  markRefreshSourceAnomaly,
}) {
  const runRegisteredLoader = useCallback((pageKey, loader, options = {}) => {
    const delay = Number(options.delay || 0)
    const force = !!options.force
    const reason = String(options.reason || 'default')

    setLastRefreshReason(reason)
    setCurrentLoaderKey(pageKey)
    setCurrentPageSignature(loader?.signature || '')

    const shouldSkip = canSkipFetch(pageKey, loader.signature, force)
    setCurrentSignatureHit(shouldSkip)
    markCacheDecision(shouldSkip)
    if (shouldSkip) {
      onLoaderLifecycle?.({
        event: 'blocked',
        pageKey,
        reason,
        signature: loader?.signature || '',
      })
      markDuplicateFetchBlocked(reason)
      if (reason === 'workbench_jump') markWorkbenchJumpDecision(true)
      return undefined
    }
    if (!force && isInFlight(pageKey, loader.signature)) {
      markDuplicateFetchBlocked(reason)
      return undefined
    }
    markInFlight(pageKey, loader.signature)
    onLoaderLifecycle?.({
      event: 'start',
      pageKey,
      reason,
      signature: loader?.signature || '',
    })

    const run = async () => {
      const startedAt = typeof performance !== 'undefined' ? performance.now() : Date.now()
      const controller = beginPageRequest(pageKey)
      try {
        await loader.run(controller)
        markFetched(pageKey, loader.signature)
        if (reason === 'workbench_jump') markWorkbenchJumpDecision(false)
        const endedAt = typeof performance !== 'undefined' ? performance.now() : Date.now()
        markFetchLatency(endedAt - startedAt)
        onLoaderLifecycle?.({
          event: 'success',
          pageKey,
          reason,
          signature: loader?.signature || '',
        })
      } catch (err) {
        if (err?.name === 'AbortError') {
          markAbort(pageKey)
          onLoaderLifecycle?.({
            event: 'abort',
            pageKey,
            reason,
            signature: loader?.signature || '',
          })
        } else {
          onLoaderLifecycle?.({
            event: 'error',
            pageKey,
            reason,
            signature: loader?.signature || '',
            message: err?.message || '',
          })
          handleError(err)
        }
      } finally {
        clearInFlight(pageKey, loader.signature)
      }
    }

    if (delay > 0) {
      const timer = setTimeout(run, delay)
      return () => clearTimeout(timer)
    }
    run()
    return undefined
  }, [beginPageRequest, canSkipFetch, isInFlight, markInFlight, clearInFlight, markAbort, markCacheDecision, markDuplicateFetchBlocked, markFetched, markFetchLatency, markWorkbenchJumpDecision, handleError, setCurrentLoaderKey, setCurrentPageSignature, setCurrentSignatureHit, setLastRefreshReason, onLoaderLifecycle])

  useEffect(() => {
    if (!authToken) return
    const loader = commonPageLoaders[activePage]
    if (!loader) return
    const reason = loadReasonRef.current
    loadReasonRef.current = 'default'
    return runRegisteredLoader(activePage, loader, { delay: loader.delay, reason })
  }, [authToken, activePage, commonPageLoaders, loadReasonRef, runRegisteredLoader])

  useEffect(() => {
    if (!authToken) return
    const loader = keyPageLoaders[activePage]
    if (!loader || !loader.canRun()) return
    const reason = loadReasonRef.current
    loadReasonRef.current = 'default'
    return runRegisteredLoader(activePage, loader, { reason })
  }, [authToken, activePage, keyPageLoaders, loadReasonRef, runRegisteredLoader])

  const refreshPage = useCallback(async (pageKey, reason = 'panel_action') => {
    if (!authToken) return false
    const normalized = String(pageKey || activePage || '').trim()
    if (!normalized) return false
    const isKnownReason = refreshReasons.has(reason)
    const normalizedReason = isKnownReason ? reason : 'panel_action'
    if (!isKnownReason) markRefreshSourceAnomaly?.(reason)

    try {
      const common = commonPageLoaders[normalized]
      if (common) {
        loadReasonRef.current = normalizedReason
        runRegisteredLoader(normalized, common, { force: true, delay: 0, reason: normalizedReason })
        return true
      }

      const keyLoader = keyPageLoaders[normalized]
      if (keyLoader && keyLoader.canRun()) {
        loadReasonRef.current = normalizedReason
        runRegisteredLoader(normalized, keyLoader, { force: true, delay: 0, reason: normalizedReason })
        return true
      }

      markLoaderFallbackUsed()
      markRefreshSourceAnomaly?.(normalizedReason)
    } catch (err) {
      handleError(err)
      return false
    }
    return false
  }, [activePage, authToken, commonPageLoaders, handleError, keyPageLoaders, loadReasonRef, markLoaderFallbackUsed, refreshReasons, runRegisteredLoader, markRefreshSourceAnomaly])

  return {
    runRegisteredLoader,
    refreshPage,
  }
}
