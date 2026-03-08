import { useMemo, useState } from 'react'
import { CONTRACT_STATUS_OPTIONS, formatMoney, translateStatus } from '../../shared'
import ListState from '../ListState'
import RowDetailDrawer from '../RowDetailDrawer'
import ServerPager from '../ServerPager'

function ContractsPanel({
  activePage,
  t,
  canWrite,
  canDeleteCustomer,
  contractForm,
  setContractForm,
  saveContract,
  formError,
  fieldErrors,
  contracts,
  editContract,
  removeContract,
  loading,
  contractQ,
  setContractQ,
  contractStatus,
  setContractStatus,
  pagination,
  onPageChange,
  onSizeChange,
  reload,
}) {
  const [sortBy, setSortBy] = useState('amountDesc')
  const [detail, setDetail] = useState(null)

  const toggleSort = (key) => {
    setSortBy((prev) => {
      if (prev === `${key}Asc`) return `${key}Desc`
      return `${key}Asc`
    })
  }

  const rows = useMemo(() => {
    const sorted = [...(contracts || [])]
    if (sortBy === 'amountDesc') sorted.sort((a, b) => Number(b.amount || 0) - Number(a.amount || 0))
    if (sortBy === 'amountAsc') sorted.sort((a, b) => Number(a.amount || 0) - Number(b.amount || 0))
    if (sortBy === 'signDateDesc') sorted.sort((a, b) => Date.parse(b.signDate || '') - Date.parse(a.signDate || ''))
    if (sortBy === 'signDateAsc') sorted.sort((a, b) => Date.parse(a.signDate || '') - Date.parse(b.signDate || ''))
    return sorted
  }, [contracts, sortBy])

  const page = pagination?.page || 1
  const totalPages = Math.max(1, pagination?.totalPages || 1)

  if (activePage !== 'contracts') return null

  return (
    <section className="panel">
      <div className="panel-head"><h2>{t('contracts')}</h2></div>
      <div className="inline-tools" style={{ marginBottom: 10 }}>
        <input className={fieldErrors?.customerId ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('customerId')} value={contractForm.customerId} onChange={(e) => setContractForm((p) => ({ ...p, customerId: e.target.value }))} />
        <input className={fieldErrors?.contractNo ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('contractNo')} value={contractForm.contractNo} onChange={(e) => setContractForm((p) => ({ ...p, contractNo: e.target.value }))} />
        <input className={fieldErrors?.title ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('title')} value={contractForm.title} onChange={(e) => setContractForm((p) => ({ ...p, title: e.target.value }))} />
        <input className={fieldErrors?.amount ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('amount')} value={contractForm.amount} onChange={(e) => setContractForm((p) => ({ ...p, amount: e.target.value }))} />
        <select className={fieldErrors?.status ? 'tool-input input-invalid' : 'tool-input'} value={contractForm.status} onChange={(e) => setContractForm((p) => ({ ...p, status: e.target.value }))}>
          <option value="">{t('selectPlaceholder')}</option>
          {CONTRACT_STATUS_OPTIONS.map((status) => <option key={status} value={status}>{translateStatus(t, status)}</option>)}
        </select>
        <input className={fieldErrors?.signDate ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('dateFormatHint')} value={contractForm.signDate} onChange={(e) => setContractForm((p) => ({ ...p, signDate: e.target.value }))} />
        <button className="mini-btn" disabled={!canWrite} onClick={saveContract}>{contractForm.id ? t('save') : t('create')}</button>
        <button className="mini-btn" onClick={() => setContractForm({ id: '', customerId: '', contractNo: '', title: '', amount: '', status: '', signDate: '' })}>{t('reset')}</button>
      </div>
      {(fieldErrors?.customerId || fieldErrors?.contractNo || fieldErrors?.title || fieldErrors?.amount || fieldErrors?.status || fieldErrors?.signDate) && (
        <div className="field-error" style={{ marginBottom: 8 }}>
          {fieldErrors?.customerId || fieldErrors?.contractNo || fieldErrors?.title || fieldErrors?.amount || fieldErrors?.status || fieldErrors?.signDate}
        </div>
      )}
      {formError && <div className="field-error" style={{ marginBottom: 8 }}>{formError}</div>}
      <div className="inline-tools" style={{ marginBottom: 10 }}>
        <input className="tool-input" placeholder={t('search')} value={contractQ} onChange={(e) => setContractQ(e.target.value)} />
        <select className="tool-input" value={contractStatus} onChange={(e) => setContractStatus(e.target.value)}>
          <option value="">{t('allStatuses')}</option>
          {CONTRACT_STATUS_OPTIONS.map((status) => <option key={status} value={status}>{translateStatus(t, status)}</option>)}
        </select>
        <button className="mini-btn" onClick={() => onPageChange(1)}>{t('search')}</button>
        <button className="mini-btn" onClick={() => reload(page)}>{t('refresh')}</button>
      </div>
      <div className="table-row table-head-row">
        <span>{t('contractNo')}</span>
        <span>{t('title')}</span>
        <button className="table-head-btn" onClick={() => toggleSort('amount')}>{t('amount')}</button>
        <button className="table-head-btn" onClick={() => toggleSort('signDate')}>{t('signDate')}</button>
        <span>{t('action')}</span>
      </div>
      <ListState loading={loading} empty={!loading && rows.length === 0} emptyText={t('noData')} />
      {!loading && rows.map((c) => <div key={c.id} className="table-row"><span>{c.contractNo}</span><span>{c.title}</span><span>{formatMoney(c.amount)}</span><span>{c.signDate || translateStatus(t, c.status)}</span><span><button className="mini-btn" onClick={() => setDetail(c)}>{t('detail')}</button><button className="mini-btn" onClick={() => editContract(c)}>{t('save')}</button>{canDeleteCustomer ? <button className="danger-btn" onClick={() => removeContract(c.id)}>{t('delete')}</button> : null}</span></div>)}
      {!loading && rows.length > 0 && <ServerPager t={t} page={page} totalPages={totalPages} size={pagination?.size || 8} onPageChange={onPageChange} onSizeChange={onSizeChange} />}
      <RowDetailDrawer open={!!detail} title={t('contracts')} t={t} onClose={() => setDetail(null)} rows={[
        { label: t('idLabel'), value: detail?.id },
        { label: t('customerId'), value: detail?.customerId },
        { label: t('contractNo'), value: detail?.contractNo },
        { label: t('title'), value: detail?.title },
        { label: t('amount'), value: detail ? formatMoney(detail.amount) : '-' },
        { label: t('status'), value: translateStatus(t, detail?.status) },
        { label: t('signDate'), value: detail?.signDate },
      ]} />
    </section>
  )
}

export default ContractsPanel
