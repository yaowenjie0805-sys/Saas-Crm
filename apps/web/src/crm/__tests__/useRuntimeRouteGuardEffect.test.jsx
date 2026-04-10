import React, { act } from 'react'
import { createRoot } from 'react-dom/client'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useRuntimeRouteGuardEffect } from '../hooks/orchestrators/runtime/useRuntimeRouteGuardEffect'

globalThis.IS_REACT_ACT_ENVIRONMENT = true

const mountedRoots = []

function Probe(props) {
  useRuntimeRouteGuardEffect(props)
  return null
}

async function renderProbe(props) {
  const container = document.createElement('div')
  document.body.appendChild(container)
  const root = createRoot(container)
  mountedRoots.push({ root, container })
  await act(async () => {
    root.render(<Probe {...props} />)
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
})

describe('useRuntimeRouteGuardEffect', () => {
  it('redirects authenticated users away from login route', async () => {
    const navigate = vi.fn()
    await renderProbe({
      authToken: 'token',
      locationPathname: '/login',
      navigate,
      sessionBootstrapping: false,
    })
    expect(navigate).toHaveBeenCalledWith('/dashboard', { replace: true })
  })

  it('keeps unauthenticated users on auth route', async () => {
    const navigate = vi.fn()
    await renderProbe({
      authToken: '',
      locationPathname: '/login',
      navigate,
      sessionBootstrapping: false,
    })
    expect(navigate).not.toHaveBeenCalled()
  })
})

