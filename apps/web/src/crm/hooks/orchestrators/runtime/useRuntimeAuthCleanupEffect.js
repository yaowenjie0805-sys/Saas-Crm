import { useEffect } from 'react'

export function useRuntimeAuthCleanupEffect({
  abortAll,
  sessionRestoreAbortRef,
  oidcExchangeAbortRef,
}) {
  useEffect(() => () => {
    abortAll()
    sessionRestoreAbortRef.current?.abort()
    oidcExchangeAbortRef.current?.abort()
  }, [abortAll, sessionRestoreAbortRef, oidcExchangeAbortRef])
}
