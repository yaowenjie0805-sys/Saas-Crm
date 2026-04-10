import React, { act } from 'react'
import { createRoot } from 'react-dom/client'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useRuntimeSessionRestoreEffect } from '../hooks/orchestrators/runtime/useRuntimeSessionRestoreEffect'

const apiMock = vi.hoisted(() => vi.fn())

vi.mock('../shared', () => ({
  api: (...args) => apiMock(...args),
}))

const mountedRoots = []
globalThis.IS_REACT_ACT_ENVIRONMENT = true

function Probe(props) {
  useRuntimeSessionRestoreEffect(props)
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
})

describe('useRuntimeSessionRestoreEffect', () => {
  it('does not fallback to /auth/session on 500 from /v1/auth/session', async () => {
    apiMock
      .mockRejectedValueOnce(Object.assign(new Error('server down'), { status: 500 }))
      .mockResolvedValueOnce({ token: 'legacy_should_not_be_used' })
    const saveAuth = vi.fn()
    const setSessionBootstrapping = vi.fn()

    await renderProbe({
      authToken: null,
      locationPathname: '/dashboard',
      lang: 'en',
      saveAuth,
      setSessionBootstrapping,
      sessionRestoreAbortRef: { current: null },
    })
    await flush()

    expect(apiMock).toHaveBeenCalledTimes(1)
    expect(saveAuth).toHaveBeenCalledWith(null)
    expect(setSessionBootstrapping).toHaveBeenLastCalledWith(false)
  })

  it('falls back to /auth/session when /v1/auth/session returns 404', async () => {
    apiMock
      .mockRejectedValueOnce(Object.assign(new Error('not found'), { status: 404 }))
      .mockResolvedValueOnce({ token: 'legacy_token' })
    const saveAuth = vi.fn()

    await renderProbe({
      authToken: null,
      locationPathname: '/dashboard',
      lang: 'en',
      saveAuth,
      setSessionBootstrapping: vi.fn(),
      sessionRestoreAbortRef: { current: null },
    })
    await flush()

    expect(apiMock).toHaveBeenCalledTimes(2)
    expect(apiMock.mock.calls[0][0]).toBe('/v1/auth/session')
    expect(apiMock.mock.calls[1][0]).toBe('/auth/session')
    expect(saveAuth).toHaveBeenCalledWith({ token: 'legacy_token' })
  })
})
