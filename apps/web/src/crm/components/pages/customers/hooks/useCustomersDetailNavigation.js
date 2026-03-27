import { useCallback, useMemo, useState } from 'react'

/**
 * Controls customer detail navigation state (drawer/page + URL synchronization).
 * Data loading remains orchestrated by the runtime component.
 */
export function useCustomersDetailNavigation({
  activePage,
  t,
  location,
  navigate,
  rowMap,
  rowIndexById,
  rows,
}) {
  const [localDetail, setLocalDetail] = useState(null)
  const [localDetailMode, setLocalDetailMode] = useState('drawer')

  const urlDetail = useMemo(() => {
    if (activePage !== 'customers') return null
    const query = new URLSearchParams(location.search)
    const view = query.get('view')
    const customerId = query.get('customerId')
    if (view !== 'detail' || !customerId) return null
    const row = rowMap.get(String(customerId))
    return row || { id: customerId, name: `${t('customers')} #${customerId}` }
  }, [activePage, location.search, rowMap, t])

  const detail = localDetail || urlDetail
  const detailMode = localDetail ? localDetailMode : (urlDetail ? 'page' : localDetailMode)
  const detailSource = localDetail ? 'local' : (urlDetail ? 'url' : null)

  const openDetailState = useCallback((row, mode = 'drawer') => {
    setLocalDetail(row)
    setLocalDetailMode(mode)
  }, [])

  const openFullDetailUrl = useCallback((row) => {
    if (!row?.id) return
    const query = new URLSearchParams(location.search)
    query.set('view', 'detail')
    query.set('customerId', String(row.id))
    navigate(`${location.pathname}?${query.toString()}`, { replace: false })
  }, [location.pathname, location.search, navigate])

  const resolveNeighbor = useCallback((step) => {
    if (!detail?.id) return null
    const idx = rowIndexById.get(String(detail.id))
    if (idx === undefined) return null
    return rows[idx + step] || null
  }, [detail, rowIndexById, rows])

  const closeFullDetail = useCallback(() => {
    const query = new URLSearchParams(location.search)
    query.delete('view')
    query.delete('customerId')
    const suffix = query.toString()
    navigate(`${location.pathname}${suffix ? `?${suffix}` : ''}`, { replace: false })
    setLocalDetailMode('drawer')
    setLocalDetail(null)
  }, [location.pathname, location.search, navigate])

  const closeDrawer = useCallback(() => {
    setLocalDetail(null)
    setLocalDetailMode('drawer')
  }, [])

  return {
    detail,
    detailMode,
    detailSource,
    openDetailState,
    openFullDetailUrl,
    resolveNeighbor,
    closeFullDetail,
    closeDrawer,
  }
}
