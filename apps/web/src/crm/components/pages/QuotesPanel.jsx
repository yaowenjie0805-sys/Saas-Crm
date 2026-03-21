import { lazy, memo, Suspense, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { api, translateStatus } from '../../shared'
import ListState from '../ListState'
import { useBatchActions } from '../useBatchActions'
import BatchResultModal from '../BatchResultModal'
import VirtualListTable from '../VirtualListTable'
import { useSelectionSet } from '../../hooks/useSelectionSet'
import { QuoteRow } from './quotes/QuotePanelRows'
import { EMPTY_FORM, EMPTY_ROWS, formatRequestError, getQuoteStatusOptions, NOOP } from './quotes/quotePanelHelpers'

const QuoteItemsSection = lazy(() => import('./quotes/QuoteItemsSection'))
const QuoteEditorModal = lazy(() => import('./quotes/QuoteEditorModal'))

function QuotesPanel({ activePage, t, canWrite, apiContext, opportunityFilter, prefill, onConsumePrefill, refreshPage, commerce }) {
  const quotes = commerce?.quotes || {}
  const items = quotes.items ?? EMPTY_ROWS
  const loading = !!quotes.loading
  const error = quotes.error || ''
  const setError = quotes.setError || NOOP
  const lastLoadedAt = quotes.lastLoadedAt || 0
  const stale = !!quotes.stale
  const statusFilter = quotes.statusFilter || ''
  const setStatusFilter = quotes.setStatusFilter || (() => {})
  const ownerFilter = quotes.ownerFilter || ''
  const setOwnerFilter = quotes.setOwnerFilter || (() => {})
  const [selectedQuoteId, setSelectedQuoteId] = useState('')
  const [quoteItems, setQuoteItems] = useState([])
  const [itemJson, setItemJson] = useState('[{"productId":"","quantity":1,"unitPrice":0,"discountRate":0,"taxRate":0}]')
  const [submitResult, setSubmitResult] = useState(null)
  const [orderResult, setOrderResult] = useState(null)
  const [tenantApprovalMode, setTenantApprovalMode] = useState('STRICT')
  const [openModal, setOpenModal] = useState(false)
  const [form, setForm] = useState(EMPTY_FORM)
  const [actionPendingIds, setActionPendingIds] = useState({})
  const [batchOwner, setBatchOwner] = useState('')
  const [batchStatus, setBatchStatus] = useState('')
  const [batchModalOpen, setBatchModalOpen] = useState(false)
  const itemAbortRef = useRef(null)
  const statusOptions = useMemo(() => getQuoteStatusOptions(t), [t])

  const token = apiContext?.token
  const lang = apiContext?.lang || 'en'
  const effectiveSelectedQuoteId = selectedQuoteId || items[0]?.id || ''
  const { summary: batchSummary, toastMessage: batchMessage, runBatch, clearSummary } = useBatchActions({ t })

  const refreshSelf = useCallback(async () => {
    await refreshPage('quotes', 'panel_action')
  }, [refreshPage])

  const loadQuoteItems = useCallback(async (quoteId = effectiveSelectedQuoteId) => {
    if (!token || !quoteId) { setQuoteItems([]); return }
    itemAbortRef.current?.abort()
    const controller = new AbortController()
    itemAbortRef.current = controller
    try {
      const data = await api(`/v1/quotes/${quoteId}/items`, { signal: controller.signal }, token, lang)
      setQuoteItems(data.items || [])
    } catch (err) {
      if (err?.name === 'AbortError') return
      setError(formatRequestError(err, t))
    }
  }, [token, lang, effectiveSelectedQuoteId, setError, t])
  useEffect(() => { if (activePage === 'quotes' && effectiveSelectedQuoteId) loadQuoteItems(effectiveSelectedQuoteId) }, [activePage, effectiveSelectedQuoteId, loadQuoteItems])
  useEffect(() => () => { itemAbortRef.current?.abort() }, [])
  useEffect(() => {
    let canceled = false
    if (activePage !== 'quotes' || !token) return () => { canceled = true }
    ;(async () => {
      try {
        const data = await api('/v2/tenant-config', {}, token, lang)
        if (!canceled) setTenantApprovalMode(String(data?.approvalMode || 'STRICT').toUpperCase())
      } catch {
        if (!canceled) setTenantApprovalMode('STRICT')
      }
    })()
    return () => { canceled = true }
  }, [activePage, token, lang])

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
      owner: patch.owner ?? row.owner ?? '',
      status: patch.status ?? row.status ?? '',
      validUntil: row.validUntil || '',
    }
    await api('/v1/quotes', { method: 'PATCH', body: JSON.stringify(payload) }, token, lang)
  }

  const batchAssign = async () => {
    if (!batchOwner.trim()) return
    const result = await runBatch({
      ids: [...selectedIds],
      worker: (id) => patchOne(id, { owner: batchOwner.trim() }),
      batch: { path: '/v1/quotes/batch-actions', action: 'ASSIGN_OWNER', payload: { owner: batchOwner.trim() }, token, lang },
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
      batch: { path: '/v1/quotes/batch-actions', action: 'UPDATE_STATUS', payload: { status: batchStatus }, token, lang },
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
    setForm({
      ...EMPTY_FORM,
      customerId: prefill?.customerId || '',
      opportunityId: prefill?.opportunityId || opportunityFilter || '',
      owner: prefill?.owner || '',
    })
    if (prefill && onConsumePrefill) onConsumePrefill()
    setOpenModal(true)
  }

  const openEdit = (row) => {
    setForm({ id: row.id, customerId: row.customerId || '', opportunityId: row.opportunityId || '', owner: row.owner || '', status: row.status || 'DRAFT', validUntil: row.validUntil || '' })
    setOpenModal(true)
  }

  const saveQuote = async () => {
    if (!canWrite) return
    try {
      if (form.id) await api('/v1/quotes', { method: 'PATCH', body: JSON.stringify(form) }, token, lang)
      else await api('/v1/quotes', { method: 'POST', body: JSON.stringify(form) }, token, lang)
      setOpenModal(false)
      setForm(EMPTY_FORM)
      await refreshSelf()
    } catch (err) {
      setError(formatRequestError(err, t))
    }
  }

  const saveItems = async () => {
    if (!canWrite || !effectiveSelectedQuoteId) return
    try {
      const rows = JSON.parse(itemJson)
      await api(`/v1/quotes/${effectiveSelectedQuoteId}/items`, { method: 'POST', body: JSON.stringify(rows) }, token, lang)
      await loadQuoteItems(effectiveSelectedQuoteId)
      await refreshSelf()
    } catch (err) {
      setError(formatRequestError(err, t))
    }
  }

  const act = async (id, action) => {
    if (actionPendingIds[id]) return
    setActionPendingIds((prev) => ({ ...prev, [id]: true }))
    try {
      const data = await api(`/v1/quotes/${id}/${action}`, { method: 'POST' }, token, lang)
      if (action === 'submit') setSubmitResult(data)
      if (action === 'to-order') setOrderResult(data)
      await refreshSelf()
    } catch (err) {
      setError(formatRequestError(err, t))
      await refreshSelf()
    } finally {
      setActionPendingIds((prev) => {
        const next = { ...prev }
        delete next[id]
        return next
      })
    }
  }

  if (activePage !== 'quotes') return null

  return (
    <section className="panel" data-testid="quotes-page">
      <div className="panel-head">
        <h2 data-testid="quotes-heading">{t('quotes')}</h2>
        <div className="inline-tools">
          <span className="muted-filter">{t('approvalMode')}: {tenantApprovalMode === 'STAGE_GATE' ? t('approvalModeStageGate') : t('approvalModeStrict')}</span>
          <button className="mini-btn" data-testid="quotes-refresh" onClick={refreshSelf}>{t('refresh')}</button>
          <button className="primary-btn" data-testid="quotes-create" disabled={!canWrite} onClick={openCreate}>{t('createQuote')}</button>
        </div>
      </div>
      <div className="filter-row" style={{ marginBottom: 8 }}>
        <input
          data-testid="quotes-owner-filter"
          className="tool-input"
          placeholder={t('owner')}
          value={ownerFilter}
          onChange={(e) => setOwnerFilter(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') refreshSelf()
          }}
        />
        <select data-testid="quotes-status-filter" className="tool-input" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">{t('allStatuses')}</option>
          {statusOptions.map((opt) => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
        </select>
      </div>
      <div className="filter-bar">
        <button data-testid="quotes-query" className="mini-btn" onClick={refreshSelf}>{t('query')}</button>
        <button className="mini-btn" onClick={resetFilters}>{t('clearFilters')}</button>
      </div>
      <div className="filter-bar" style={{ marginBottom: 8 }}>
        <span className="muted-filter">{t('batchSelectedCount')}: {selectedCount}</span>
        <button className="mini-btn" onClick={selectPage}>{t('selectPage')}</button>
        <button className="mini-btn" onClick={clearSelection}>{t('clearSelection')}</button>
        <input className="tool-input" placeholder={t('batchOwnerPlaceholder')} value={batchOwner} onChange={(e) => setBatchOwner(e.target.value)} />
        <select className="tool-input" value={batchStatus} onChange={(e) => setBatchStatus(e.target.value)}>
          <option value="">{t('batchSetStatus')}</option>
          {statusOptions.map((opt) => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
        </select>
        <button className="mini-btn" disabled={!canWrite} onClick={batchAssign}>{t('batchAssignOwner')}</button>
        <button className="mini-btn" disabled={!canWrite} onClick={batchChangeStatus}>{t('batchSetStatus')}</button>
        {batchSummary?.failed > 0 && <button className="mini-btn" onClick={() => setBatchModalOpen(true)}>{t('batchResultTitle')}</button>}
      </div>
      {batchMessage && <div className="info-banner" style={{ marginBottom: 8 }}>{batchMessage}</div>}
      {error && <div className="field-error" style={{ marginBottom: 8 }}>{error}</div>}
      {!!lastLoadedAt && stale && <div className="info-banner" style={{ marginBottom: 8 }}>{t('loading')}</div>}
      {submitResult && (
        <div className="info-banner" style={{ marginBottom: 8 }}>
          {submitResult.approvalTriggered
            ? `${t('approvalTriggered')}: ${submitResult.approvalInstanceId || '-'}`
            : `${t('approvalNotTriggered')} (${t('approvalThresholdHint')})`}
        </div>
      )}
      {orderResult && (
        <div className="info-banner" style={{ marginBottom: 8 }}>
          {`${t('toOrder')}: ${orderResult.orderId || orderResult.id || '-'} (${translateStatus(t, orderResult.orderStatus || orderResult.status || '-')})`}
        </div>
      )}
      {!loading && opportunityFilter && filteredItems.length === 0 && (
        <div className="empty-tip" style={{ marginBottom: 8 }}>{t('filteredResultEmpty')}</div>
      )}
      <div className="table-row table-head-row table-row-6"><span><input data-testid="quotes-select-all" type="checkbox" checked={allChecked} onChange={(e) => toggleAll(e.target.checked)} /></span><span>{t('idLabel')}</span><span>{t('owner')}</span><span>{t('status')}</span><span>{t('amount')}</span><span>{t('action')}</span></div>
      <ListState loading={loading} empty={!loading && filteredItems.length === 0} emptyText={t('noData')} />
      {!loading && filteredItems.length > 0 && (
        <VirtualListTable
          rows={filteredItems}
          viewportHeight={460}
          getRowKey={(row) => row.id}
          renderRow={(row) => (
            <QuoteRow
              key={row.id}
              row={row}
              checked={selectedIds.has(row.id)}
              onToggle={(e) => toggleOne(row.id, e.target.checked)}
              t={t}
              act={act}
              openEdit={openEdit}
              setSelectedQuoteId={setSelectedQuoteId}
              pending={!!actionPendingIds[row.id]}
            />
          )}
        />
      )}

      <Suspense fallback={<div className="loading">{t('loading')}</div>}>
        <QuoteItemsSection
          t={t}
          canWrite={canWrite}
          effectiveSelectedQuoteId={effectiveSelectedQuoteId}
          itemJson={itemJson}
          setItemJson={setItemJson}
          saveItems={saveItems}
          quoteItems={quoteItems}
        />
      </Suspense>

      {openModal && (
        <Suspense fallback={<div className="loading">{t('loading')}</div>}>
          <QuoteEditorModal
            t={t}
            canWrite={canWrite}
            form={form}
            setForm={setForm}
            setOpenModal={setOpenModal}
            saveQuote={saveQuote}
          />
        </Suspense>
      )}
      <BatchResultModal t={t} open={batchModalOpen} summary={batchSummary} onClose={() => { setBatchModalOpen(false); clearSummary() }} />
    </section>
  )
}

export default memo(QuotesPanel)
