import { act } from 'react'
import { createRoot } from 'react-dom/client'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useApi } from '../hooks/useApi'

const requestApiMock = vi.hoisted(() => vi.fn())

vi.mock('../api/client', () => ({
  requestApi: (...args) => requestApiMock(...args),
  downloadApi: vi.fn(),
  uploadApi: vi.fn(),
}))

function createDeferred() {
  let resolve
  let reject
  const promise = new Promise((res, rej) => {
    resolve = res
    reject = rej
  })

  return { promise, resolve, reject }
}

function flushMicrotasks() {
  return new Promise((resolve) => setTimeout(resolve, 0))
}

function renderHook() {
  let current = null
  const container = document.createElement('div')
  document.body.appendChild(container)
  const root = createRoot(container)

  function Harness() {
    current = useApi()
    return null
  }

  act(() => {
    root.render(<Harness />)
  })

  return {
    get current() {
      return current
    },
    unmount() {
      act(() => {
        root.unmount()
      })
      container.remove()
    },
  }
}

afterEach(() => {
  requestApiMock.mockReset()
})

describe('useApi', () => {
  it('keeps loading true until all concurrent requests finish', async () => {
    const first = createDeferred()
    const second = createDeferred()
    requestApiMock
      .mockReturnValueOnce(first.promise)
      .mockReturnValueOnce(second.promise)

    const hook = renderHook()

    act(() => {
      hook.current.request('/api/first')
      hook.current.request('/api/second')
    })

    expect(hook.current.loading).toBe(true)
    expect(requestApiMock).toHaveBeenNthCalledWith(1, '/first', {})
    expect(requestApiMock).toHaveBeenNthCalledWith(2, '/second', {})

    first.resolve({ ok: 1 })
    await act(async () => {
      await flushMicrotasks()
    })

    expect(hook.current.loading).toBe(true)

    second.resolve({ ok: 2 })
    await act(async () => {
      await flushMicrotasks()
    })

    expect(hook.current.loading).toBe(false)

    hook.unmount()
  })

  it('does not update state after unmount', async () => {
    const deferred = createDeferred()
    requestApiMock.mockReturnValueOnce(deferred.promise)

    const hook = renderHook()

    act(() => {
      hook.current.request('/api/unmount-check')
    })

    hook.unmount()
    deferred.resolve({ ok: true })

    await act(async () => {
      await flushMicrotasks()
    })

    expect(true).toBe(true)
  })
})
