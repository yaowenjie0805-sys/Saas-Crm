import ListState from '../../../ListState'
import ServerPager from '../../../ServerPager'
import VirtualListTable from '../../../VirtualListTable'
import CustomerRow from './CustomerRow'

export default function CustomersTableSection({
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
  return (
    <>
      <div className="table-row table-head-row table-row-6">
        <span><input type="checkbox" checked={allChecked} onChange={(e) => toggleAll(e.target.checked)} /></span>
        <button className="table-head-btn" onClick={() => setSortBy((prev) => (prev === 'nameAsc' ? 'nameDesc' : 'nameAsc'))}>{t('companyName')}</button>
        <span>{t('owner')}</span>
        <span>{t('status')}</span>
        <button className="table-head-btn" onClick={() => setSortBy((prev) => (prev === 'valueAsc' ? 'valueDesc' : 'valueAsc'))}>{t('amount')}</button>
        <span>{t('action')}</span>
      </div>
      <ListState loading={loading} empty={!loading && rows.length === 0} emptyText={t('noData')} />
      {!loading && rows.length > 0 && (
        <VirtualListTable
          rows={rows}
          viewportHeight={460}
          getRowKey={(row) => row.id}
          renderRow={(row) => (
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
          )}
        />
      )}
      {!loading && rows.length > 0 && <ServerPager t={t} page={page} totalPages={totalPages} size={pagination?.size || 8} onPageChange={onPageChange} onSizeChange={onSizeChange} />}
    </>
  )
}
