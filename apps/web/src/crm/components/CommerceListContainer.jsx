import { memo, useMemo } from 'react'
import ListState from '../ListState'
import { useBatchActions } from '../useBatchActions'
import BatchResultModal from '../BatchResultModal'
import VirtualListTable from '../VirtualListTable'
import { useSelectionSet } from '../../hooks/useSelectionSet'

/**
 * 通用列表容器组件
 * 提取 QuotesPanel 和 OrdersPanel 的共同模式
 */
const CommerceListContainer = memo(function CommerceListContainer({
  t,
  items,
  loading,
  error,
  lastLoadedAt,
  stale,
  statusFilter,
  setStatusFilter,
  ownerFilter,
  setOwnerFilter,
  statusOptions,
  renderHeader,
  renderRow,
  getRowKey = (row) => row.id,
  viewportHeight = 460,
  children,
}) {
  const { summary: batchSummary, toastMessage: batchMessage, clearSummary } = useBatchActions({ t })

  const filteredItems = useMemo(() => {
    const owner = ownerFilter.trim().toLowerCase()
    return (items || []).filter((row) => {
      if (statusFilter && String(row.status || '') !== statusFilter) return false
      if (owner && !String(row.owner || '').toLowerCase().includes(owner)) return false
      return true
    })
  }, [items, statusFilter, ownerFilter])

  const selection = useSelectionSet(filteredItems, getRowKey)
  const { selectedIds, selectedCount, clearSelection, selectPage, toggleOne } = selection

  return (
    <>
      {renderHeader?.({ selectedCount, clearSelection, selectPage, batchSummary })}

      <div className="filter-row mb-2">
        <input
          className="tool-input"
          placeholder={t('owner')}
          value={ownerFilter}
          onChange={(e) => setOwnerFilter(e.target.value)}
        />
        <select className="tool-input" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">{t('allStatuses')}</option>
          {statusOptions.map((opt) => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
        </select>
      </div>

      {batchMessage && <div className="info-banner mb-2">{batchMessage}</div>}
      {error && <div className="field-error mb-2">{error}</div>}
      {!!lastLoadedAt && stale && <div className="info-banner mb-2">{t('loading')}</div>}

      {children}

      <ListState loading={loading} empty={!loading && filteredItems.length === 0} emptyText={t('noData')} />

      {!loading && filteredItems.length > 0 && (
        <VirtualListTable
          rows={filteredItems}
          viewportHeight={viewportHeight}
          getRowKey={getRowKey}
          renderRow={(row) => renderRow?.({
            row,
            checked: selectedIds.has(getRowKey(row)),
            onToggle: (e) => toggleOne(getRowKey(row), e.target.checked),
          })}
        />
      )}

      <BatchResultModal
        t={t}
        open={false}
        summary={batchSummary}
        onClose={() => clearSummary()}
      />
    </>
  )
})

export default CommerceListContainer
