import { useMemo, useState } from 'react'
import { formatMoney, PAYMENT_METHOD_OPTIONS, PAYMENT_STATUS_OPTIONS, translateMethod, translateStatus } from '../../shared'
import ListState from '../ListState'
import RowDetailDrawer from '../RowDetailDrawer'
import ServerPager from '../ServerPager'

function PaymentsPanel({
  activePage,
  t,
  canWrite,
  canDeleteCustomer,
  paymentForm,
  setPaymentForm,
  savePayment,
  formError,
  fieldErrors,
  payments,
  editPayment,
  removePayment,
  loading,
  paymentStatus,
  setPaymentStatus,
  pagination,
  onPageChange,
  onSizeChange,
  reload,
}) {
  const [sortBy, setSortBy] = useState('dateDesc')
  const [detail, setDetail] = useState(null)

  const toggleSort = (key) => {
    setSortBy((prev) => {
      if (prev === `${key}Asc`) return `${key}Desc`
      return `${key}Asc`
    })
  }

  const rows = useMemo(() => {
    const sorted = [...(payments || [])]
    if (sortBy === 'amountDesc') sorted.sort((a, b) => Number(b.amount || 0) - Number(a.amount || 0))
    if (sortBy === 'amountAsc') sorted.sort((a, b) => Number(a.amount || 0) - Number(b.amount || 0))
    if (sortBy === 'dateDesc') sorted.sort((a, b) => Date.parse(b.receivedDate || '') - Date.parse(a.receivedDate || ''))
    if (sortBy === 'dateAsc') sorted.sort((a, b) => Date.parse(a.receivedDate || '') - Date.parse(b.receivedDate || ''))
    return sorted
  }, [payments, sortBy])

  const page = pagination?.page || 1
  const totalPages = Math.max(1, pagination?.totalPages || 1)

  if (activePage !== 'payments') return null

  return (
    <section className="panel">
      <div className="panel-head"><h2>{t('payments')}</h2></div>
      <div className="inline-tools" style={{ marginBottom: 10 }}>
        <input className={fieldErrors?.contractId ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('contractId')} value={paymentForm.contractId} onChange={(e) => setPaymentForm((p) => ({ ...p, contractId: e.target.value }))} />
        <input className={fieldErrors?.amount ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('amount')} value={paymentForm.amount} onChange={(e) => setPaymentForm((p) => ({ ...p, amount: e.target.value }))} />
        <input className={fieldErrors?.receivedDate ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('dateFormatHint')} value={paymentForm.receivedDate} onChange={(e) => setPaymentForm((p) => ({ ...p, receivedDate: e.target.value }))} />
        <select className={fieldErrors?.method ? 'tool-input input-invalid' : 'tool-input'} value={paymentForm.method} onChange={(e) => setPaymentForm((p) => ({ ...p, method: e.target.value }))}>
          <option value="">{t('selectPlaceholder')}</option>
          {PAYMENT_METHOD_OPTIONS.map((method) => <option key={method} value={method}>{translateMethod(t, method)}</option>)}
        </select>
        <select className={fieldErrors?.status ? 'tool-input input-invalid' : 'tool-input'} value={paymentForm.status} onChange={(e) => setPaymentForm((p) => ({ ...p, status: e.target.value }))}>
          <option value="">{t('selectPlaceholder')}</option>
          {PAYMENT_STATUS_OPTIONS.map((status) => <option key={status} value={status}>{translateStatus(t, status)}</option>)}
        </select>
        <input className={fieldErrors?.remark ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('remark')} value={paymentForm.remark} onChange={(e) => setPaymentForm((p) => ({ ...p, remark: e.target.value }))} />
        <button className="mini-btn" disabled={!canWrite} onClick={savePayment}>{paymentForm.id ? t('save') : t('create')}</button>
        <button className="mini-btn" onClick={() => setPaymentForm({ id: '', contractId: '', amount: '', receivedDate: '', method: '', status: '', remark: '' })}>{t('reset')}</button>
      </div>
      {(fieldErrors?.contractId || fieldErrors?.amount || fieldErrors?.receivedDate || fieldErrors?.method || fieldErrors?.status || fieldErrors?.remark) && (
        <div className="field-error" style={{ marginBottom: 8 }}>
          {fieldErrors?.contractId || fieldErrors?.amount || fieldErrors?.receivedDate || fieldErrors?.method || fieldErrors?.status || fieldErrors?.remark}
        </div>
      )}
      {formError && <div className="field-error" style={{ marginBottom: 8 }}>{formError}</div>}
      <div className="inline-tools" style={{ marginBottom: 10 }}>
        <select className="tool-input" value={paymentStatus} onChange={(e) => setPaymentStatus(e.target.value)}>
          <option value="">{t('allStatuses')}</option>
          {PAYMENT_STATUS_OPTIONS.map((status) => <option key={status} value={status}>{translateStatus(t, status)}</option>)}
        </select>
        <button className="mini-btn" onClick={() => onPageChange(1)}>{t('search')}</button>
        <button className="mini-btn" onClick={() => reload(page)}>{t('refresh')}</button>
      </div>
      <div className="table-row table-head-row">
        <span>{t('contractId')}</span>
        <button className="table-head-btn" onClick={() => toggleSort('amount')}>{t('amount')}</button>
        <button className="table-head-btn" onClick={() => toggleSort('date')}>{t('receivedDate')}</button>
        <span>{t('status')}</span>
        <span>{t('action')}</span>
      </div>
      <ListState loading={loading} empty={!loading && rows.length === 0} emptyText={t('noData')} />
      {!loading && rows.map((p) => <div key={p.id} className="table-row"><span>{p.contractId}</span><span>{formatMoney(p.amount)}</span><span>{p.receivedDate || '-'}</span><span>{translateStatus(t, p.status)}</span><span><button className="mini-btn" onClick={() => setDetail(p)}>{t('detail')}</button><button className="mini-btn" onClick={() => editPayment(p)}>{t('save')}</button>{canDeleteCustomer ? <button className="danger-btn" onClick={() => removePayment(p.id)}>{t('delete')}</button> : null}</span></div>)}
      {!loading && rows.length > 0 && <ServerPager t={t} page={page} totalPages={totalPages} size={pagination?.size || 8} onPageChange={onPageChange} onSizeChange={onSizeChange} />}
      <RowDetailDrawer open={!!detail} title={t('payments')} t={t} onClose={() => setDetail(null)} rows={[
        { label: t('idLabel'), value: detail?.id },
        { label: t('contractId'), value: detail?.contractId },
        { label: t('amount'), value: detail ? formatMoney(detail.amount) : '-' },
        { label: t('receivedDate'), value: detail?.receivedDate },
        { label: t('method'), value: translateMethod(t, detail?.method) },
        { label: t('status'), value: translateStatus(t, detail?.status) },
        { label: t('remark'), value: detail?.remark },
      ]} />
    </section>
  )
}

export default PaymentsPanel
