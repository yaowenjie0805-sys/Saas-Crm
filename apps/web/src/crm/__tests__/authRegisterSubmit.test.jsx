import React, { act, useEffect } from 'react'
import { createRoot } from 'react-dom/client'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useRuntimeAuthActions, useRuntimeFormValidators } from '../hooks/orchestrators/runtime'

const apiMock = vi.hoisted(() => vi.fn())
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

vi.mock('../shared', () => ({
  LANG_KEY: 'crm_lang_test',
  OIDC_STATE_KEY: 'oidc_state_test',
  api: (...args) => apiMock(...args),
}))

afterEach(async () => {
  while (mountedRoots.length) {
    const mounted = mountedRoots.pop()
    await act(async () => {
      mounted.root.unmount()
    })
    mounted.container.remove()
  }
  apiMock.mockReset()
})

const t = (key) => {
  const labels = {
    fieldRequired: 'Required',
    passwordNotMatch: 'Passwords do not match',
    usernameExists: 'Username already exists',
  }
  return labels[key] || key
}

async function createAuthActions(overrides = {}) {
  const formErrorsState = { current: { login: {}, register: {}, sso: {} } }
  const actionsRef = { current: null }
  const setFormErrors = (updater) => {
    formErrorsState.current = typeof updater === 'function'
      ? updater(formErrorsState.current)
      : updater
  }

  function Probe() {
    const actions = useRuntimeAuthActions({
      lang: 'en',
      t,
      loginForm: { tenantId: 'tenant_acme', username: '', password: '', mfaCode: '' },
      registerForm: {
        username: 'alice',
        password: 'Secret#123',
        confirmPassword: 'Secret#123',
        displayName: 'Alice',
      },
      ssoForm: { username: 'sso-user', code: 'SSO-ACCESS', displayName: '' },
      ssoConfig: {},
      mfaChallengeId: '',
      validateLogin: () => ({}),
      validateRegister: () => ({}),
      validateSso: () => ({}),
      setLoginError: vi.fn(),
      setFormErrors,
      saveAuth: vi.fn(),
      setMfaChallengeId: vi.fn(),
      setError: vi.fn(),
      handleLoginError: vi.fn(),
      setOidcAuthorizing: vi.fn(),
      logoutGuardRef: { current: false },
      ...overrides,
    })

    useEffect(() => {
      actionsRef.current = actions
    }, [actions])

    return null
  }

  await render(<Probe />)
  return { actions: actionsRef.current, formErrorsState }
}

describe('register submit flow', () => {
  it('submitRegister posts to /auth/register with tenant header from loginForm', async () => {
    apiMock.mockResolvedValueOnce({ ok: true })
    const { actions, formErrorsState } = await createAuthActions()

    await actions.submitRegister({ preventDefault: vi.fn() })

    expect(apiMock).toHaveBeenCalledTimes(1)
    const [path, options] = apiMock.mock.calls[0]
    expect(path).toBe('/auth/register')
    expect(options.method).toBe('POST')
    expect(options.headers).toEqual({ 'X-Tenant-Id': 'tenant_acme' })
    expect(JSON.parse(options.body)).toEqual({
      username: 'alice',
      password: 'Secret#123',
      confirmPassword: 'Secret#123',
      displayName: 'Alice',
    })
    expect(formErrorsState.current.register).toEqual({})
  })

  it('submitRegister maps TENANT_HEADER_REQUIRED backend code to register.tenantId error', async () => {
    const err = new Error('tenant missing')
    err.code = 'TENANT_HEADER_REQUIRED'
    apiMock.mockRejectedValueOnce(err)

    const { actions, formErrorsState } = await createAuthActions()
    await actions.submitRegister({ preventDefault: vi.fn() })

    expect(formErrorsState.current.register.tenantId).toBe('Required')
  })

  it('submitRegister maps username_exists backend code to register.username error', async () => {
    const err = new Error('username exists')
    err.code = 'username_exists'
    apiMock.mockRejectedValueOnce(err)

    const { actions, formErrorsState } = await createAuthActions()
    await actions.submitRegister({ preventDefault: vi.fn() })

    expect(formErrorsState.current.register.username).toBe('Username already exists')
  })

  it('submitRegister blocks request when tenant validation fails', async () => {
    const { actions, formErrorsState } = await createAuthActions({
      loginForm: { tenantId: '   ', username: '', password: '', mfaCode: '' },
      validateRegister: () => ({ tenantId: 'Required' }),
    })

    await actions.submitRegister({ preventDefault: vi.fn() })

    expect(formErrorsState.current.register.tenantId).toBe('Required')
    expect(apiMock).not.toHaveBeenCalled()
  })
})

describe('register form validation', () => {
  it('validateRegister requires tenantId, username, password, and confirmPassword', () => {
    const { validateRegister } = useRuntimeFormValidators({
      t,
      auth: null,
      loginForm: { tenantId: '   ', username: '', password: '', mfaCode: '' },
      registerForm: { username: '   ', password: '', confirmPassword: '', displayName: '' },
      ssoForm: { username: '', code: '' },
      contactForm: {},
      customerForm: {},
      contractForm: {},
      paymentForm: {},
      auditFrom: '',
      auditTo: '',
    })

    expect(validateRegister()).toEqual({
      tenantId: 'Required',
      username: 'Required',
      password: 'Required',
      confirmPassword: 'Required',
    })
  })
})
