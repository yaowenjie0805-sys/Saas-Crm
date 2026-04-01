import { useMemo } from 'react'
import { useRuntimeSectionFields } from './useRuntimeSectionFields'

export function useAuthRuntimeState() {
  const defaultTenant = String(import.meta.env.VITE_DEFAULT_TENANT || '').trim()
  const defaults = useMemo(() => ({
    loading: false,
    error: '',
    loginError: '',
    crudErrors: { lead: '', customer: '', opportunity: '', followUp: '', contact: '', contract: '', payment: '' },
    crudFieldErrors: { lead: {}, customer: {}, opportunity: {}, followUp: {}, contact: {}, contract: {}, payment: {} },
    loginForm: () => ({
      tenantId: localStorage.getItem('crm_last_tenant') || defaultTenant || 'tenant_default',
      username: '',
      password: '',
      mfaCode: '',
    }),
    mfaChallengeId: '',
    ssoConfig: { enabled: false, providerName: '', mode: 'mock' },
    ssoForm: { username: 'sso_user', code: 'SSO-ACCESS', displayName: '' },
    oidcAuthorizing: false,
    sessionBootstrapping: true,
    formErrors: { login: {}, register: {}, sso: {} },
    activePage: 'dashboard',
  }), [])

  return useRuntimeSectionFields('auth', 'ui', defaults)
}
