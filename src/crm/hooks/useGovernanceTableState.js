import { useCallback, useState } from 'react'

export function useGovernanceTableState(initial = {}) {
  const [query, setQuery] = useState(initial.query || '')
  const [filter, setFilter] = useState(initial.filter || '')
  const [page, setPage] = useState(initial.page || 1)
  const [size, setSize] = useState(initial.size || 8)

  const updateQuery = useCallback((value) => {
    setQuery(value)
    setPage(1)
  }, [])

  const updateFilter = useCallback((value) => {
    setFilter(value)
    setPage(1)
  }, [])

  const updateSize = useCallback((value) => {
    setSize(value)
    setPage(1)
  }, [])

  const reset = useCallback(() => {
    setQuery(initial.query || '')
    setFilter(initial.filter || '')
    setPage(1)
  }, [initial.filter, initial.query])

  return {
    query,
    setQuery: updateQuery,
    filter,
    setFilter: updateFilter,
    page,
    setPage,
    size,
    setSize: updateSize,
    reset,
  }
}

