import { CUSTOMER_STATUS_OPTIONS, translateStatus } from '../../../../shared'

export default function CustomerBatchToolbar({
  t,
  selectedCount,
  selectPage,
  clearSelection,
  batchOwner,
  setBatchOwner,
  batchStatus,
  setBatchStatus,
  batchAssign,
  batchChangeStatus,
  canWrite,
  canDeleteCustomer,
  batchDelete,
  batchSummary,
  setBatchModalOpen,
  batchMessage,
}) {
  return (
    <>
      <div className="inline-tools filter-bar" style={{ marginBottom: 10 }}>
        <span className="muted-filter">{t('batchSelectedCount')}: {selectedCount}</span>
        <button className="mini-btn" onClick={selectPage}>{t('selectPage')}</button>
        <button className="mini-btn" onClick={clearSelection}>{t('clearSelection')}</button>
        <input className="tool-input" placeholder={t('batchOwnerPlaceholder')} value={batchOwner} onChange={(e) => setBatchOwner(e.target.value)} />
        <select className="tool-input" value={batchStatus} onChange={(e) => setBatchStatus(e.target.value)}>
          <option value="">{t('batchSetStatus')}</option>
          {CUSTOMER_STATUS_OPTIONS.map((status) => <option key={status} value={status}>{translateStatus(t, status)}</option>)}
        </select>
        <button className="mini-btn" disabled={!canWrite} onClick={batchAssign}>{t('batchAssignOwner')}</button>
        <button className="mini-btn" disabled={!canWrite} onClick={batchChangeStatus}>{t('batchSetStatus')}</button>
        {canDeleteCustomer && <button className="danger-btn" onClick={batchDelete}>{t('batchDelete')}</button>}
        {batchSummary?.failed > 0 && <button className="mini-btn" onClick={() => setBatchModalOpen(true)}>{t('batchResultTitle')}</button>}
      </div>
      {batchMessage && <div className="info-banner" style={{ marginBottom: 8 }}>{batchMessage}</div>}
    </>
  )
}
