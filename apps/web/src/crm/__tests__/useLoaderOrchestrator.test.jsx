import React, { act } from 'react'
import { createRoot } from 'react-dom/client'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useLoaderOrchestrator } from '../hooks/useLoaderOrchestrator'

globalThis.IS_REACT_ACT_ENVIRONMENT = true

const mountedRoots = []

function Probe({ options }) {
  useLoaderOrchestrator(options)
  return null
}

async function renderProbe(options) {
  const container = document.createElement('div')
  document.body.appendChild(container)
  const root = createRoot(container)
  mountedRoots.push({ root, container })
  await act(async () => {
    root.render(<Probe options={options} />)
  })
  return root
}

afterEach(async () => {
  while (mountedRoots.length) {
    const mounted = mountedRoots.pop()
    await act(async () => {
      mounted.root.unmount()
    })
    mounted.container.remove()
  }
  vi.useRealTimers()
})

describe('useLoaderOrchestrator', () => {
  it('marks delayed loaders as in-flight before timer fires', async () => {
    vi.useFakeTimers()
    const markInFlight = vi.fn()
    const clearInFlight = vi.fn()
    const beginPageRequest = vi.fn(() => ({ signal: undefined }))

    await renderProbe({
      loaders: {
        authToken: 'token',
        activePage: 'dashboard',
        commonPageLoaders: {
          dashboard: {
            signature: 'sig-1',
            delay: 50,
            run: vi.fn(async () => {}),
          },
        },
        keyPageLoaders: {},
        loadReasonRef: { current: 'default' },
        refreshReasons: new Set(['default']),
      },
      runtime: {
        beginPageRequest,
        canSkipFetch: () => false,
        isInFlight: () => false,
        markInFlight,
        clearInFlight,
      },
      metrics: {
        setLastRefreshReason: vi.fn(),
        setCurrentLoaderKey: vi.fn(),
        setCurrentPageSignature: vi.fn(),
        setCurrentSignatureHit: vi.fn(),
        markCacheDecision: vi.fn(),
        markDuplicateFetchBlocked: vi.fn(),
        markWorkbenchJumpDecision: vi.fn(),
        markFetched: vi.fn(),
        markFetchLatency: vi.fn(),
        markAbort: vi.fn(),
        markLoaderFallbackUsed: vi.fn(),
      },
      handlers: {
        handleError: vi.fn(),
        onLoaderLifecycle: vi.fn(),
        markRefreshSourceAnomaly: vi.fn(),
      },
    })

    expect(markInFlight).toHaveBeenCalled()
    await act(async () => {
      vi.advanceTimersByTime(60)
    })
    expect(beginPageRequest).toHaveBeenCalled()
    expect(clearInFlight).toHaveBeenCalled()
  })

  it('rechecks skip condition before delayed loader execution', async () => {
    vi.useFakeTimers()
    const markInFlight = vi.fn()
    const clearInFlight = vi.fn()
    const beginPageRequest = vi.fn(() => ({ signal: undefined }))
    const canSkipFetch = vi.fn()
      .mockReturnValueOnce(false)
      .mockReturnValueOnce(true)

    await renderProbe({
      loaders: {
        authToken: 'token',
        activePage: 'dashboard',
        commonPageLoaders: {
          dashboard: {
            signature: 'sig-2',
            delay: 50,
            run: vi.fn(async () => {}),
          },
        },
        keyPageLoaders: {},
        loadReasonRef: { current: 'default' },
        refreshReasons: new Set(['default']),
      },
      runtime: {
        beginPageRequest,
        canSkipFetch,
        isInFlight: () => false,
        markInFlight,
        clearInFlight,
      },
      metrics: {
        setLastRefreshReason: vi.fn(),
        setCurrentLoaderKey: vi.fn(),
        setCurrentPageSignature: vi.fn(),
        setCurrentSignatureHit: vi.fn(),
        markCacheDecision: vi.fn(),
        markDuplicateFetchBlocked: vi.fn(),
        markWorkbenchJumpDecision: vi.fn(),
        markFetched: vi.fn(),
        markFetchLatency: vi.fn(),
        markAbort: vi.fn(),
        markLoaderFallbackUsed: vi.fn(),
      },
      handlers: {
        handleError: vi.fn(),
        onLoaderLifecycle: vi.fn(),
        markRefreshSourceAnomaly: vi.fn(),
      },
    })

    await act(async () => {
      vi.advanceTimersByTime(60)
    })

    expect(beginPageRequest).not.toHaveBeenCalled()
    expect(clearInFlight).toHaveBeenCalled()
  })
})
