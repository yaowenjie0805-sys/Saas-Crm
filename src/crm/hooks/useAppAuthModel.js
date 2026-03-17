import { useCallback } from 'react'
import { LANG_KEY, api } from '../shared'

export function useAppAuthModel({
  auth,
  setAuth,
  setError,
  setLoginError,
  setCrudErrors,
  setCrudFieldErrors,
  setCurrentLoaderKey,
  setCurrentPageSignature,
  setCurrentSignatureHit,
  setRecentWorkbenchJump,
  setDomainLoadSource,
  setLastRefreshReason,
  abortAll,
  loadReasonRef,
  workbenchJumpRef,
  navigate,
  pathname,
  logoutGuardRef,
  suppressLoginErrorUntilRef,
  markLoginErrorLeakBlocked,
  markAuthChannelMisroute,
  t,
  formatErrorMessage,
}) {
  const saveAuth = useCallback((next) => {
    if (!next) {
      localStorage.removeItem('crm_auth')
      setAuth(null)
      return
    }
    const tenantId = String(next.tenantId || '').trim()
    if (tenantId) {
      localStorage.setItem('crm_last_tenant', tenantId)
    }
    const safePersisted = {
      username: next.username || '',
      displayName: next.displayName || '',
      role: next.role || '',
      ownerScope: next.ownerScope || '',
      tenantId,
      department: next.department || '',
      dataScope: next.dataScope || '',
      dateFormat: next.dateFormat || 'yyyy-MM-dd',
      sessionActive: true,
    }
    localStorage.setItem('crm_auth', JSON.stringify(safePersisted))
    setAuth({ ...safePersisted, token: 'COOKIE_SESSION' })
  }, [setAuth])

  const handleLoginError = useCallback((err) => {
    setLoginError(formatErrorMessage(err))
  }, [formatErrorMessage, setLoginError])

  const handleError = useCallback((err) => {
    if (logoutGuardRef.current) return
    const now = Date.now()
    if (!auth?.token && now < suppressLoginErrorUntilRef.current) {
      markLoginErrorLeakBlocked()
      return
    }
    if (!auth?.token) {
      const isAuthRoute = pathname === '/login' || pathname === '/activate'
      if (!isAuthRoute) {
        markAuthChannelMisroute()
        markLoginErrorLeakBlocked()
        return
      }
      setLoginError(formatErrorMessage(err))
      return
    }
    if (err.status === 401 && auth?.token) {
      saveAuth(null)
      setError(t('sessionExpired'))
      return
    }
    setError(formatErrorMessage(err))
  }, [logoutGuardRef, auth?.token, suppressLoginErrorUntilRef, markLoginErrorLeakBlocked, pathname, markAuthChannelMisroute, setLoginError, formatErrorMessage, saveAuth, setError, t])

  const performLogout = useCallback(async () => {
    logoutGuardRef.current = true
    suppressLoginErrorUntilRef.current = Date.now() + 4000
    try {
      const token = auth?.token
      const langValue = localStorage.getItem(LANG_KEY) || 'en'
      if (token) {
        await api('/v1/auth/logout', { method: 'POST' }, token, langValue).catch(async () => {
          await api('/auth/logout', { method: 'POST' }, token, langValue).catch(() => null)
        })
      }
    } finally {
      abortAll()
      loadReasonRef.current = 'default'
      workbenchJumpRef.current = { page: '', signature: '' }
      saveAuth(null)
      setError('')
      setLoginError('')
      setCrudErrors({ lead: '', customer: '', opportunity: '', followUp: '', contact: '', contract: '', payment: '' })
      setCrudFieldErrors({ lead: {}, customer: {}, opportunity: {}, followUp: {}, contact: {}, contract: {}, payment: {} })
      setCurrentLoaderKey('')
      setCurrentPageSignature('')
      setCurrentSignatureHit(false)
      setRecentWorkbenchJump({ targetPage: '', signature: '', reason: 'workbench_jump', hit: false, at: '' })
      setDomainLoadSource({
        customer: { source: '', at: '' },
        commerce: { source: '', at: '' },
        governance: { source: '', at: '' },
        approval: { source: '', at: '' },
        reporting: { source: '', at: '' },
        workbench: { source: '', at: '' },
      })
      setLastRefreshReason('default')
      navigate('/login', { replace: true })
    }
  }, [logoutGuardRef, suppressLoginErrorUntilRef, auth?.token, abortAll, loadReasonRef, workbenchJumpRef, saveAuth, setError, setLoginError, setCrudErrors, setCrudFieldErrors, setCurrentLoaderKey, setCurrentPageSignature, setCurrentSignatureHit, setRecentWorkbenchJump, setDomainLoadSource, setLastRefreshReason, navigate])

  return {
    saveAuth,
    handleLoginError,
    handleError,
    performLogout,
  }
}
