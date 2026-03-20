import { useMemo, useState } from 'react'
import { formatMoney, translateStatus } from '../../../../shared'
import { useSelectionSet } from '../../../../hooks/useSelectionSet'

/**
 * Manage list-level state for the customers page.
 * Keeps sorting, filter draft synchronization and selection behavior together.
 */
export function useCustomersListModel({
  customers,
  t,
  pagination,
  customerQ,
  setCustomerQ,
  customerStatus,
  setCustomerStatus,
  onPageChange,
}) {
  const [sortBy, setSortBy] = useState('nameAsc')
  const [customerQDraft, setCustomerQDraft] = useState(customerQ || '')
  const [customerStatusDraft, setCustomerStatusDraft] = useState(customerStatus || '')

  const rows = useMemo(() => {
    const sorted = [...(customers || [])]
    if (sortBy === 'nameAsc') sorted.sort((a, b) => String(a.name || '').localeCompare(String(b.name || '')))
    if (sortBy === 'nameDesc') sorted.sort((a, b) => String(b.name || '').localeCompare(String(a.name || '')))
    if (sortBy === 'valueAsc') sorted.sort((a, b) => Number(a.value || 0) - Number(b.value || 0))
    if (sortBy === 'valueDesc') sorted.sort((a, b) => Number(b.value || 0) - Number(a.value || 0))
    return sorted.map((row) => ({ ...row, statusLabel: translateStatus(t, row.status), valueText: formatMoney(row.value) }))
  }, [customers, sortBy, t])

  const rowMap = useMemo(() => new Map(rows.map((row) => [String(row.id), row])), [rows])
  const byId = useMemo(() => new Map((customers || []).map((row) => [row.id, row])), [customers])
  const rowIndexById = useMemo(() => {
    const map = new Map()
    rows.forEach((row, idx) => map.set(String(row.id), idx))
    return map
  }, [rows])
  const page = pagination?.page || 1
  const totalPages = Math.max(1, pagination?.totalPages || 1)
  const selection = useSelectionSet(rows, (row) => row.id)

  const applyFilters = () => {
    if (customerQ !== customerQDraft) setCustomerQ(customerQDraft)
    if (customerStatus !== customerStatusDraft) setCustomerStatus(customerStatusDraft)
    onPageChange(1)
  }

  const resetFilters = () => {
    setCustomerQDraft('')
    setCustomerStatusDraft('')
    if (customerQ !== '') setCustomerQ('')
    if (customerStatus !== '') setCustomerStatus('')
    onPageChange(1)
  }

  return {
    rows,
    rowMap,
    byId,
    rowIndexById,
    page,
    totalPages,
    selection,
    sortBy,
    setSortBy,
    customerQDraft,
    setCustomerQDraft,
    customerStatusDraft,
    setCustomerStatusDraft,
    applyFilters,
    resetFilters,
  }
}
