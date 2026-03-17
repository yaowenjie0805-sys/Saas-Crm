import { useCallback, useMemo, useState } from 'react'

export function useSelectionSet(rows, getRowId) {
  const [selectedIds, setSelectedIds] = useState(() => new Set())

  const idsOnPage = useMemo(
    () => (rows || []).map((row) => getRowId(row)).filter((id) => id !== null && id !== undefined && id !== ''),
    [rows, getRowId],
  )

  const selectedCount = selectedIds.size
  const allChecked = idsOnPage.length > 0 && idsOnPage.every((id) => selectedIds.has(id))

  const clearSelection = useCallback(() => {
    setSelectedIds(new Set())
  }, [])

  const selectPage = useCallback(() => {
    setSelectedIds(new Set(idsOnPage))
  }, [idsOnPage])

  const toggleAll = useCallback((checked) => {
    setSelectedIds(checked ? new Set(idsOnPage) : new Set())
  }, [idsOnPage])

  const toggleOne = useCallback((id, checked) => {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (checked) next.add(id)
      else next.delete(id)
      return next
    })
  }, [])

  return {
    selectedIds,
    setSelectedIds,
    selectedCount,
    allChecked,
    clearSelection,
    selectPage,
    toggleAll,
    toggleOne,
  }
}

