import { useEffect } from 'react'
import { api, OIDC_STATE_KEY } from '../../../shared'
import {
  isRequestCanceled,
  isValidOidcState,
  resolveOidcTenantId,
  sharedOidcExchangeCodeCache,
} from './useRuntimeAuthPersistenceUtils'

export function useRuntimeOidcExchangeEffect({
  authToken,
  ssoEnabled,
  ssoMode,
  lang,
  setLoginError,
  t,
  setOidcAuthorizing,
  loginFormTenantId,
  saveAuth,
  handleLoginError,
  oidcExchangeRequestIdRef,
  oidcExchangeAbortRef,
}) {
  useEffect(() => {
    if (authToken) return
    if (!(ssoEnabled && ssoMode === 'oidc')) return
    const params = new URLSearchParams(window.location.search)
    const code = params.get('code')
    const state = params.get('state')
    if (!code) return
    if (sharedOidcExchangeCodeCache.hasFresh(code)) return
    const expected = localStorage.getItem(OIDC_STATE_KEY)
    if (!isValidOidcState(expected, state)) {
      setLoginError(t('invalidOidcState'))
      return
    }
    oidcExchangeAbortRef.current?.abort()
    sharedOidcExchangeCodeCache.remember(code)
    const requestId = ++oidcExchangeRequestIdRef.current
    const controller = new AbortController()
    oidcExchangeAbortRef.current = controller
    ;(async () => {
      let shouldClearOidcState = false
      try {
        setOidcAuthorizing(true)
        const tenantId = resolveOidcTenantId(loginFormTenantId, localStorage.getItem('crm_last_tenant'))
        if (!tenantId) {
          setLoginError(t('fieldRequired'))
          return
        }
        const d = await api(
          '/auth/sso/login',
          {
            method: 'POST',
            headers: { 'X-Tenant-Id': tenantId },
            body: JSON.stringify({ code }),
            signal: controller.signal,
          },
          null,
          lang,
        )
        if (oidcExchangeRequestIdRef.current !== requestId || controller.signal.aborted) return
        saveAuth(d)
        localStorage.setItem('crm_last_tenant', tenantId)
        shouldClearOidcState = true
        const url = new URL(window.location.href)
        url.searchParams.delete('code')
        url.searchParams.delete('state')
        window.history.replaceState({}, document.title, url.toString())
      } catch (err) {
        if (controller.signal.aborted || isRequestCanceled(err) || oidcExchangeRequestIdRef.current !== requestId) return
        sharedOidcExchangeCodeCache.forget(code)
        handleLoginError(err)
      } finally {
        if (shouldClearOidcState) localStorage.removeItem(OIDC_STATE_KEY)
        if (oidcExchangeRequestIdRef.current === requestId) {
          if (oidcExchangeAbortRef.current === controller) oidcExchangeAbortRef.current = null
          setOidcAuthorizing(false)
        }
      }
    })()
    return () => {
      controller.abort()
      if (oidcExchangeAbortRef.current === controller) oidcExchangeAbortRef.current = null
    }
  }, [
    authToken,
    ssoEnabled,
    ssoMode,
    lang,
    setLoginError,
    t,
    setOidcAuthorizing,
    loginFormTenantId,
    saveAuth,
    handleLoginError,
    oidcExchangeRequestIdRef,
    oidcExchangeAbortRef,
  ])
}
