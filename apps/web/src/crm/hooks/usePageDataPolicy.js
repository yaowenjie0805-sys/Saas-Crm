import { useCallback, useRef } from 'react'

const DEFAULT_TTL = 10000

export function usePageDataPolicy(ttlMs = DEFAULT_TTL) {
  const abortRef = useRef({})
  const fetchMetaRef = useRef({})
  const inFlightRef = useRef({})

  const beginPageRequest = useCallback((key) => {
    const prev = abortRef.current[key]
    if (prev?.abort) prev.abort()
    const controller = new AbortController()
    abortRef.current[key] = controller
    return controller
  }, [])

  const canSkipFetch = useCallback((key, signature, force = false) => {
    if (force) return false
    const meta = fetchMetaRef.current[key]
    if (!meta) return false
    const fresh = (Date.now() - meta.at) < ttlMs
    return fresh && meta.signature === signature
  }, [ttlMs])

  const isInFlight = useCallback((key, signature) => {
    const meta = inFlightRef.current[key]
    return !!(meta && meta.signature === signature)
  }, [])

  const markInFlight = useCallback((key, signature) => {
    inFlightRef.current[key] = { signature, at: Date.now() }
  }, [])

  const clearInFlight = useCallback((key, signature) => {
    const meta = inFlightRef.current[key]
    if (!meta) return
    if (!signature || meta.signature === signature) delete inFlightRef.current[key]
  }, [])

  const markFetched = useCallback((key, signature) => {
    fetchMetaRef.current[key] = { signature, at: Date.now() }
  }, [])

  const abortAll = useCallback(() => {
    Object.values(abortRef.current || {}).forEach((controller) => {
      if (controller?.abort) controller.abort()
    })
    inFlightRef.current = {}
  }, [])

  return {
    beginPageRequest,
    canSkipFetch,
    isInFlight,
    markInFlight,
    clearInFlight,
    markFetched,
    abortAll,
  }
}
