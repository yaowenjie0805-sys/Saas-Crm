import { memo, useMemo, useState } from 'react'
import { api, formatMoney, PAYMENT_METHOD_OPTIONS, PAYMENT_STATUS_OPTIONS, translateMethod, translateStatus } from '../../shared'
import ListState from '../ListState'
import RowDetailDrawer from '../RowDetailDrawer'
import ServerPager from '../ServerPager'
import { useBatchActions } from '../useBatchActions'
import BatchResultModal from '../BatchResultModal'
import VirtualListTable from '../VirtualListTable'
import { useSelectionSet } from '../../hooks/useSelectionSet'

const PaymentRow = memo(function PaymentRow({ row, checked, onToggle, t, setDetail, editPayment, canDeleteCustomer, removePayment }) {
  return (
    <div className="table-row table-row-6">
      <span><input type="checkbox" checked={checked} onChange={onToggle} /></span>
      <span>{row.contractId}</span>
      <span>{formatMoney(row.amount)}</span>
      <span>{row.receivedDate || '-'}</span>
      <span>{translateStatus(t, row.status)}</span>
      <span>
        <button className="mini-btn" onClick={() => setDetail(row)}>{t('detail')}</button>
        <button className="mini-btn" onClick={() => editPayment(row)}>{t('save')}</button>
        {canDeleteCustomer ? <button className="danger-btn" onClick={() => removePayment(row.id)}>{t('delete')}</button> : null}
      </span>
    </div>
  )
})

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
  onRefresh,
  apiContext,
}) {
  const [sortBy, setSortBy] = useState('dateDesc')
  const [detail, setDetail] = useState(null)
  const [batchStatus, setBatchStatus] = useState('')
  const [batchModalOpen, setBatchModalOpen] = useState(false)
  const token = apiContext?.token
  const lang = apiContext?.lang || 'en'
  const { summary: batchSummary, toastMessage: batchMessage, runBatch, clearSummary } = useBatchActions({ t })
  const refreshSelf = onRefresh

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
  const selection = useSelectionSet(rows, (row) => row.id)
  const { selectedIds, selectedCount, allChecked, clearSelection, selectPage, toggleAll, toggleOne } = selection
  const byId = useMemo(() => new Map((payments || []).map((row) => [row.id, row])), [payments])

  const updateOne = async (id, patch) => {
    const row = byId.get(id)
    if (!row) return
    const payload = {
      contractId: String(row.contractId || '').trim(),
      amount: Number(row.amount || 0),
      receivedDate: String(row.receivedDate || '').trim(),
      method: String(row.method || '').trim(),
      status: String(patch.status ?? row.status ?? '').trim(),
      remark: String(row.remark || '').trim(),
    }
    await api('/payments/' + id, { method: 'PATCH', body: JSON.stringify(payload) }, token, lang)
  }

  const batchDelete = async () => {
    if (!canDeleteCustomer) return
    const result = await runBatch({
      ids: [...selectedIds],
      worker: (id) => api('/payments/' + id, { method: 'DELETE' }, token, lang),
      batch: { path: '/v1/payments/batch-actions', action: 'DELETE', token, lang },
      canRun: canDeleteCustomer,
    })
    if (result?.failed > 0) setBatchModalOpen(true)
    clearSelection()
    await refreshSelf()
  }

  const batchChangeStatus = async () => {
    if (!batchStatus) return
    const result = await runBatch({
      ids: [...selectedIds],
      worker: (id) => updateOne(id, { status: batchStatus }),
      batch: { path: '/v1/payments/batch-actions', action: 'UPDATE_STATUS', payload: { status: batchStatus }, token, lang },
    })
    if (result?.failed > 0) setBatchModalOpen(true)
    clearSelection()
    await refreshSelf()
  }

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
      <div className="inline-tools filter-row" style={{ marginBottom: 8 }}>
        <select className="tool-input" value={paymentStatus} onChange={(e) => setPaymentStatus(e.target.value)}>
          <option value="">{t('allStatuses')}</option>
          {PAYMENT_STATUS_OPTIONS.map((status) => <option key={status} value={status}>{translateStatus(t, status)}</option>)}
        </select>
      </div>
      <div className="inline-tools filter-bar" style={{ marginBottom: 10 }}>
        <button className="mini-btn" onClick={() => onPageChange(1)}>{t('search')}</button>
        <button className="mini-btn" onClick={refreshSelf}>{t('refresh')}</button>
      </div>
      <div className="inline-tools filter-bar" style={{ marginBottom: 10 }}>
        <span className="muted-filter">{t('batchSelectedCount')}: {selectedCount}</span>
        <button className="mini-btn" onClick={selectPage}>{t('selectPage')}</button>
        <button className="mini-btn" onClick={clearSelection}>{t('clearSelection')}</button>
        <select className="tool-input" value={batchStatus} onChange={(e) => setBatchStatus(e.target.value)}>
          <option value="">{t('batchSetStatus')}</option>
          {PAYMENT_STATUS_OPTIONS.map((status) => <option key={status} value={status}>{translateStatus(t, status)}</option>)}
        </select>
        <button className="mini-btn" disabled={!canWrite} onClick={batchChangeStatus}>{t('batchSetStatus')}</button>
        {canDeleteCustomer && <button className="danger-btn" onClick={batchDelete}>{t('batchDelete')}</button>}
        {batchSummary?.failed > 0 && <button className="mini-btn" onClick={() => setBatchModalOpen(true)}>{t('batchResultTitle')}</button>}
      </div>
      {batchMessage && <div className="info-banner" style={{ marginBottom: 8 }}>{batchMessage}</div>}
      <div className="table-row table-head-row table-row-6">
        <span><input type="checkbox" checked={allChecked} onChange={(e) => toggleAll(e.target.checked)} /></span>
        <span>{t('contractId')}</span>
        <button className="table-head-btn" onClick={() => toggleSort('amount')}>{t('amount')}</button>
        <button className="table-head-btn" onClick={() => toggleSort('date')}>{t('receivedDate')}</button>
        <span>{t('status')}</span>
        <span>{t('action')}</span>
      </div>
      <ListState loading={loading} empty={!loading && rows.length === 0} emptyText={t('noData')} />
      {!loading && rows.length > 0 && (
        <VirtualListTable
          rows={rows}
          viewportHeight={460}
          getRowKey={(row) => row.id}
          renderRow={(row) => (
            <PaymentRow
              key={row.id}
              row={row}
              checked={selectedIds.has(row.id)}
              onToggle={(e) => toggleOne(row.id, e.target.checked)}
              t={t}
              setDetail={setDetail}
              editPayment={editPayment}
              canDeleteCustomer={canDeleteCustomer}
              removePayment={removePayment}
            />
          )}
        />
      )}
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
      <BatchResultModal t={t} open={batchModalOpen} summary={batchSummary} onClose={() => { setBatchModalOpen(false); clearSummary() }} />
    </section>
  )
}

export default memo(PaymentsPanel)
