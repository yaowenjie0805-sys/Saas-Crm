import { useMemo } from 'react'
import { useRuntimeSectionFields } from './useRuntimeSectionFields'

const isDevMode = import.meta.env.MODE === 'development'
const readDefault = (key, fallback = '') => {
  const value = String(import.meta.env[key] || '').trim()
  return value || fallback
}

export function useAuthRuntimeState() {
  const defaultTenant = readDefault('VITE_DEFAULT_TENANT', isDevMode ? 'tenant_default' : '')
  const defaultUsername = readDefault('VITE_DEFAULT_USERNAME', isDevMode ? 'admin' : '')
  const defaultPassword = readDefault('VITE_DEFAULT_PASSWORD', isDevMode ? 'admin123' : '')
  const defaults = useMemo(() => ({
    loading: false,
    error: '',
    loginError: '',
    crudErrors: { lead: '', customer: '', opportunity: '', followUp: '', contact: '', contract: '', payment: '' },
    crudFieldErrors: { lead: {}, customer: {}, opportunity: {}, followUp: {}, contact: {}, contract: {}, payment: {} },
    loginForm: () => ({
      tenantId: localStorage.getItem('crm_last_tenant') || defaultTenant || '',
      username: defaultUsername,
      password: defaultPassword,
      mfaCode: '',
    }),
    mfaChallengeId: '',
    ssoConfig: { enabled: false, providerName: '', mode: 'mock' },
    ssoForm: { username: 'sso_user', code: 'SSO-ACCESS', displayName: '' },
    oidcAuthorizing: false,
    sessionBootstrapping: true,
    formErrors: { login: {}, sso: {} },
    activePage: 'dashboard',
  }), [defaultPassword, defaultTenant, defaultUsername])

  return useRuntimeSectionFields('auth', 'ui', defaults)
}
