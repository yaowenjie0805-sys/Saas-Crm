import { memo, useCallback } from 'react'
import ListState from '../../../ListState'
import ServerPager from '../../../ServerPager'
import VirtualListTable from '../../../VirtualListTable'
import CustomerRow from './CustomerRow'

const CustomersTableSection = memo(function CustomersTableSection({
  t,
  allChecked,
  toggleAll,
  setSortBy,
  loading,
  rows,
  selectedIds,
  toggleOne,
  openDetail,
  editCustomer,
  canDeleteCustomer,
  removeCustomer,
  page,
  totalPages,
  pagination,
  onPageChange,
  onSizeChange,
}) {
  const toggleNameSort = useCallback(
    () => setSortBy((prev) => (prev === 'nameAsc' ? 'nameDesc' : 'nameAsc')),
    [setSortBy]
  )

  const toggleValueSort = useCallback(
    () => setSortBy((prev) => (prev === 'valueAsc' ? 'valueDesc' : 'valueAsc')),
    [setSortBy]
  )

  const getRowKey = useCallback((row) => row.id, [])

  const renderRow = useCallback(
    (row) => (
      <CustomerRow
        key={row.id}
        row={row}
        checked={selectedIds.has(row.id)}
        onToggle={(e) => toggleOne(row.id, e.target.checked)}
        t={t}
        openDetail={(item) => openDetail(item, 'drawer')}
        editCustomer={editCustomer}
        canDeleteCustomer={canDeleteCustomer}
        removeCustomer={removeCustomer}
      />
    ),
    [canDeleteCustomer, editCustomer, openDetail, removeCustomer, selectedIds, t, toggleOne]
  )

  return (
    <>
      <div className="table-row table-head-row table-row-6">
        <span><input type="checkbox" checked={allChecked} onChange={(e) => toggleAll(e.target.checked)} /></span>
        <button className="table-head-btn" onClick={toggleNameSort}>{t('companyName')}</button>
        <span>{t('owner')}</span>
        <span>{t('status')}</span>
        <button className="table-head-btn" onClick={toggleValueSort}>{t('amount')}</button>
        <span>{t('action')}</span>
      </div>
      <ListState loading={loading} empty={!loading && rows.length === 0} emptyText={t('noData')} />
      {!loading && rows.length > 0 && (
        <VirtualListTable
          rows={rows}
          viewportHeight={460}
          getRowKey={getRowKey}
          renderRow={renderRow}
        />
      )}
      {!loading && rows.length > 0 && <ServerPager t={t} page={page} totalPages={totalPages} size={pagination?.size || 8} onPageChange={onPageChange} onSizeChange={onSizeChange} />}
    </>
  )
})

export default CustomersTableSection
