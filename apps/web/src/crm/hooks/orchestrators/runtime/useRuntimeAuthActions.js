import { OIDC_STATE_KEY, api } from '../../../shared'

export function useRuntimeAuthActions({
  lang,
  t,
  loginForm,
  registerForm,
  ssoForm,
  ssoConfig,
  mfaChallengeId,
  validateLogin,
  validateRegister,
  validateSso,
  setLoginError,
  setFormErrors,
  saveAuth,
  setMfaChallengeId,
  setError,
  handleLoginError,
  setOidcAuthorizing,
  logoutGuardRef,
}) {
  const submitLogin = async (e) => {
    e.preventDefault()
    logoutGuardRef.current = false
    setLoginError('')
    const nextErrors = validateLogin()
    setFormErrors((p) => ({ ...p, login: nextErrors }))
    if (Object.keys(nextErrors).length > 0) return
    try {
      if (mfaChallengeId && loginForm.mfaCode?.trim()) {
        const mfaAuth = await api(
          '/v1/auth/mfa/verify',
          {
            method: 'POST',
            body: JSON.stringify({ challengeId: mfaChallengeId, code: loginForm.mfaCode }),
            headers: { 'X-Tenant-Id': loginForm.tenantId.trim() },
          },
          null,
          lang,
        )
        saveAuth(mfaAuth)
        localStorage.setItem('crm_last_tenant', loginForm.tenantId.trim())
        setMfaChallengeId('')
        setError('')
        setLoginError('')
        return
      }

      const payload = {
        tenantId: loginForm.tenantId.trim(),
        username: loginForm.username.trim(),
        password: loginForm.password,
        mfaCode: loginForm.mfaCode?.trim() || '',
      }
      const d = await api(
        '/v1/auth/login',
        { method: 'POST', body: JSON.stringify(payload), headers: { 'X-Tenant-Id': payload.tenantId } },
        null,
        lang,
      )
      if (d?.mfaRequired) {
        setMfaChallengeId(d.challengeId || '')
        setLoginError(t('mfaPending'))
        return
      }
      saveAuth(d)
      localStorage.setItem('crm_last_tenant', payload.tenantId)
      setMfaChallengeId('')
      setLoginError('')
    } catch (err) {
      handleLoginError(err)
    }
  }

  const submitSsoLogin = async (e) => {
    e.preventDefault()
    logoutGuardRef.current = false
    setLoginError('')
    const tenantId = loginForm.tenantId?.trim() || ''
    const nextErrors = validateSso()
    if (!tenantId) {
      nextErrors.tenantId = t('fieldRequired')
    }
    setFormErrors((p) => ({ ...p, sso: nextErrors }))
    if (Object.keys(nextErrors).length > 0) return
    try {
      saveAuth(
        await api(
          '/auth/sso/login',
          { method: 'POST', body: JSON.stringify(ssoForm), headers: { 'X-Tenant-Id': tenantId } },
          null,
          lang,
        ),
      )
      setLoginError('')
    } catch (err) {
      handleLoginError(err)
    }
  }

  const submitRegister = async (e) => {
    e.preventDefault()
    logoutGuardRef.current = false
    setLoginError('')
    const nextErrors = validateRegister()
    setFormErrors((p) => ({ ...p, register: nextErrors }))
    if (Object.keys(nextErrors).length > 0) return

    const tenantId = loginForm.tenantId?.trim() || ''
    const payload = {
      username: registerForm.username?.trim() || '',
      password: registerForm.password || '',
      confirmPassword: registerForm.confirmPassword || '',
      displayName: registerForm.displayName?.trim() || '',
    }

    try {
      await api(
        '/auth/register',
        {
          method: 'POST',
          body: JSON.stringify(payload),
          headers: { 'X-Tenant-Id': tenantId },
        },
        null,
        lang,
      )
      setFormErrors((p) => ({ ...p, register: {} }))
    } catch (err) {
      const code = String(err?.code || '').trim().toUpperCase()
      if (code === 'TENANT_HEADER_REQUIRED') {
        setFormErrors((p) => ({
          ...p,
          register: { ...p.register, tenantId: t('fieldRequired') },
        }))
        return
      }
      if (code === 'USERNAME_EXISTS') {
        setFormErrors((p) => ({
          ...p,
          register: { ...p.register, username: t('usernameExists') },
        }))
        return
      }
      handleLoginError(err)
    }
  }

  const startOidcLogin = () => {
    logoutGuardRef.current = false
    if (!ssoConfig?.authorizeEndpoint || !ssoConfig?.clientId) {
      setLoginError(t('oidcConfigMissing'))
      return
    }
    const state = Math.random().toString(36).slice(2) + Date.now().toString(36)
    localStorage.setItem(OIDC_STATE_KEY, state)
    const redirect = ssoConfig.redirectUri || window.location.origin
    const scope = ssoConfig.scope || 'openid profile email'
    const q = new URLSearchParams({
      response_type: 'code',
      client_id: ssoConfig.clientId,
      redirect_uri: redirect,
      scope,
      state,
    })
    setOidcAuthorizing(true)
    window.location.assign(`${ssoConfig.authorizeEndpoint}?${q}`)
  }

  return {
    submitLogin,
    submitRegister,
    submitSsoLogin,
    startOidcLogin,
  }
}
