import { useEffect } from 'react'
import { api } from '../../../shared'
import { isRequestCanceled } from './useRuntimeAuthPersistenceUtils'

const LEGACY_SESSION_FALLBACK_STATUSES = new Set([404, 405, 501])

const shouldTryLegacySessionFallback = (error) =>
  LEGACY_SESSION_FALLBACK_STATUSES.has(Number(error?.status))

export function useRuntimeSessionRestoreEffect({
  authToken,
  locationPathname,
  lang,
  saveAuth,
  setSessionBootstrapping,
  sessionRestoreAbortRef,
}) {
  useEffect(() => {
    sessionRestoreAbortRef.current?.abort()
    const controller = new AbortController()
    sessionRestoreAbortRef.current = controller
    let disposed = false
    const run = async () => {
      if (authToken) {
        if (!disposed) setSessionBootstrapping(false)
        return
      }
      if (locationPathname === '/activate') {
        if (!disposed) setSessionBootstrapping(false)
        return
      }
      try {
        const restored = await api('/v1/auth/session', { signal: controller.signal }, null, lang)
        if (!disposed && !controller.signal.aborted) saveAuth(restored)
      } catch (error) {
        if (disposed || controller.signal.aborted || isRequestCanceled(error)) return
        if (!shouldTryLegacySessionFallback(error)) {
          if (!disposed) saveAuth(null)
          return
        }
        try {
          const restored = await api('/auth/session', { signal: controller.signal }, null, lang)
          if (!disposed && !controller.signal.aborted) saveAuth(restored)
        } catch (fallbackError) {
          if (disposed || controller.signal.aborted || isRequestCanceled(fallbackError)) return
          if (!disposed) saveAuth(null)
        }
      } finally {
        if (!disposed && !controller.signal.aborted) setSessionBootstrapping(false)
        if (sessionRestoreAbortRef.current === controller) sessionRestoreAbortRef.current = null
      }
    }
    run()
    return () => {
      disposed = true
      controller.abort()
      if (sessionRestoreAbortRef.current === controller) sessionRestoreAbortRef.current = null
    }
  }, [authToken, locationPathname, lang, saveAuth, setSessionBootstrapping, sessionRestoreAbortRef])
}
