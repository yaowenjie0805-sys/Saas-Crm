import { useRef } from 'react'
import { useRuntimeAnonymousSsoConfigEffect } from './useRuntimeAnonymousSsoConfigEffect'
import { useRuntimeAuthCleanupEffect } from './useRuntimeAuthCleanupEffect'
import { useRuntimeOidcExchangeEffect } from './useRuntimeOidcExchangeEffect'
import { useRuntimeRouteGuardEffect } from './useRuntimeRouteGuardEffect'
import { useRuntimeSessionRestoreEffect } from './useRuntimeSessionRestoreEffect'

export function useRuntimeAuthPersistenceEffects({
  lang,
  abortAll,
  auth,
  locationPathname,
  sessionBootstrapping,
  navigate,
  saveAuth,
  setSessionBootstrapping,
  loadSsoConfig,
  ssoConfig,
  setLoginError,
  t,
  setOidcAuthorizing,
  loginFormTenantId,
  handleLoginError,
}) {
  const ssoConfigLoadKeyRef = useRef('')
  const oidcExchangeRequestIdRef = useRef(0)
  const sessionRestoreAbortRef = useRef(null)
  const oidcExchangeAbortRef = useRef(null)

  useRuntimeAuthCleanupEffect({
    abortAll,
    sessionRestoreAbortRef,
    oidcExchangeAbortRef,
  })

  useRuntimeSessionRestoreEffect({
    authToken: auth?.token,
    locationPathname,
    lang,
    saveAuth,
    setSessionBootstrapping,
    sessionRestoreAbortRef,
  })

  useRuntimeRouteGuardEffect({
    authToken: auth?.token,
    locationPathname,
    navigate,
    sessionBootstrapping,
  })

  useRuntimeAnonymousSsoConfigEffect({
    authToken: auth?.token,
    lang,
    loadSsoConfig,
    ssoConfigLoadKeyRef,
  })

  useRuntimeOidcExchangeEffect({
    authToken: auth?.token,
    ssoEnabled: ssoConfig?.enabled,
    ssoMode: ssoConfig?.mode,
    lang,
    setLoginError,
    t,
    setOidcAuthorizing,
    loginFormTenantId,
    saveAuth,
    handleLoginError,
    oidcExchangeRequestIdRef,
    oidcExchangeAbortRef,
  })
}
