import { CUSTOMER_STATUS_OPTIONS, translateStatus } from '../../../../shared'

export default function CustomerFilterBar({
  t,
  customerQDraft,
  setCustomerQDraft,
  customerStatusDraft,
  setCustomerStatusDraft,
  applyFilters,
  resetFilters,
  onRefresh,
}) {
  return (
    <>
      <div className="inline-tools filter-row" style={{ marginBottom: 8 }}>
        <input data-testid="customers-search-input" className="tool-input" placeholder={t('search')} value={customerQDraft} onChange={(e) => setCustomerQDraft(e.target.value)} onKeyDown={(e) => { if (e.key === 'Enter') applyFilters() }} />
        <select data-testid="customers-search-status" className="tool-input" value={customerStatusDraft} onChange={(e) => setCustomerStatusDraft(e.target.value)} onKeyDown={(e) => { if (e.key === 'Enter') applyFilters() }}>
          <option value="">{t('allStatuses')}</option>
          {CUSTOMER_STATUS_OPTIONS.map((status) => <option key={status} value={status}>{translateStatus(t, status)}</option>)}
        </select>
      </div>
      <div className="inline-tools filter-bar" style={{ marginBottom: 10 }}>
        <button className="mini-btn" data-testid="customers-search-submit" onClick={applyFilters}>{t('search')}</button>
        <button className="mini-btn" data-testid="customers-search-reset" onClick={resetFilters}>{t('reset')}</button>
        <button className="mini-btn" data-testid="customers-refresh" onClick={() => onRefresh && onRefresh()}>{t('refresh')}</button>
      </div>
    </>
  )
}
