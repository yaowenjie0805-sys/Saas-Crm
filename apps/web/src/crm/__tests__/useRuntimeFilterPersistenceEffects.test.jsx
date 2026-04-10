import React, { act } from 'react'
import { createRoot } from 'react-dom/client'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { FILTERS_KEY } from '../shared'
import { useRuntimeFilterPersistenceEffects } from '../hooks/orchestrators/runtime/useRuntimeFilterPersistenceEffects'

const mountedRoots = []
globalThis.IS_REACT_ACT_ENVIRONMENT = true

const PAGE_SIZE_KEYS = [
  'crm_page_size_customers',
  'crm_page_size_leads',
  'crm_page_size_opportunities',
  'crm_page_size_contacts',
  'crm_page_size_contracts',
  'crm_page_size_payments',
  'crm_page_size_notification_jobs',
  'crm_page_size_lead_import_export_jobs',
]

const baseArgs = {
  leadQ: '',
  leadStatus: '',
  customerQ: '',
  customerStatus: '',
  oppStage: '',
  followCustomerId: '',
  followQ: '',
  contactQ: '',
  contractQ: '',
  contractStatus: '',
  paymentStatus: '',
  auditUser: '',
  auditRole: '',
  auditAction: '',
  auditFrom: '',
  auditTo: '',
  reportOwner: '',
  reportDepartment: '',
  reportTimezone: '',
  reportCurrency: '',
  customerSize: 20,
  leadSize: 20,
  opportunitySize: 20,
  contactSize: 20,
  contractSize: 20,
  paymentSize: 20,
  notificationSize: 20,
  leadImportExportSize: 20,
}

function Probe({ args }) {
  useRuntimeFilterPersistenceEffects(args)
  return null
}

const renderProbe = async (argsOverride = {}) => {
  const container = document.createElement('div')
  document.body.appendChild(container)
  const root = createRoot(container)
  mountedRoots.push({ root, container })
  await act(async () => {
    root.render(<Probe args={{ ...baseArgs, ...argsOverride }} />)
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
  localStorage.clear()
  vi.restoreAllMocks()
  vi.useRealTimers()
})

describe('useRuntimeFilterPersistenceEffects', () => {
  it('persists filters with debounce', async () => {
    vi.useFakeTimers()
    localStorage.setItem(FILTERS_KEY, '')
    for (const key of PAGE_SIZE_KEYS) {
      localStorage.setItem(key, '20')
    }
    const setItemSpy = vi.spyOn(Storage.prototype, 'setItem')

    await renderProbe({ leadQ: 'alice' })

    expect(setItemSpy).not.toHaveBeenCalledWith(FILTERS_KEY, expect.any(String))

    await act(async () => {
      vi.advanceTimersByTime(120)
    })

    expect(setItemSpy).toHaveBeenCalledWith(
      FILTERS_KEY,
      expect.stringContaining('"leadQ":"alice"'),
    )
  })

  it('does not write invalid page size value when size is undefined', async () => {
    localStorage.setItem('crm_page_size_customers', '20')

    await renderProbe({ customerSize: undefined })

    expect(localStorage.getItem('crm_page_size_customers')).toBe('20')
  })

  it('does not write invalid page size value when size is non-positive', async () => {
    localStorage.setItem('crm_page_size_customers', '20')

    await renderProbe({ customerSize: -1 })

    expect(localStorage.getItem('crm_page_size_customers')).toBe('20')
  })
})
