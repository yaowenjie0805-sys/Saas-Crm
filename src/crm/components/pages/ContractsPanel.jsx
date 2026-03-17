import { memo, useMemo, useState } from 'react'
import { api, CONTRACT_STATUS_OPTIONS, formatMoney, translateStatus } from '../../shared'
import ListState from '../ListState'
import RowDetailDrawer from '../RowDetailDrawer'
import ServerPager from '../ServerPager'
import { useBatchActions } from '../useBatchActions'
import BatchResultModal from '../BatchResultModal'
import VirtualListTable from '../VirtualListTable'
import { useSelectionSet } from '../../hooks/useSelectionSet'

const ContractRow = memo(function ContractRow({ row, checked, onToggle, t, setDetail, editContract, canDeleteCustomer, removeContract }) {
  return (
    <div className="table-row table-row-6">
      <span><input type="checkbox" checked={checked} onChange={onToggle} /></span>
      <span>{row.contractNo}</span>
      <span>{row.title}</span>
      <span>{formatMoney(row.amount)}</span>
      <span>{row.signDate || translateStatus(t, row.status)}</span>
      <span>
        <button className="mini-btn" onClick={() => setDetail(row)}>{t('detail')}</button>
        <button className="mini-btn" onClick={() => editContract(row)}>{t('save')}</button>
        {canDeleteCustomer ? <button className="danger-btn" onClick={() => removeContract(row.id)}>{t('delete')}</button> : null}
      </span>
    </div>
  )
})

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
  onRefresh,
  apiContext,
}) {
  const [sortBy, setSortBy] = useState('amountDesc')
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
    const sorted = [...(contracts || [])]
    if (sortBy === 'amountDesc') sorted.sort((a, b) => Number(b.amount || 0) - Number(a.amount || 0))
    if (sortBy === 'amountAsc') sorted.sort((a, b) => Number(a.amount || 0) - Number(b.amount || 0))
    if (sortBy === 'signDateDesc') sorted.sort((a, b) => Date.parse(b.signDate || '') - Date.parse(a.signDate || ''))
    if (sortBy === 'signDateAsc') sorted.sort((a, b) => Date.parse(a.signDate || '') - Date.parse(b.signDate || ''))
    return sorted
  }, [contracts, sortBy])

  const page = pagination?.page || 1
  const totalPages = Math.max(1, pagination?.totalPages || 1)
  const selection = useSelectionSet(rows, (row) => row.id)
  const { selectedIds, selectedCount, allChecked, clearSelection, selectPage, toggleAll, toggleOne } = selection
  const byId = useMemo(() => new Map((contracts || []).map((row) => [row.id, row])), [contracts])

  const updateOne = async (id, patch) => {
    const row = byId.get(id)
    if (!row) return
    const payload = {
      customerId: String(row.customerId || '').trim(),
      contractNo: String(row.contractNo || '').trim(),
      title: String(row.title || '').trim(),
      amount: Number(row.amount || 0),
      status: String(patch.status ?? row.status ?? '').trim(),
      signDate: String(row.signDate || '').trim(),
    }
    await api('/contracts/' + id, { method: 'PATCH', body: JSON.stringify(payload) }, token, lang)
  }

  const batchDelete = async () => {
    if (!canDeleteCustomer) return
    const result = await runBatch({
      ids: [...selectedIds],
      worker: (id) => api('/contracts/' + id, { method: 'DELETE' }, token, lang),
      batch: { path: '/v1/contracts/batch-actions', action: 'DELETE', token, lang },
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
      batch: { path: '/v1/contracts/batch-actions', action: 'UPDATE_STATUS', payload: { status: batchStatus }, token, lang },
    })
    if (result?.failed > 0) setBatchModalOpen(true)
    clearSelection()
    await refreshSelf()
  }

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
      <div className="inline-tools filter-row" style={{ marginBottom: 8 }}>
        <input className="tool-input" placeholder={t('search')} value={contractQ} onChange={(e) => setContractQ(e.target.value)} />
        <select className="tool-input" value={contractStatus} onChange={(e) => setContractStatus(e.target.value)}>
          <option value="">{t('allStatuses')}</option>
          {CONTRACT_STATUS_OPTIONS.map((status) => <option key={status} value={status}>{translateStatus(t, status)}</option>)}
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
          {CONTRACT_STATUS_OPTIONS.map((status) => <option key={status} value={status}>{translateStatus(t, status)}</option>)}
        </select>
        <button className="mini-btn" disabled={!canWrite} onClick={batchChangeStatus}>{t('batchSetStatus')}</button>
        {canDeleteCustomer && <button className="danger-btn" onClick={batchDelete}>{t('batchDelete')}</button>}
        {batchSummary?.failed > 0 && <button className="mini-btn" onClick={() => setBatchModalOpen(true)}>{t('batchResultTitle')}</button>}
      </div>
      {batchMessage && <div className="info-banner" style={{ marginBottom: 8 }}>{batchMessage}</div>}
      <div className="table-row table-head-row table-row-6">
        <span><input type="checkbox" checked={allChecked} onChange={(e) => toggleAll(e.target.checked)} /></span>
        <span>{t('contractNo')}</span>
        <span>{t('title')}</span>
        <button className="table-head-btn" onClick={() => toggleSort('amount')}>{t('amount')}</button>
        <button className="table-head-btn" onClick={() => toggleSort('signDate')}>{t('signDate')}</button>
        <span>{t('action')}</span>
      </div>
      <ListState loading={loading} empty={!loading && rows.length === 0} emptyText={t('noData')} />
      {!loading && rows.length > 0 && (
        <VirtualListTable
          rows={rows}
          viewportHeight={460}
          getRowKey={(row) => row.id}
          renderRow={(row) => (
            <ContractRow
              key={row.id}
              row={row}
              checked={selectedIds.has(row.id)}
              onToggle={(e) => toggleOne(row.id, e.target.checked)}
              t={t}
              setDetail={setDetail}
              editContract={editContract}
              canDeleteCustomer={canDeleteCustomer}
              removeContract={removeContract}
            />
          )}
        />
      )}
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
      <BatchResultModal t={t} open={batchModalOpen} summary={batchSummary} onClose={() => { setBatchModalOpen(false); clearSummary() }} />
    </section>
  )
}

export default memo(ContractsPanel)
