import ServerPager from '../../ServerPager'
import VirtualListTable from '../../VirtualListTable'
import { LeadRow } from './LeadPanelRows'

export default function LeadListSection({
  t,
  rows,
  leads,
  selectedIds,
  allChecked,
  toggleAll,
  toggleOne,
  canWrite,
  editLead,
  convertLead,
  pagination,
  onPageChange,
  onSizeChange,
}) {
  return (
    <>
      <div className="table-row table-head-row table-row-6" data-testid="leads-list-header" style={{ marginTop: 12 }}>
        <span><input type="checkbox" checked={allChecked} onChange={(e) => toggleAll(e.target.checked)} /></span>
        <span>{t('leadName')}</span>
        <span>{t('companyName')}</span>
        <span>{t('status')}</span>
        <span>{t('owner')}</span>
        <span>{t('action')}</span>
      </div>
      <div data-testid="leads-list">
        <VirtualListTable
          rows={rows}
          viewportHeight={460}
          getRowKey={(row) => row.id}
          renderRow={(row) => (
            <LeadRow
              key={row.id}
              row={row}
              checked={selectedIds.has(row.id)}
              onToggle={(e) => toggleOne(row.id, e.target.checked)}
              canWrite={canWrite}
              t={t}
              editLead={editLead}
              convertLead={convertLead}
            />
          )}
        />
      </div>
      {(leads || []).length === 0 && <div className="empty-tip" data-testid="leads-empty">{t('noData')}</div>}
      {(rows || []).length > 0 && (
        <div data-testid="leads-pagination">
          <ServerPager
            t={t}
            page={pagination.page}
            totalPages={pagination.totalPages}
            size={pagination.size}
            onPageChange={onPageChange}
            onSizeChange={onSizeChange}
          />
        </div>
      )}
    </>
  )
}
