import { useCallback, useMemo, useState } from 'react'

export function useSelectionSet(rows, getRowId) {
  const [internalSelectedIds, setInternalSelectedIds] = useState(() => new Set())

  const idsOnPage = useMemo(
    () => (rows || []).map((row) => getRowId(row)).filter((id) => id !== null && id !== undefined && id !== ''),
    [rows, getRowId],
  )

  const selectedIds = useMemo(() => {
    if (!internalSelectedIds.size) return new Set()
    const pageSet = new Set(idsOnPage)
    return new Set([...internalSelectedIds].filter((id) => pageSet.has(id)))
  }, [internalSelectedIds, idsOnPage])

  const selectedCount = selectedIds.size
  const allChecked = idsOnPage.length > 0 && idsOnPage.every((id) => selectedIds.has(id))

  const clearSelection = useCallback(() => {
    setInternalSelectedIds(new Set())
  }, [])

  const selectPage = useCallback(() => {
    setInternalSelectedIds(new Set(idsOnPage))
  }, [idsOnPage])

  const toggleAll = useCallback((checked) => {
    setInternalSelectedIds(checked ? new Set(idsOnPage) : new Set())
  }, [idsOnPage])

  const toggleOne = useCallback((id, checked) => {
    setInternalSelectedIds((prev) => {
      const next = new Set(prev)
      if (checked) next.add(id)
      else next.delete(id)
      return next
    })
  }, [])

  return {
    selectedIds,
    setSelectedIds: setInternalSelectedIds,
    selectedCount,
    allChecked,
    clearSelection,
    selectPage,
    toggleAll,
    toggleOne,
  }
}
