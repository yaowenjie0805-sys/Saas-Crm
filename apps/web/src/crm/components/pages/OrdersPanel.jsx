import { lazy, memo, Suspense, useCallback, useEffect, useMemo, useState } from 'react'
import { api, formatMoney, translateStatus } from '../../shared'
import ListState from '../ListState'
import { useBatchActions } from '../useBatchActions'
import BatchResultModal from '../BatchResultModal'
import VirtualListTable from '../VirtualListTable'
import { useSelectionSet } from '../../hooks/useSelectionSet'

const EMPTY_FORM = { id: '', customerId: '', opportunityId: '', quoteId: '', owner: '', amount: '0', signDate: '' }
const EMPTY_ROWS = []
const OrderEditorModal = lazy(() => import('./orders/OrderEditorModal'))

const OrderRow = memo(function OrderRow({ row, checked, onToggle, t, openEdit, act, actionPending, canWrite }) {
  return (
    <div className="table-row table-row-6">
      <span><input type="checkbox" checked={checked} onChange={onToggle} /></span>
      <span>{row.orderNo}</span>
      <span>{row.owner}</span>
      <span>{translateStatus(t, row.status)}</span>
      <span>{formatMoney(row.amount)}</span>
      <span>
        <button className="mini-btn" onClick={() => openEdit(row)}>{t('detail')}</button>
        <button className="mini-btn" disabled={!canWrite || actionPending} onClick={() => act(row.id, 'confirm')}>{t('confirm')}</button>
        <button className="mini-btn" disabled={!canWrite || actionPending} onClick={() => act(row.id, 'fulfill')}>{t('fulfill')}</button>
        <button className="mini-btn" disabled={!canWrite || actionPending} onClick={() => act(row.id, 'complete')}>{t('complete')}</button>
        <button className="mini-btn" disabled={!canWrite || actionPending} onClick={() => act(row.id, 'to-contract')}>{t('toContract')}</button>
        <button className="mini-btn" disabled={!canWrite || actionPending} onClick={() => act(row.id, 'cancel')}>{t('cancel')}</button>
      </span>
    </div>
  )
})

function OrdersPanel({ activePage, t, canWrite, apiContext, opportunityFilter, refreshPage, commerce }) {
  const orders = commerce?.orders || {}
  const items = orders.items ?? EMPTY_ROWS
  const loading = !!orders.loading
  const error = orders.error || ''
  const setError = orders.setError || (() => {})
  const statusFilter = orders.statusFilter || ''
  const setStatusFilter = orders.setStatusFilter || (() => {})
  const ownerFilter = orders.ownerFilter || ''
  const setOwnerFilter = orders.setOwnerFilter || (() => {})
  const lastLoadedAt = orders.lastLoadedAt || 0
  const stale = !!orders.stale
  const [openModal, setOpenModal] = useState(false)
  const [form, setForm] = useState(EMPTY_FORM)
  const [batchOwner, setBatchOwner] = useState('')
  const [batchStatus, setBatchStatus] = useState('')
  const [batchModalOpen, setBatchModalOpen] = useState(false)
  const [approvalMode, setApprovalMode] = useState('STRICT')
  const [actionPendingById, setActionPendingById] = useState({})

  const token = apiContext?.token
  const lang = apiContext?.lang || 'en'
  const { summary: batchSummary, toastMessage: batchMessage, runBatch, clearSummary } = useBatchActions({ t })
  const refreshSelf = useCallback(async () => {
    await refreshPage('orders', 'panel_action')
  }, [refreshPage])
  const loadApprovalMode = useCallback(async () => {
    if (!token) return
    try {
      const cfg = await api('/v2/tenant-config', {}, token, lang)
      setApprovalMode(String(cfg?.approvalMode || 'STRICT').toUpperCase() === 'STAGE_GATE' ? 'STAGE_GATE' : 'STRICT')
    } catch {
      setApprovalMode('STRICT')
    }
  }, [lang, token])

  useEffect(() => {
    if (activePage !== 'orders') return
    loadApprovalMode()
  }, [activePage, loadApprovalMode])

  const filteredItems = useMemo(() => {
    const owner = ownerFilter.trim().toLowerCase()
    return (items || []).filter((row) => {
      if (statusFilter && String(row.status || '') !== statusFilter) return false
      if (owner && !String(row.owner || '').toLowerCase().includes(owner)) return false
      return true
    })
  }, [items, statusFilter, ownerFilter])
  const selection = useSelectionSet(filteredItems, (row) => row.id)
  const { selectedIds, selectedCount, allChecked, clearSelection, selectPage, toggleAll, toggleOne } = selection
  const byId = useMemo(() => new Map((items || []).map((row) => [row.id, row])), [items])

  const patchOne = async (id, patch) => {
    const row = byId.get(id)
    if (!row) return
    const payload = {
      id: row.id,
      customerId: row.customerId || '',
      opportunityId: row.opportunityId || '',
      quoteId: row.quoteId || '',
      owner: patch.owner ?? row.owner ?? '',
      amount: Number(row.amount || 0),
      signDate: row.signDate || '',
      status: patch.status ?? row.status ?? '',
    }
    await api('/v1/orders', { method: 'PATCH', body: JSON.stringify(payload) }, token, lang)
  }

  const batchAssign = async () => {
    if (!batchOwner.trim()) return
    const result = await runBatch({
      ids: [...selectedIds],
      worker: (id) => patchOne(id, { owner: batchOwner.trim() }),
      batch: { path: '/v1/orders/batch-actions', action: 'ASSIGN_OWNER', payload: { owner: batchOwner.trim() }, token, lang },
    })
    if (result?.failed > 0) setBatchModalOpen(true)
    clearSelection()
    await refreshSelf()
  }

  const batchChangeStatus = async () => {
    if (!batchStatus) return
    const result = await runBatch({
      ids: [...selectedIds],
      worker: (id) => patchOne(id, { status: batchStatus }),
      batch: { path: '/v1/orders/batch-actions', action: 'UPDATE_STATUS', payload: { status: batchStatus }, token, lang },
    })
    if (result?.failed > 0) setBatchModalOpen(true)
    clearSelection()
    await refreshSelf()
  }

  const resetFilters = () => {
    setStatusFilter('')
    setOwnerFilter('')
  }

  const openCreate = () => {
    setForm({ ...EMPTY_FORM, opportunityId: opportunityFilter || '' })
    setOpenModal(true)
  }

  const openEdit = (row) => {
    setForm({ id: row.id, customerId: row.customerId || '', opportunityId: row.opportunityId || '', quoteId: row.quoteId || '', owner: row.owner || '', amount: String(row.amount || 0), signDate: row.signDate || '' })
    setOpenModal(true)
  }

  const save = async () => {
    if (!canWrite) return
    try {
      const payload = { ...form, amount: Number(form.amount || 0) }
      if (form.id) await api('/v1/orders', { method: 'PATCH', body: JSON.stringify(payload) }, token, lang)
      else await api('/v1/orders', { method: 'POST', body: JSON.stringify(payload) }, token, lang)
      setOpenModal(false)
      setForm(EMPTY_FORM)
      await refreshSelf()
    } catch (err) {
      setError(err.requestId ? `${err.message} [${err.requestId}]` : err.message)
    }
  }

  const renderActionError = useCallback((err) => {
    if (err?.code === 'order_stage_gate_requires_quote_accepted') {
      const fallback = t('orderStageGateQuoteAcceptedRequired')
      const req = err?.details?.requiredStatus ? ` (${t('requiredStatusLabel')}: ${err.details.requiredStatus})` : ''
      return `${fallback}${req}`
    }
    if (err?.code === 'order_stage_gate_requires_fulfilling') {
      const fallback = t('orderStageGateFulfillingRequired')
      const req = err?.details?.requiredStatus ? ` (${t('requiredStatusLabel')}: ${err.details.requiredStatus})` : ''
      return `${fallback}${req}`
    }
    if (err?.code === 'order_status_transition_invalid') return t('orderStatusConflict')
    return err?.message || t('loadFailed')
  }, [t])

  const act = async (id, action) => {
    if (actionPendingById[id]) return
    setActionPendingById((prev) => ({ ...prev, [id]: true }))
    try {
      await api(`/v1/orders/${id}/${action}`, { method: 'POST' }, token, lang)
      await refreshSelf()
    } catch (err) {
      const message = renderActionError(err)
      setError(err.requestId ? `${message} [${err.requestId}]` : message)
      await refreshSelf()
    } finally {
      setActionPendingById((prev) => {
        const next = { ...prev }
        delete next[id]
        return next
      })
    }
  }

  if (activePage !== 'orders') return null

  return (
    <section className="panel">
      <div className="panel-head">
        <h2>{t('orders')}</h2>
        <div className="inline-tools">
          <span className="muted-filter">{t('approvalModeLabel')}: {approvalMode === 'STAGE_GATE' ? t('approvalModeStageGate') : t('approvalModeStrict')}</span>
          <button className="mini-btn" onClick={refreshSelf}>{t('refresh')}</button>
          <button className="primary-btn" disabled={!canWrite} onClick={openCreate}>{t('createOrder')}</button>
        </div>
      </div>
      <div className="filter-row" style={{ marginBottom: 8 }}>
        <input
          className="tool-input"
          placeholder={t('owner')}
          value={ownerFilter}
          onChange={(e) => setOwnerFilter(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') refreshSelf()
          }}
        />
        <select className="tool-input" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">{t('allStatuses')}</option>
          <option value="DRAFT">{translateStatus(t, 'DRAFT')}</option>
          <option value="CONFIRMED">{translateStatus(t, 'CONFIRMED')}</option>
          <option value="FULFILLING">{translateStatus(t, 'FULFILLING')}</option>
          <option value="COMPLETED">{translateStatus(t, 'COMPLETED')}</option>
          <option value="CANCELED">{translateStatus(t, 'CANCELED')}</option>
        </select>
      </div>
      <div className="filter-bar">
        <button className="mini-btn" onClick={refreshSelf}>{t('query')}</button>
        <button className="mini-btn" onClick={resetFilters}>{t('clearFilters')}</button>
      </div>
      <div className="filter-bar" style={{ marginBottom: 8 }}>
        <span className="muted-filter">{t('batchSelectedCount')}: {selectedCount}</span>
        <button className="mini-btn" onClick={selectPage}>{t('selectPage')}</button>
        <button className="mini-btn" onClick={clearSelection}>{t('clearSelection')}</button>
        <input className="tool-input" placeholder={t('batchOwnerPlaceholder')} value={batchOwner} onChange={(e) => setBatchOwner(e.target.value)} />
        <select className="tool-input" value={batchStatus} onChange={(e) => setBatchStatus(e.target.value)}>
          <option value="">{t('batchSetStatus')}</option>
          <option value="DRAFT">{translateStatus(t, 'DRAFT')}</option>
          <option value="CONFIRMED">{translateStatus(t, 'CONFIRMED')}</option>
          <option value="FULFILLING">{translateStatus(t, 'FULFILLING')}</option>
          <option value="COMPLETED">{translateStatus(t, 'COMPLETED')}</option>
          <option value="CANCELED">{translateStatus(t, 'CANCELED')}</option>
        </select>
        <button className="mini-btn" disabled={!canWrite} onClick={batchAssign}>{t('batchAssignOwner')}</button>
        <button className="mini-btn" disabled={!canWrite} onClick={batchChangeStatus}>{t('batchSetStatus')}</button>
        {batchSummary?.failed > 0 && <button className="mini-btn" onClick={() => setBatchModalOpen(true)}>{t('batchResultTitle')}</button>}
      </div>
      {batchMessage && <div className="info-banner" style={{ marginBottom: 8 }}>{batchMessage}</div>}
      {error && <div className="field-error" style={{ marginBottom: 8 }}>{error}</div>}
      {!!lastLoadedAt && stale && <div className="info-banner" style={{ marginBottom: 8 }}>{t('loading')}</div>}
      {!loading && opportunityFilter && filteredItems.length === 0 && (
        <div className="empty-tip" style={{ marginBottom: 8 }}>{t('filteredResultEmpty')}</div>
      )}
      <div className="table-row table-head-row table-row-6"><span><input type="checkbox" checked={allChecked} onChange={(e) => toggleAll(e.target.checked)} /></span><span>{t('idLabel')}</span><span>{t('owner')}</span><span>{t('status')}</span><span>{t('amount')}</span><span>{t('action')}</span></div>
      <ListState loading={loading} empty={!loading && filteredItems.length === 0} emptyText={t('noData')} />
      {!loading && filteredItems.length > 0 && (
        <VirtualListTable
          rows={filteredItems}
          viewportHeight={460}
          getRowKey={(row) => row.id}
          renderRow={(row) => (
            <OrderRow
              key={row.id}
              row={row}
              checked={selectedIds.has(row.id)}
              onToggle={(e) => toggleOne(row.id, e.target.checked)}
              t={t}
              openEdit={openEdit}
              act={act}
              actionPending={!!actionPendingById[row.id]}
              canWrite={canWrite}
            />
          )}
        />
      )}

      {openModal && (
        <Suspense fallback={<div className="loading">{t('loading')}</div>}>
          <OrderEditorModal
            t={t}
            canWrite={canWrite}
            form={form}
            setForm={setForm}
            setOpenModal={setOpenModal}
            save={save}
          />
        </Suspense>
      )}
      <BatchResultModal t={t} open={batchModalOpen} summary={batchSummary} onClose={() => { setBatchModalOpen(false); clearSummary() }} />
    </section>
  )
}

export default memo(OrdersPanel)
