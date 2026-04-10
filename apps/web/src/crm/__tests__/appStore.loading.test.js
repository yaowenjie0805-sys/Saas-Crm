import { afterEach, expect, test } from 'vitest'
import { useAppStore } from '../store/appStore'

const baselineState = useAppStore.getState()

afterEach(() => {
  useAppStore.setState(baselineState, true)
})

test('stale loading completion should not override newer in-flight request', () => {
  const store = useAppStore.getState()

  store.loadStarted('customerDomain', 'req-1')
  store.loadStarted('customerDomain', 'req-2')
  store.loadSucceeded('customerDomain', 'req-1')
  store.loadFailed('customerDomain', 'stale error', 'req-1')

  const loadingState = useAppStore.getState().loading.customerDomain
  expect(loadingState.status).toBe('loading')
  expect(loadingState.requestId).toBe('req-2')
  expect(loadingState.error).toBe('')

  store.loadSucceeded('customerDomain', 'req-2')
  const completedState = useAppStore.getState().loading.customerDomain
  expect(completedState.status).toBe('success')
  expect(completedState.requestId).toBe('req-2')
})

test('completion without requestId remains backward compatible', () => {
  const store = useAppStore.getState()

  store.loadStarted('customerDomain', 'req-legacy')
  store.loadSucceeded('customerDomain')

  const loadingState = useAppStore.getState().loading.customerDomain
  expect(loadingState.status).toBe('success')
  expect(loadingState.requestId).toBe('')
})
