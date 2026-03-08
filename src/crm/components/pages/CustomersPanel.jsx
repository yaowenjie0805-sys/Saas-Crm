import { useMemo, useState } from 'react'
import { CUSTOMER_STATUS_OPTIONS, formatMoney, translateStatus } from '../../shared'
import ListState from '../ListState'
import RowDetailDrawer from '../RowDetailDrawer'
import ServerPager from '../ServerPager'

function CustomersPanel({
  activePage,
  t,
  canWrite,
  customerForm,
  setCustomerForm,
  saveCustomer,
  formError,
  fieldErrors,
  customers,
  editCustomer,
  canDeleteCustomer,
  removeCustomer,
  loading,
  customerQ,
  setCustomerQ,
  customerStatus,
  setCustomerStatus,
  pagination,
  onPageChange,
  onSizeChange,
  reload,
}) {
  const [sortBy, setSortBy] = useState('nameAsc')
  const [detail, setDetail] = useState(null)

  const toggleSort = (key) => {
    setSortBy((prev) => {
      if (prev === `${key}Asc`) return `${key}Desc`
      return `${key}Asc`
    })
  }

  const rows = useMemo(() => {
    const sorted = [...(customers || [])]
    if (sortBy === 'nameAsc') sorted.sort((a, b) => String(a.name || '').localeCompare(String(b.name || '')))
    if (sortBy === 'nameDesc') sorted.sort((a, b) => String(b.name || '').localeCompare(String(a.name || '')))
    if (sortBy === 'valueAsc') sorted.sort((a, b) => Number(a.value || 0) - Number(b.value || 0))
    if (sortBy === 'valueDesc') sorted.sort((a, b) => Number(b.value || 0) - Number(a.value || 0))
    return sorted
  }, [customers, sortBy])

  const page = pagination?.page || 1
  const totalPages = Math.max(1, pagination?.totalPages || 1)

  if (activePage !== 'customers') return null

  return (
    <section className="panel">
      <div className="panel-head"><h2>{t('customers')}</h2></div>
      <div className="inline-tools" style={{ marginBottom: 10 }}>
        <input className={fieldErrors?.name ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('companyName')} value={customerForm.name} onChange={(e) => setCustomerForm((p) => ({ ...p, name: e.target.value }))} />
        <input className={fieldErrors?.owner ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('owner')} value={customerForm.owner} onChange={(e) => setCustomerForm((p) => ({ ...p, owner: e.target.value }))} />
        <select className={fieldErrors?.status ? 'tool-input input-invalid' : 'tool-input'} value={customerForm.status} onChange={(e) => setCustomerForm((p) => ({ ...p, status: e.target.value }))}>
          <option value="">{t('selectPlaceholder')}</option>
          {CUSTOMER_STATUS_OPTIONS.map((status) => <option key={status} value={status}>{translateStatus(t, status)}</option>)}
        </select>
        <input className={fieldErrors?.value ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('amount')} value={customerForm.value} onChange={(e) => setCustomerForm((p) => ({ ...p, value: e.target.value }))} />
        <button className="mini-btn" disabled={!canWrite} onClick={saveCustomer}>{customerForm.id ? t('save') : t('create')}</button>
        <button className="mini-btn" onClick={() => setCustomerForm({ id: '', name: '', owner: '', status: '', tag: '', value: '' })}>{t('reset')}</button>
      </div>
      {(fieldErrors?.name || fieldErrors?.owner || fieldErrors?.status || fieldErrors?.value) && (
        <div className="field-error" style={{ marginBottom: 8 }}>
          {fieldErrors?.name || fieldErrors?.owner || fieldErrors?.status || fieldErrors?.value}
        </div>
      )}
      {formError && <div className="field-error" style={{ marginBottom: 8 }}>{formError}</div>}
      <div className="inline-tools" style={{ marginBottom: 10 }}>
        <input className="tool-input" placeholder={t('search')} value={customerQ} onChange={(e) => setCustomerQ(e.target.value)} />
        <select className="tool-input" value={customerStatus} onChange={(e) => setCustomerStatus(e.target.value)}>
          <option value="">{t('allStatuses')}</option>
          {CUSTOMER_STATUS_OPTIONS.map((status) => <option key={status} value={status}>{translateStatus(t, status)}</option>)}
        </select>
        <button className="mini-btn" onClick={() => onPageChange(1)}>{t('search')}</button>
        <button className="mini-btn" onClick={() => reload(page)}>{t('refresh')}</button>
      </div>
      <div className="table-row table-head-row">
        <button className="table-head-btn" onClick={() => toggleSort('name')}>{t('companyName')}</button>
        <span>{t('owner')}</span>
        <span>{t('status')}</span>
        <button className="table-head-btn" onClick={() => toggleSort('value')}>{t('amount')}</button>
        <span>{t('action')}</span>
      </div>
      <ListState loading={loading} empty={!loading && rows.length === 0} emptyText={t('noData')} />
      {!loading && rows.map((c) => <div key={c.id} className="table-row"><span>{c.name}</span><span>{c.owner}</span><span>{translateStatus(t, c.status)}</span><span>{formatMoney(c.value)}</span><span><button className="mini-btn" onClick={() => setDetail(c)}>{t('detail')}</button><button className="mini-btn" onClick={() => editCustomer(c)}>{t('save')}</button>{canDeleteCustomer ? <button className="danger-btn" onClick={() => removeCustomer(c.id)}>{t('delete')}</button> : null}</span></div>)}
      {!loading && rows.length > 0 && <ServerPager t={t} page={page} totalPages={totalPages} size={pagination?.size || 8} onPageChange={onPageChange} onSizeChange={onSizeChange} />}
      <RowDetailDrawer open={!!detail} title={t('customers')} t={t} onClose={() => setDetail(null)} rows={[
        { label: t('idLabel'), value: detail?.id },
        { label: t('companyName'), value: detail?.name },
        { label: t('owner'), value: detail?.owner },
        { label: t('status'), value: translateStatus(t, detail?.status) },
        { label: t('amount'), value: detail ? formatMoney(detail.value) : '-' },
      ]} />
    </section>
  )
}

export default CustomersPanel
