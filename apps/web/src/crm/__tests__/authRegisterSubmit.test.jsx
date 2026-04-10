import React, { act, useEffect } from 'react'
import { createRoot } from 'react-dom/client'
import { afterEach, describe, expect, it, vi } from 'vitest'
import LoginView from '../components/LoginView'
import { useRuntimeAuthActions } from '../hooks/orchestrators/runtime'

const mountedRoots = []
globalThis.IS_REACT_ACT_ENVIRONMENT = true

const t = (key) => {
  const labels = {
    loginTitle: 'Login',
    loginHint: 'Use your account',
    tenantId: 'Tenant ID',
    tenantHint: 'Tenant required',
    username: 'Username',
    password: 'Password',
    mfaCode: 'MFA',
    login: 'Login',
    inviteActivateBtn: 'Activate Account',
    toRegister: 'Create Account',
  }
  return labels[key] || key
}

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
})

describe('login view invitation-only entry', () => {
  it('renders activate entry and no self-register switch', async () => {
    const { container } = await render(
      <LoginView
        lang="en"
        setLang={vi.fn()}
        t={t}
        submitLogin={vi.fn()}
        loginForm={{ tenantId: '', username: '', password: '', mfaCode: '' }}
        setLoginForm={vi.fn()}
        formErrors={{ login: {}, sso: {} }}
        setFormErrors={vi.fn()}
        submitSsoLogin={vi.fn()}
        ssoConfig={{ enabled: false }}
        ssoForm={{ username: '', code: '', displayName: '' }}
        setSsoForm={vi.fn()}
        oidcAuthorizing={false}
        startOidcLogin={vi.fn()}
        openActivate={vi.fn()}
        error=""
      />,
    )

    const activateBtn = container.querySelector('[data-testid="open-activate"]')
    expect(activateBtn?.textContent).toBe('Activate Account')
    expect(container.textContent).not.toContain('Create Account')
    expect(container.querySelector('[data-testid="register-tenant-id"]')).toBeNull()
  })

  it('calls openActivate when clicking invitation entry', async () => {
    const openActivate = vi.fn()
    const { container } = await render(
      <LoginView
        lang="en"
        setLang={vi.fn()}
        t={t}
        submitLogin={vi.fn()}
        loginForm={{ tenantId: '', username: '', password: '', mfaCode: '' }}
        setLoginForm={vi.fn()}
        formErrors={{ login: {}, sso: {} }}
        setFormErrors={vi.fn()}
        submitSsoLogin={vi.fn()}
        ssoConfig={{ enabled: false }}
        ssoForm={{ username: '', code: '', displayName: '' }}
        setSsoForm={vi.fn()}
        oidcAuthorizing={false}
        startOidcLogin={vi.fn()}
        openActivate={openActivate}
        error=""
      />,
    )

    await act(async () => {
      container.querySelector('[data-testid="open-activate"]')?.dispatchEvent(
        new MouseEvent('click', { bubbles: true }),
      )
    })

    expect(openActivate).toHaveBeenCalledTimes(1)
  })
})

describe('runtime auth actions invitation-only', () => {
  it('does not expose submitRegister action', async () => {
    const actionsRef = { current: null }

    function Probe() {
      const actions = useRuntimeAuthActions({
        lang: 'en',
        t,
        loginForm: { tenantId: '', username: '', password: '', mfaCode: '' },
        ssoForm: { username: '', code: '', displayName: '' },
        ssoConfig: {},
        mfaChallengeId: '',
        validateLogin: () => ({}),
        validateSso: () => ({}),
        setLoginError: vi.fn(),
        setFormErrors: vi.fn(),
        saveAuth: vi.fn(),
        setMfaChallengeId: vi.fn(),
        setError: vi.fn(),
        handleLoginError: vi.fn(),
        setOidcAuthorizing: vi.fn(),
        logoutGuardRef: { current: false },
      })

      useEffect(() => {
        actionsRef.current = actions
      }, [actions])

      return null
    }

    await render(<Probe />)
    expect('submitRegister' in actionsRef.current).toBe(false)
  })
})
