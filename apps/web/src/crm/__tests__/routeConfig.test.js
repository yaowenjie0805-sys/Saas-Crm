import { describe, expect, it } from 'vitest'
import {
  PAGE_CHUNK_PRELOADERS,
  PAGE_DOMAIN_MAP,
  PAGE_DOMAIN_PRELOADERS,
  PAGE_TO_PATH,
  PATH_TO_PAGE,
  REFRESH_REASONS,
  normalizeDateFormat,
  parseDateByFormat,
  isValidDateStringByTenantFormat,
} from '../hooks/orchestrators/runtime/routeConfig'

describe('routeConfig', () => {
  it('maps pages to routes and back', () => {
    expect(PAGE_TO_PATH.dashboard).toBe('/dashboard')
    expect(PATH_TO_PAGE['/reports/designer']).toBe('reportDesigner')
    expect(PAGE_DOMAIN_MAP.approvals).toBe('approval')
    expect(REFRESH_REASONS.has('sidebar_nav')).toBe(true)
  })

  it('provides page preloaders as lazy functions', async () => {
    expect(typeof PAGE_CHUNK_PRELOADERS.dashboard).toBe('function')
    expect(typeof PAGE_CHUNK_PRELOADERS.approvals).toBe('function')
    expect(typeof PAGE_DOMAIN_PRELOADERS.dashboard).toBe('function')
    expect(PAGE_DOMAIN_PRELOADERS.dashboard()).resolves.toBeUndefined()
  })

  it('normalizes and parses tenant date formats', () => {
    expect(normalizeDateFormat('YYYY-MM-DD')).toBe('yyyy-MM-dd')
    expect(normalizeDateFormat('dd/MM/yyyy')).toBe('dd/MM/yyyy')
    expect(parseDateByFormat('2026-04-02', 'yyyy-MM-dd')).toBeInstanceOf(Date)
    expect(parseDateByFormat('02/04/2026', 'dd/MM/yyyy')).toBeInstanceOf(Date)
    expect(isValidDateStringByTenantFormat('2026-04-02', 'YYYY-MM-DD')).toBe(true)
    expect(isValidDateStringByTenantFormat('', 'YYYY-MM-DD')).toBe(true)
  })
})
