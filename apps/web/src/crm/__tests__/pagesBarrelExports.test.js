import { describe, expect, it } from 'vitest'
import * as pages from '../components/pages'
import * as apiHooks from '../hooks/api'

describe('pages barrel exports', () => {
  it('exposes page domains through named exports', () => {
    expect(pages.default).toBeUndefined()
    expect(pages.DashboardPage).toBeDefined()
    expect(pages.CustomersPage).toBeDefined()
    expect(pages.ApprovalsPage).toBeDefined()
    expect(pages.ImportExportPage).toBeDefined()
  })

  it('exposes hook facades through named exports only', () => {
    expect(apiHooks.default).toBeUndefined()
    expect(apiHooks.useApiClient).toBeDefined()
    expect(apiHooks.useWorkflowApi).toBeDefined()
    expect(apiHooks.useApprovalApi).toBeDefined()
    expect(apiHooks.useImportExportApi).toBeDefined()
  })
})
