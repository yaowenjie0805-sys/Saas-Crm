import { describe, expect, it } from 'vitest'
import { normalizeRuntimePageSizeValue } from '../hooks/orchestrators/runtime/useRuntimeFilterPersistenceUtils'

describe('useRuntimeFilterPersistenceUtils', () => {
  it('normalizes valid numeric page size values', () => {
    expect(normalizeRuntimePageSizeValue(20)).toBe('20')
    expect(normalizeRuntimePageSizeValue('30')).toBe('30')
    expect(normalizeRuntimePageSizeValue(9.8)).toBe('9')
  })

  it('returns null for invalid page size values', () => {
    expect(normalizeRuntimePageSizeValue(undefined)).toBeNull()
    expect(normalizeRuntimePageSizeValue(null)).toBeNull()
    expect(normalizeRuntimePageSizeValue(0)).toBeNull()
    expect(normalizeRuntimePageSizeValue(-1)).toBeNull()
    expect(normalizeRuntimePageSizeValue('abc')).toBeNull()
  })
})
