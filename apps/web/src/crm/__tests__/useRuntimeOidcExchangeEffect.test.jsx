import React, { act } from 'react'
import { createRoot } from 'react-dom/client'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useRuntimeOidcExchangeEffect } from '../hooks/orchestrators/runtime/useRuntimeOidcExchangeEffect'
import { sharedOidcExchangeCodeCache } from '../hooks/orchestrators/runtime/useRuntimeAuthPersistenceUtils'

const apiMock = vi.hoisted(() => vi.fn())

vi.mock('../shared', () => ({
  OIDC_STATE_KEY: 'oidc_state_test',
  api: (...args) => apiMock(...args),
}))

const mountedRoots = []
globalThis.IS_REACT_ACT_ENVIRONMENT = true

function Probe(props) {
  useRuntimeOidcExchangeEffect(props)
  return null
}

const renderProbe = async (props) => {
  const container = document.createElement('div')
  document.body.appendChild(container)
  const root = createRoot(container)
  mountedRoots.push({ root, container })
  await act(async () => {
    root.render(<Probe {...props} />)
  })
}

const flush = async () => {
  await act(async () => {
    await Promise.resolve()
    await Promise.resolve()
  })
}

afterEach(async () => {
  while (mountedRoots.length) {
    const mounted = mountedRoots.pop()
    await act(async () => {
      mounted.root.unmount()
    })
    mounted.container.remove()
  }
  apiMock.mockReset()
  sharedOidcExchangeCodeCache.clear()
  localStorage.clear()
  window.history.replaceState({}, '', '/')
})

describe('useRuntimeOidcExchangeEffect', () => {
  it('blocks exchange and reports invalid state when oidc state mismatches', async () => {
    window.history.replaceState({}, '', '/login?code=c1&state=wrong')
    localStorage.setItem('oidc_state_test', 'expected')
    const setLoginError = vi.fn()

    await renderProbe({
      authToken: null,
      ssoEnabled: true,
      ssoMode: 'oidc',
      lang: 'en',
      setLoginError,
      t: (key) => key,
      setOidcAuthorizing: vi.fn(),
      loginFormTenantId: 'tenant_a',
      saveAuth: vi.fn(),
      handleLoginError: vi.fn(),
      oidcExchangeRequestIdRef: { current: 0 },
      oidcExchangeAbortRef: { current: null },
    })
    await flush()

    expect(setLoginError).toHaveBeenCalledWith('invalidOidcState')
    expect(apiMock).not.toHaveBeenCalled()
  })

  it('uses cached tenant fallback and exchanges code successfully', async () => {
    window.history.replaceState({}, '', '/login?code=c2&state=s2')
    localStorage.setItem('oidc_state_test', 's2')
    localStorage.setItem('crm_last_tenant', ' tenant_cached ')
    apiMock.mockResolvedValueOnce({ token: 't' })
    const saveAuth = vi.fn()
    const setOidcAuthorizing = vi.fn()

    await renderProbe({
      authToken: null,
      ssoEnabled: true,
      ssoMode: 'oidc',
      lang: 'en',
      setLoginError: vi.fn(),
      t: (key) => key,
      setOidcAuthorizing,
      loginFormTenantId: '   ',
      saveAuth,
      handleLoginError: vi.fn(),
      oidcExchangeRequestIdRef: { current: 0 },
      oidcExchangeAbortRef: { current: null },
    })
    await flush()

    expect(apiMock).toHaveBeenCalled()
    const [, request] = apiMock.mock.calls[0]
    expect(request.headers['X-Tenant-Id']).toBe('tenant_cached')
    expect(saveAuth).toHaveBeenCalledWith({ token: 't' })
    expect(localStorage.getItem('oidc_state_test')).toBeNull()
    expect(setOidcAuthorizing).toHaveBeenCalledWith(true)
    expect(setOidcAuthorizing).toHaveBeenLastCalledWith(false)
  })

  it('skips duplicate exchange for the same oidc code', async () => {
    window.history.replaceState({}, '', '/login?code=dup1&state=s3')
    localStorage.setItem('oidc_state_test', 's3')
    apiMock.mockResolvedValue({ token: 't' })

    const createProps = () => ({
      authToken: null,
      ssoEnabled: true,
      ssoMode: 'oidc',
      lang: 'en',
      setLoginError: vi.fn(),
      t: (key) => key,
      setOidcAuthorizing: vi.fn(),
      loginFormTenantId: 'tenant_dup',
      saveAuth: vi.fn(),
      handleLoginError: vi.fn(),
      oidcExchangeRequestIdRef: { current: 0 },
      oidcExchangeAbortRef: { current: null },
    })

    await renderProbe(createProps())
    await flush()
    await renderProbe(createProps())
    await flush()

    expect(apiMock).toHaveBeenCalledTimes(1)
  })

  it('allows retrying the same oidc code after exchange failure', async () => {
    window.history.replaceState({}, '', '/login?code=retry1&state=s4')
    localStorage.setItem('oidc_state_test', 's4')
    apiMock
      .mockRejectedValueOnce(new Error('network-fail'))
      .mockResolvedValueOnce({ token: 't_retry' })

    const createProps = () => ({
      authToken: null,
      ssoEnabled: true,
      ssoMode: 'oidc',
      lang: 'en',
      setLoginError: vi.fn(),
      t: (key) => key,
      setOidcAuthorizing: vi.fn(),
      loginFormTenantId: 'tenant_retry',
      saveAuth: vi.fn(),
      handleLoginError: vi.fn(),
      oidcExchangeRequestIdRef: { current: 0 },
      oidcExchangeAbortRef: { current: null },
    })

    const firstProps = createProps()
    await renderProbe(firstProps)
    await flush()

    expect(firstProps.handleLoginError).toHaveBeenCalledTimes(1)
    expect(localStorage.getItem('oidc_state_test')).toBe('s4')

    const secondProps = createProps()
    await renderProbe(secondProps)
    await flush()

    expect(apiMock).toHaveBeenCalledTimes(2)
    expect(secondProps.saveAuth).toHaveBeenCalledWith({ token: 't_retry' })
  })
})
