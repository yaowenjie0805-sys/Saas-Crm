import { startTransition, useCallback } from 'react'

/**
 * Global search bridge that maps topbar query into page-level filters.
 * Behavior remains non-breaking: unsupported pages are no-op.
 */
export function useGlobalSearchBridge({
  activePage,
  customers,
  leads,
  commerce,
  refreshPage,
  setNotice,
  t,
}) {
  return useCallback((rawQuery) => {
    const query = String(rawQuery || '').trim()
    const isEmpty = query.length === 0
    const translated = typeof t === 'function' ? t : (key) => String(key || '')

    if (activePage === 'customers') {
      startTransition(() => {
        customers?.setCustomerQ?.(query)
      })
      refreshPage('customers', isEmpty ? 'topbar_search_clear' : 'topbar_search')
      setNotice('')
      return
    }

    if (activePage === 'quotes') {
      startTransition(() => {
        commerce?.quotes?.setOwnerFilter?.(query)
      })
      refreshPage('quotes', isEmpty ? 'topbar_search_clear' : 'topbar_search')
      setNotice('')
      return
    }

    if (activePage === 'orders') {
      startTransition(() => {
        commerce?.orders?.setOwnerFilter?.(query)
      })
      refreshPage('orders', isEmpty ? 'topbar_search_clear' : 'topbar_search')
      setNotice('')
      return
    }

    if (activePage === 'leads') {
      startTransition(() => {
        leads?.setLeadQ?.(query)
      })
      refreshPage('leads', isEmpty ? 'topbar_search_clear' : 'topbar_search')
      setNotice('')
      return
    }

    const localized = translated('globalSearchUnsupportedPage')
    setNotice(localized && localized !== 'globalSearchUnsupportedPage' ? localized : 'Search is not supported on this page')
  }, [activePage, commerce, customers, leads, refreshPage, setNotice, t])
}
