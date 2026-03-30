import { memo, useCallback, useMemo, useState } from 'react'
import { api, formatMoney, translateStatus } from '../../shared'
import ListState from '../ListState'
import { useBatchActions } from '../useBatchActions'
import BatchResultModal from '../BatchResultModal'
import VirtualListTable from '../VirtualListTable'
import { useSelectionSet } from '../../hooks/useSelectionSet'

const EMPTY_FORM = { id: '', code: '', name: '', category: '', status: 'ACTIVE', standardPrice: '0', taxRate: '0', currency: 'CNY', unit: '' }
const EMPTY_ROWS = []

const ProductRow = memo(function ProductRow({ row, checked, onToggle, t, openEdit }) {
  return (
    <div className="table-row table-row-6">
      <span><input type="checkbox" checked={checked} onChange={onToggle} /></span>
      <span>{row.code}</span>
      <span>{row.name}</span>
      <span>{translateStatus(t, row.status)}</span>
      <span>{formatMoney(row.standardPrice)}</span>
      <span><button className="mini-btn" onClick={() => openEdit(row)}>{t('detail')}</button></span>
    </div>
  )
})

function ProductsPanel({ activePage, t, canWrite, apiContext, refreshPage, commerce }) {
  const products = commerce?.products || {}
  const items = products.items ?? EMPTY_ROWS
  const status = products.status || ''
  const queryCode = products.queryCode || ''
  const queryName = products.queryName || ''
  const queryCategory = products.queryCategory || ''
  const loading = !!products.loading
  const error = products.error || ''
  const setError = products.setError || (() => {})
  const lastLoadedAt = products.lastLoadedAt || 0
  const stale = !!products.stale
  const setStatus = products.setStatus || (() => {})
  const setQueryCode = products.setQueryCode || (() => {})
  const setQueryName = products.setQueryName || (() => {})
  const setQueryCategory = products.setQueryCategory || (() => {})
  const [form, setForm] = useState(EMPTY_FORM)
  const [openModal, setOpenModal] = useState(false)
  const [batchStatus, setBatchStatus] = useState('')
  const [batchModalOpen, setBatchModalOpen] = useState(false)

  const token = apiContext?.token
  const lang = apiContext?.lang || 'en'
  const { summary: batchSummary, toastMessage: batchMessage, runBatch, clearSummary } = useBatchActions({ t })
  const refreshSelf = useCallback(async () => {
    await refreshPage('products', 'panel_action')
  }, [refreshPage])

  const filteredItems = useMemo(() => {
    const code = queryCode.trim().toLowerCase()
    const name = queryName.trim().toLowerCase()
    const category = queryCategory.trim().toLowerCase()
    return (items || []).filter((row) => {
      if (code && !String(row.code || '').toLowerCase().includes(code)) return false
      if (name && !String(row.name || '').toLowerCase().includes(name)) return false
      if (category && !String(row.category || '').toLowerCase().includes(category)) return false
      return true
    })
  }, [items, queryCode, queryName, queryCategory])
  const selection = useSelectionSet(filteredItems, (row) => row.id)
  const { selectedIds, selectedCount, allChecked, clearSelection, selectPage, toggleAll, toggleOne } = selection
  const byId = useMemo(() => new Map((items || []).map((row) => [row.id, row])), [items])

  const batchChangeStatus = async () => {
    if (!batchStatus) return
    const result = await runBatch({
      ids: [...selectedIds],
      worker: async (id) => {
        const row = byId.get(id)
        if (!row) return
        const payload = {
          id: row.id,
          code: row.code || '',
          name: row.name || '',
          category: row.category || '',
          status: batchStatus,
          standardPrice: Number(row.standardPrice || 0),
          taxRate: Number(row.taxRate || 0),
          currency: row.currency || 'CNY',
          unit: row.unit || '',
        }
        await api('/v1/products', { method: 'PATCH', body: JSON.stringify(payload) }, token, lang)
      },
      batch: { path: '/v1/products/batch-actions', action: 'UPDATE_STATUS', payload: { status: batchStatus }, token, lang },
    })
    if (result?.failed > 0) setBatchModalOpen(true)
    clearSelection()
    await refreshSelf()
  }

  const resetFilters = () => {
    setQueryCode('')
    setQueryName('')
    setQueryCategory('')
    setStatus('')
  }

  const openCreate = () => {
    setForm(EMPTY_FORM)
    setOpenModal(true)
  }

  const openEdit = (row) => {
    setForm({
      id: row.id,
      code: row.code || '',
      name: row.name || '',
      category: row.category || '',
      status: row.status || 'ACTIVE',
      standardPrice: String(row.standardPrice || 0),
      taxRate: String(row.taxRate || 0),
      currency: row.currency || 'CNY',
      unit: row.unit || '',
    })
    setOpenModal(true)
  }

  const save = async () => {
    if (!canWrite) return
    try {
      const payload = { ...form, standardPrice: Number(form.standardPrice || 0), taxRate: Number(form.taxRate || 0) }
      if (form.id) await api('/v1/products', { method: 'PATCH', body: JSON.stringify(payload) }, token, lang)
      else await api('/v1/products', { method: 'POST', body: JSON.stringify(payload) }, token, lang)
      setOpenModal(false)
      setForm(EMPTY_FORM)
      await refreshSelf()
    } catch (err) {
      setError(err.requestId ? `${err.message} [${err.requestId}]` : err.message)
    }
  }

  if (activePage !== 'products') return null

  return (
    <section className="panel" data-testid="products-page">
      <div className="panel-head">
        <h2>{t('products')}</h2>
        <div className="inline-tools">
          <button className="mini-btn" onClick={refreshSelf}>{t('refresh')}</button>
          <button className="primary-btn" disabled={!canWrite} onClick={openCreate}>{t('createProduct')}</button>
        </div>
      </div>

      <div className="filter-row" style={{ marginBottom: 8 }}>
        <input className="tool-input" placeholder={t('code')} value={queryCode} onChange={(e) => setQueryCode(e.target.value)} />
        <input className="tool-input" placeholder={t('title')} value={queryName} onChange={(e) => setQueryName(e.target.value)} />
        <input className="tool-input" placeholder={t('category')} value={queryCategory} onChange={(e) => setQueryCategory(e.target.value)} />
        <select className="tool-input" value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">{t('allStatuses')}</option>
          <option value="ACTIVE">{translateStatus(t, 'ACTIVE')}</option>
          <option value="INACTIVE">{translateStatus(t, 'INACTIVE')}</option>
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
        <select className="tool-input" value={batchStatus} onChange={(e) => setBatchStatus(e.target.value)}>
          <option value="">{t('batchSetStatus')}</option>
          <option value="ACTIVE">{translateStatus(t, 'ACTIVE')}</option>
          <option value="INACTIVE">{translateStatus(t, 'INACTIVE')}</option>
        </select>
        <button className="mini-btn" disabled={!canWrite} onClick={batchChangeStatus}>{t('batchSetStatus')}</button>
        {batchSummary?.failed > 0 && <button className="mini-btn" onClick={() => setBatchModalOpen(true)}>{t('batchResultTitle')}</button>}
      </div>
      {batchMessage && <div className="info-banner" style={{ marginBottom: 8 }}>{batchMessage}</div>}

      {error && <div className="field-error" style={{ marginBottom: 8 }}>{error}</div>}
      {!!lastLoadedAt && stale && <div className="info-banner" style={{ marginBottom: 8 }}>{t('loading')}</div>}
      <div className="table-row table-head-row table-row-6"><span><input type="checkbox" checked={allChecked} onChange={(e) => toggleAll(e.target.checked)} /></span><span>{t('code')}</span><span>{t('title')}</span><span>{t('status')}</span><span>{t('amount')}</span><span>{t('action')}</span></div>
      <ListState loading={loading} empty={!loading && filteredItems.length === 0} emptyText={t('noData')} />
      {!loading && filteredItems.length > 0 && (
        <VirtualListTable
          rows={filteredItems}
          viewportHeight={460}
          getRowKey={(row) => row.id}
          renderRow={(row) => (
            <ProductRow
              key={row.id}
              row={row}
              checked={selectedIds.has(row.id)}
              onToggle={(e) => toggleOne(row.id, e.target.checked)}
              t={t}
              openEdit={openEdit}
            />
          )}
        />
      )}

      {openModal && (
        <div className="modal-mask">
          <div className="modal-card">
            <div className="panel-head">
              <h2>{form.id ? t('editProduct') : t('createProduct')}</h2>
              <button className="mini-btn" onClick={() => setOpenModal(false)}>{t('closeModal')}</button>
            </div>
            <div className="forms-grid">
              <input className="tool-input" placeholder={t('code')} value={form.code} onChange={(e) => setForm((p) => ({ ...p, code: e.target.value }))} />
              <input className="tool-input" placeholder={t('title')} value={form.name} onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))} />
              <input className="tool-input" placeholder={t('category')} value={form.category} onChange={(e) => setForm((p) => ({ ...p, category: e.target.value }))} />
              <input className="tool-input" placeholder={t('standardPrice')} value={form.standardPrice} onChange={(e) => setForm((p) => ({ ...p, standardPrice: e.target.value }))} />
              <input className="tool-input" placeholder={t('taxRateLabel')} value={form.taxRate} onChange={(e) => setForm((p) => ({ ...p, taxRate: e.target.value }))} />
              <select className="tool-input" value={form.status} onChange={(e) => setForm((p) => ({ ...p, status: e.target.value }))}>
                <option value="ACTIVE">{translateStatus(t, 'ACTIVE')}</option>
                <option value="INACTIVE">{translateStatus(t, 'INACTIVE')}</option>
              </select>
            </div>
            <div className="inline-tools" style={{ marginTop: 12 }}>
              <button className="primary-btn" disabled={!canWrite} onClick={save}>{t('save')}</button>
            </div>
          </div>
        </div>
      )}
      <BatchResultModal t={t} open={batchModalOpen} summary={batchSummary} onClose={() => { setBatchModalOpen(false); clearSummary() }} />
    </section>
  )
}

export default memo(ProductsPanel)
