import { useEffect, useRef } from 'react'
import { api, FILTERS_KEY, OIDC_STATE_KEY } from '../../../shared'
import { PAGE_TO_PATH } from './routeConfig'

const REQUEST_CANCEL_ERRORS = new Set(['AbortError', 'CanceledError'])
const OIDC_EXCHANGE_CODE_CACHE_TTL_MS = 10 * 60 * 1000
const OIDC_EXCHANGE_CODE_CACHE_MAX_SIZE = 64
const OIDC_EXCHANGE_CODE_CACHE = new Map()
const PERSISTENCE_DEBOUNCE_MS = 120

const isRequestCanceled = (error) => REQUEST_CANCEL_ERRORS.has(error?.name)
const safeSetLocalStorage = (key, value) => {
  try {
    localStorage.setItem(key, value)
  } catch {
    // ignore storage write failures
  }
}
const pruneOidcExchangeCodeCache = (now = Date.now()) => {
  for (const [code, cachedAt] of OIDC_EXCHANGE_CODE_CACHE) {
    if (now - cachedAt <= OIDC_EXCHANGE_CODE_CACHE_TTL_MS) continue
    OIDC_EXCHANGE_CODE_CACHE.delete(code)
  }
  while (OIDC_EXCHANGE_CODE_CACHE.size > OIDC_EXCHANGE_CODE_CACHE_MAX_SIZE) {
    const oldestEntry = OIDC_EXCHANGE_CODE_CACHE.keys().next()
    if (oldestEntry.done) break
    OIDC_EXCHANGE_CODE_CACHE.delete(oldestEntry.value)
  }
}
const hasFreshOidcExchangeCode = (code, now = Date.now()) => {
  pruneOidcExchangeCodeCache(now)
  const cachedAt = OIDC_EXCHANGE_CODE_CACHE.get(code)
  if (cachedAt === undefined) return false
  if (now - cachedAt > OIDC_EXCHANGE_CODE_CACHE_TTL_MS) {
    OIDC_EXCHANGE_CODE_CACHE.delete(code)
    return false
  }
  return true
}
const rememberOidcExchangeCode = (code, now = Date.now()) => {
  pruneOidcExchangeCodeCache(now)
  OIDC_EXCHANGE_CODE_CACHE.set(code, now)
  pruneOidcExchangeCodeCache(now)
}

export function useRuntimePersistenceEffects({
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
  leadQ,
  leadStatus,
  customerQ,
  customerStatus,
  oppStage,
  followCustomerId,
  followQ,
  contactQ,
  contractQ,
  contractStatus,
  paymentStatus,
  auditUser,
  auditRole,
  auditAction,
  auditFrom,
  auditTo,
  reportOwner,
  reportDepartment,
  reportTimezone,
  reportCurrency,
  customerSize,
  leadSize,
  opportunitySize,
  contactSize,
  contractSize,
  paymentSize,
  notificationSize,
  leadImportExportSize,
  }) {
  const ssoConfigLoadKeyRef = useRef('')
  const oidcExchangeInFlightRef = useRef(false)
  const oidcExchangeRequestIdRef = useRef(0)
  const sessionRestoreAbortRef = useRef(null)
  const oidcExchangeAbortRef = useRef(null)
  const persistedFiltersValueRef = useRef(localStorage.getItem(FILTERS_KEY) || '')
  const pendingFiltersValueRef = useRef(persistedFiltersValueRef.current)
  const filtersWriteTimerRef = useRef(null)
  const persistedPageSizeValuesRef = useRef({
    customers: localStorage.getItem('crm_page_size_customers') || '',
    leads: localStorage.getItem('crm_page_size_leads') || '',
    opportunities: localStorage.getItem('crm_page_size_opportunities') || '',
    contacts: localStorage.getItem('crm_page_size_contacts') || '',
    contracts: localStorage.getItem('crm_page_size_contracts') || '',
    payments: localStorage.getItem('crm_page_size_payments') || '',
    notificationJobs: localStorage.getItem('crm_page_size_notification_jobs') || '',
    leadImportExportJobs: localStorage.getItem('crm_page_size_lead_import_export_jobs') || '',
  })
  useEffect(() => () => {
    abortAll()
    sessionRestoreAbortRef.current?.abort()
    oidcExchangeAbortRef.current?.abort()
  }, [abortAll])
  useEffect(() => () => {
    if (filtersWriteTimerRef.current) {
      clearTimeout(filtersWriteTimerRef.current)
      filtersWriteTimerRef.current = null
    }
    if (pendingFiltersValueRef.current !== persistedFiltersValueRef.current) {
      safeSetLocalStorage(FILTERS_KEY, pendingFiltersValueRef.current)
      persistedFiltersValueRef.current = pendingFiltersValueRef.current
    }
  }, [])

  useEffect(() => {
    sessionRestoreAbortRef.current?.abort()
    const controller = new AbortController()
    sessionRestoreAbortRef.current = controller
    let disposed = false
    const run = async () => {
      if (auth?.token) {
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
  }, [auth?.token, locationPathname, lang, saveAuth, setSessionBootstrapping])

  useEffect(() => {
    const onAuthRoute = locationPathname === '/login' || locationPathname === '/activate'
    if (sessionBootstrapping) return
    if (!auth?.token && !onAuthRoute) {
      navigate('/login', { replace: true })
      return
    }
    if (auth?.token && locationPathname === '/') {
      navigate(PAGE_TO_PATH.dashboard, { replace: true })
    }
  }, [auth?.token, locationPathname, navigate, sessionBootstrapping])

  useEffect(() => {
    if (auth?.token) {
      ssoConfigLoadKeyRef.current = ''
      return
    }
    const loadKey = `${lang}:anonymous`
    if (ssoConfigLoadKeyRef.current === loadKey) return
    ssoConfigLoadKeyRef.current = loadKey
    loadSsoConfig()
  }, [lang, auth?.token, loadSsoConfig])

  useEffect(() => {
    if (auth?.token) return
    if (!(ssoConfig?.enabled && ssoConfig?.mode === 'oidc')) return
    const params = new URLSearchParams(window.location.search)
    const code = params.get('code')
    const state = params.get('state')
    if (!code) return
    if (hasFreshOidcExchangeCode(code)) return
    const expected = localStorage.getItem(OIDC_STATE_KEY)
    if (!expected || !state || expected !== state) {
      setLoginError(t('invalidOidcState'))
      return
    }
    oidcExchangeAbortRef.current?.abort()
    rememberOidcExchangeCode(code)
    const requestId = ++oidcExchangeRequestIdRef.current
    const controller = new AbortController()
    oidcExchangeAbortRef.current = controller
    oidcExchangeInFlightRef.current = true
    ;(async () => {
      let shouldClearOidcState = false
      try {
        setOidcAuthorizing(true)
        const tenantId = loginFormTenantId.trim() || localStorage.getItem('crm_last_tenant') || 'tenant_default'
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
        handleLoginError(err)
      } finally {
        if (shouldClearOidcState) localStorage.removeItem(OIDC_STATE_KEY)
        if (oidcExchangeRequestIdRef.current === requestId) {
          oidcExchangeInFlightRef.current = false
          if (oidcExchangeAbortRef.current === controller) oidcExchangeAbortRef.current = null
          setOidcAuthorizing(false)
        }
      }
    })()
    return () => {
      controller.abort()
      if (oidcExchangeAbortRef.current === controller) oidcExchangeAbortRef.current = null
      oidcExchangeInFlightRef.current = false
    }
  }, [
    auth?.token,
    ssoConfig?.enabled,
    ssoConfig?.mode,
    lang,
    setLoginError,
    t,
    setOidcAuthorizing,
    loginFormTenantId,
    saveAuth,
    handleLoginError,
  ])

  useEffect(() => {
    const nextFiltersValue = JSON.stringify({
      leadQ,
      leadStatus,
      customerQ,
      customerStatus,
      oppStage,
      followCustomerId,
      followQ,
      contactQ,
      contractQ,
      contractStatus,
      paymentStatus,
      auditUser,
      auditRole,
      auditAction,
      auditFrom,
      auditTo,
      reportOwner,
      reportDepartment,
      reportTimezone,
      reportCurrency,
    })
    if (nextFiltersValue === persistedFiltersValueRef.current) return
    pendingFiltersValueRef.current = nextFiltersValue
    if (filtersWriteTimerRef.current) clearTimeout(filtersWriteTimerRef.current)
    filtersWriteTimerRef.current = setTimeout(() => {
      filtersWriteTimerRef.current = null
      if (pendingFiltersValueRef.current === persistedFiltersValueRef.current) return
      safeSetLocalStorage(FILTERS_KEY, pendingFiltersValueRef.current)
      persistedFiltersValueRef.current = pendingFiltersValueRef.current
    }, PERSISTENCE_DEBOUNCE_MS)
    return () => {
      if (filtersWriteTimerRef.current) {
        clearTimeout(filtersWriteTimerRef.current)
        filtersWriteTimerRef.current = null
      }
    }
  }, [
    leadQ,
    leadStatus,
    customerQ,
    customerStatus,
    oppStage,
    followCustomerId,
    followQ,
    contactQ,
    contractQ,
    contractStatus,
    paymentStatus,
    auditUser,
    auditRole,
    auditAction,
    auditFrom,
    auditTo,
    reportOwner,
    reportDepartment,
    reportTimezone,
    reportCurrency,
  ])

  useEffect(() => {
    const nextPageSizeValues = {
      customers: String(customerSize),
      leads: String(leadSize),
      opportunities: String(opportunitySize),
      contacts: String(contactSize),
      contracts: String(contractSize),
      payments: String(paymentSize),
      notificationJobs: String(notificationSize),
      leadImportExportJobs: String(leadImportExportSize),
    }
    const storageEntries = [
      ['customers', 'crm_page_size_customers'],
      ['leads', 'crm_page_size_leads'],
      ['opportunities', 'crm_page_size_opportunities'],
      ['contacts', 'crm_page_size_contacts'],
      ['contracts', 'crm_page_size_contracts'],
      ['payments', 'crm_page_size_payments'],
      ['notificationJobs', 'crm_page_size_notification_jobs'],
      ['leadImportExportJobs', 'crm_page_size_lead_import_export_jobs'],
    ]
    for (const [valueKey, storageKey] of storageEntries) {
      const nextValue = nextPageSizeValues[valueKey]
      if (persistedPageSizeValuesRef.current[valueKey] === nextValue) continue
      safeSetLocalStorage(storageKey, nextValue)
      persistedPageSizeValuesRef.current[valueKey] = nextValue
    }
  }, [
    leadSize,
    customerSize,
    opportunitySize,
    contactSize,
    contractSize,
    paymentSize,
    notificationSize,
    leadImportExportSize,
  ])
}
