import { useEffect, useRef } from 'react'
import { api, FILTERS_KEY, LANG_KEY, OIDC_STATE_KEY } from '../../../shared'
import { PAGE_TO_PATH } from './routeConfig'

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
  useEffect(() => localStorage.setItem(LANG_KEY, lang), [lang])
  useEffect(() => () => abortAll(), [abortAll])

  useEffect(() => {
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
        const restored = await api('/v1/auth/session', {}, null, lang)
        if (!disposed) saveAuth(restored)
      } catch {
        try {
          const restored = await api('/auth/session', {}, null, lang)
          if (!disposed) saveAuth(restored)
        } catch {
          if (!disposed) saveAuth(null)
        }
      } finally {
        if (!disposed) setSessionBootstrapping(false)
      }
    }
    run()
    return () => {
      disposed = true
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
    const expected = localStorage.getItem(OIDC_STATE_KEY)
    if (expected && state && expected !== state) {
      setLoginError(t('invalidOidcState'))
      return
    }
    localStorage.removeItem(OIDC_STATE_KEY)
    ;(async () => {
      try {
        setOidcAuthorizing(true)
        const tenantId = loginFormTenantId.trim() || localStorage.getItem('crm_last_tenant') || 'tenant_default'
        const d = await api(
          '/auth/sso/login',
          {
            method: 'POST',
            headers: { 'X-Tenant-Id': tenantId },
            body: JSON.stringify({ code }),
          },
          null,
          lang,
        )
        saveAuth(d)
        localStorage.setItem('crm_last_tenant', tenantId)
        const url = new URL(window.location.href)
        url.searchParams.delete('code')
        url.searchParams.delete('state')
        window.history.replaceState({}, document.title, url.toString())
      } catch (err) {
        handleLoginError(err)
      } finally {
        setOidcAuthorizing(false)
      }
    })()
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
    localStorage.setItem(
      FILTERS_KEY,
      JSON.stringify({
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
      }),
    )
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
    localStorage.setItem('crm_page_size_customers', String(customerSize))
    localStorage.setItem('crm_page_size_leads', String(leadSize))
    localStorage.setItem('crm_page_size_opportunities', String(opportunitySize))
    localStorage.setItem('crm_page_size_contacts', String(contactSize))
    localStorage.setItem('crm_page_size_contracts', String(contractSize))
    localStorage.setItem('crm_page_size_payments', String(paymentSize))
    localStorage.setItem('crm_page_size_notification_jobs', String(notificationSize))
    localStorage.setItem('crm_page_size_lead_import_export_jobs', String(leadImportExportSize))
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
