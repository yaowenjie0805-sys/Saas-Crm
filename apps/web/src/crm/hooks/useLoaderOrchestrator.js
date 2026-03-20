import { useCallback, useEffect, useRef } from 'react'

/**
 * Centralized loader orchestration with dedupe, skip and lifecycle tracking.
 * Stable callbacks are preserved through an internal runtime ref.
 */
export function useLoaderOrchestrator({
  loaders,
  runtime,
  metrics,
  handlers,
}) {
  const stateRef = useRef(null)
  stateRef.current = { loaders, runtime, metrics, handlers }

  const runRegisteredLoader = useCallback((pageKey, loader, options = {}) => {
    const state = stateRef.current
    if (!state) return undefined
    const { runtime: run, metrics: mark, handlers: handle } = state
    const delay = Number(options.delay || 0)
    const force = !!options.force
    const reason = String(options.reason || 'default')

    mark.setLastRefreshReason(reason)
    mark.setCurrentLoaderKey(pageKey)
    mark.setCurrentPageSignature(loader?.signature || '')

    const shouldSkip = run.canSkipFetch(pageKey, loader.signature, force)
    mark.setCurrentSignatureHit(shouldSkip)
    mark.markCacheDecision(shouldSkip)
    if (shouldSkip) {
      handle.onLoaderLifecycle?.({
        event: 'blocked',
        pageKey,
        reason,
        signature: loader?.signature || '',
      })
      mark.markDuplicateFetchBlocked(reason)
      if (reason === 'workbench_jump') mark.markWorkbenchJumpDecision(true)
      return undefined
    }
    if (!force && run.isInFlight(pageKey, loader.signature)) {
      mark.markDuplicateFetchBlocked(reason)
      return undefined
    }
    run.markInFlight(pageKey, loader.signature)
    handle.onLoaderLifecycle?.({
      event: 'start',
      pageKey,
      reason,
      signature: loader?.signature || '',
    })

    const execute = async () => {
      const startedAt = typeof performance !== 'undefined' ? performance.now() : Date.now()
      const controller = run.beginPageRequest(pageKey)
      try {
        await loader.run(controller)
        mark.markFetched(pageKey, loader.signature)
        if (reason === 'workbench_jump') mark.markWorkbenchJumpDecision(false)
        const endedAt = typeof performance !== 'undefined' ? performance.now() : Date.now()
        mark.markFetchLatency(endedAt - startedAt)
        handle.onLoaderLifecycle?.({
          event: 'success',
          pageKey,
          reason,
          signature: loader?.signature || '',
        })
      } catch (err) {
        if (err?.name === 'AbortError') {
          mark.markAbort(pageKey)
          handle.onLoaderLifecycle?.({
            event: 'abort',
            pageKey,
            reason,
            signature: loader?.signature || '',
          })
        } else {
          handle.onLoaderLifecycle?.({
            event: 'error',
            pageKey,
            reason,
            signature: loader?.signature || '',
            message: err?.message || '',
          })
          handle.handleError(err)
        }
      } finally {
        run.clearInFlight(pageKey, loader.signature)
      }
    }

    if (delay > 0) {
      const timer = setTimeout(execute, delay)
      return () => clearTimeout(timer)
    }
    execute()
    return undefined
  }, [])

  const activePage = loaders?.activePage
  const commonPageLoaders = loaders?.commonPageLoaders || {}
  const keyPageLoaders = loaders?.keyPageLoaders || {}
  const activeCommonSignature = commonPageLoaders[activePage]?.signature || ''
  useEffect(() => {
    const state = stateRef.current
    if (!state?.loaders?.authToken) return
    const currentPage = state.loaders.activePage
    const commonLoader = state.loaders.commonPageLoaders[currentPage]
    if (!commonLoader) return
    const reason = state.loaders.loadReasonRef.current
    state.loaders.loadReasonRef.current = 'default'
    return runRegisteredLoader(currentPage, commonLoader, { delay: commonLoader.delay, reason })
  }, [activeCommonSignature, runRegisteredLoader])

  const activeKeySignature = keyPageLoaders[activePage]?.signature || ''
  const activeKeyCanRun = !!keyPageLoaders[activePage]?.canRun?.()
  useEffect(() => {
    const state = stateRef.current
    if (!state?.loaders?.authToken) return
    const currentPage = state.loaders.activePage
    const keyLoader = state.loaders.keyPageLoaders[currentPage]
    if (!keyLoader || !keyLoader.canRun()) return
    const reason = state.loaders.loadReasonRef.current
    state.loaders.loadReasonRef.current = 'default'
    return runRegisteredLoader(currentPage, keyLoader, { reason })
  }, [activeKeySignature, activeKeyCanRun, runRegisteredLoader])

  const refreshPage = useCallback(async (pageKey, reason = 'panel_action') => {
    const state = stateRef.current
    if (!state?.loaders?.authToken) return false
    const normalized = String(pageKey || state.loaders.activePage || '').trim()
    if (!normalized) return false

    const knownReasons = state.loaders.refreshReasons
    const isKnownReason = knownReasons.has(reason)
    const normalizedReason = isKnownReason ? reason : 'panel_action'
    if (!isKnownReason) state.handlers.markRefreshSourceAnomaly?.(reason)

    try {
      const commonLoader = state.loaders.commonPageLoaders[normalized]
      if (commonLoader) {
        state.loaders.loadReasonRef.current = normalizedReason
        runRegisteredLoader(normalized, commonLoader, { force: true, delay: 0, reason: normalizedReason })
        return true
      }

      const keyLoader = state.loaders.keyPageLoaders[normalized]
      if (keyLoader && keyLoader.canRun()) {
        state.loaders.loadReasonRef.current = normalizedReason
        runRegisteredLoader(normalized, keyLoader, { force: true, delay: 0, reason: normalizedReason })
        return true
      }

      state.metrics.markLoaderFallbackUsed()
      state.handlers.markRefreshSourceAnomaly?.(normalizedReason)
    } catch (err) {
      state.handlers.handleError(err)
      return false
    }
    return false
  }, [runRegisteredLoader])

  return {
    runRegisteredLoader,
    refreshPage,
  }
}
