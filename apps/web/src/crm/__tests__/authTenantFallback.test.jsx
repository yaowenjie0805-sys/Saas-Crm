import React, { act } from 'react'
import { createRoot } from 'react-dom/client'
import { afterEach, describe, expect, it, vi } from 'vitest'
import InvitationAcceptView from '../components/InvitationAcceptView'
import { useAuthRuntimeState } from '../hooks/orchestrators'
import { useRuntimeAuthActions } from '../hooks/orchestrators/runtime'

const useRuntimeSectionFieldsMock = vi.hoisted(() =>
  vi.fn((_domain, _section, defaults) => defaults),
)
const apiMock = vi.hoisted(() => vi.fn())

vi.mock('../hooks/orchestrators/useRuntimeSectionFields', () => ({
  useRuntimeSectionFields: (...args) => useRuntimeSectionFieldsMock(...args),
}))

vi.mock('../shared', () => ({
  LANG_KEY: 'crm_lang_test',
  OIDC_STATE_KEY: 'oidc_state_test',
  api: (...args) => apiMock(...args),
}))

const mountedRoots = []
globalThis.IS_REACT_ACT_ENVIRONMENT = true

const render = async (element) => {
  const container = document.createElement('div')
  document.body.appendChild(container)
  const root = createRoot(container)
  mountedRoots.push({ container, root })

  await act(async () => {
    root.render(element)
  })

  return { container }
}

afterEach(async () => {
  while (mountedRoots.length) {
    const mounted = mountedRoots.pop()
    await act(async () => {
      mounted.root.unmount()
    })
    mounted.container.remove()
  }
  useRuntimeSectionFieldsMock.mockClear()
  apiMock.mockReset()
  localStorage.removeItem('crm_last_tenant')
  window.history.replaceState({}, '', '/')
})

describe('tenant fallback cleanup', () => {
  it('useAuthRuntimeState loginForm defaults tenantId to tenant_default when cache is absent', async () => {
    localStorage.removeItem('crm_last_tenant')
    const capturedStateRef = { current: null }

    function Probe() {
      const state = useAuthRuntimeState()
      React.useEffect(() => {
        capturedStateRef.current = state
      }, [state])
      return null
    }

    await render(<Probe />)

    expect(capturedStateRef.current.loginForm().tenantId).toBe('tenant_default')
  })

  it('useRuntimeAuthActions.submitSsoLogin blocks request and sets tenant field error when tenant is blank', async () => {
    let ssoErrors = {}
    const setFormErrors = (updater) => {
      const nextState = typeof updater === 'function'
        ? updater({ sso: ssoErrors, login: {}, register: {} })
        : updater
      ssoErrors = nextState.sso || {}
    }

    const actions = useRuntimeAuthActions({
      lang: 'en',
      t: (key) => (key === 'fieldRequired' ? 'Required' : key),
      loginForm: { tenantId: '   ' },
      ssoForm: { username: 'sso-user', code: 'SSO-ACCESS', displayName: '' },
      ssoConfig: {},
      mfaChallengeId: '',
      validateLogin: () => ({}),
      validateSso: () => ({}),
      setLoginError: vi.fn(),
      setFormErrors,
      saveAuth: vi.fn(),
      setMfaChallengeId: vi.fn(),
      setError: vi.fn(),
      handleLoginError: vi.fn(),
      setOidcAuthorizing: vi.fn(),
      logoutGuardRef: { current: false },
    })

    await actions.submitSsoLogin({ preventDefault: vi.fn() })

    expect(ssoErrors.tenantId).toBe('Required')
    expect(apiMock).not.toHaveBeenCalled()
  })

  it('InvitationAcceptView should not fallback tenantId to tenant_default', async () => {
    localStorage.removeItem('crm_last_tenant')
    window.history.replaceState({}, '', '/invite?token=abc')

    const { container } = await render(
      <InvitationAcceptView
        lang="en"
        setLang={vi.fn()}
        t={(key) => key}
        onBackToLogin={vi.fn()}
      />,
    )

    const tenantInput = container.querySelector('input[placeholder="tenantId"]')
    expect(tenantInput).not.toBeNull()
    expect(tenantInput.value).toBe('')
  })
})
