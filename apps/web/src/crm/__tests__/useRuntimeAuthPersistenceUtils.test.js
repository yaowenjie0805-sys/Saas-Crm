import { describe, expect, it } from 'vitest'
import {
  createOidcExchangeCodeCache,
  isValidOidcState,
  resolveOidcTenantId,
} from '../hooks/orchestrators/runtime/useRuntimeAuthPersistenceUtils'

describe('useRuntimeAuthPersistenceUtils', () => {
  it('resolveOidcTenantId prefers login form tenant then falls back to cached tenant', () => {
    expect(resolveOidcTenantId('  tenant_a  ', 'tenant_b')).toBe('tenant_a')
    expect(resolveOidcTenantId('   ', ' tenant_b ')).toBe('tenant_b')
    expect(resolveOidcTenantId('', null)).toBe('')
  })

  it('isValidOidcState returns true only when expected and actual state are equal', () => {
    expect(isValidOidcState('abc', 'abc')).toBe(true)
    expect(isValidOidcState('abc', 'def')).toBe(false)
    expect(isValidOidcState('', 'abc')).toBe(false)
    expect(isValidOidcState('abc', '')).toBe(false)
  })

  it('createOidcExchangeCodeCache respects ttl and max size', () => {
    const cache = createOidcExchangeCodeCache({ ttlMs: 10, maxSize: 2 })
    cache.remember('c1', 1)
    cache.remember('c2', 2)
    expect(cache.hasFresh('c1', 5)).toBe(true)
    cache.remember('c3', 3)
    expect(cache.hasFresh('c1', 5)).toBe(false)
    expect(cache.hasFresh('c2', 5)).toBe(true)
    expect(cache.hasFresh('c3', 5)).toBe(true)
    expect(cache.hasFresh('c2', 20)).toBe(false)
  })

  it('createOidcExchangeCodeCache allows forgetting a remembered code', () => {
    const cache = createOidcExchangeCodeCache({ ttlMs: 100, maxSize: 4 })
    cache.remember('c1', 1)
    expect(cache.hasFresh('c1', 5)).toBe(true)
    cache.forget('c1')
    expect(cache.hasFresh('c1', 5)).toBe(false)
  })
})
