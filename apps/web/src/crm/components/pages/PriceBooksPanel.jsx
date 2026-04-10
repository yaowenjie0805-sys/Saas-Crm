import { memo, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { api, formatMoney, translateStatus } from '../../shared'
import ListState from '../ListState'
import VirtualListTable from '../VirtualListTable'
const NOOP = () => {}
const EMPTY_ROWS = []
function PriceBooksPanel({ activePage, t, canWrite, apiContext, refreshPage, commerce }) {
  const priceBooks = commerce?.priceBooks || {}
  const books = priceBooks.books ?? EMPTY_ROWS
  const loading = !!priceBooks.loading
  const error = priceBooks.error || ''
  const setError = priceBooks.setError || NOOP
  const lastLoadedAt = priceBooks.lastLoadedAt || 0
  const stale = !!priceBooks.stale
  const statusFilter = priceBooks.statusFilter || ''
  const setStatusFilter = priceBooks.setStatusFilter || (() => {})
  const nameFilter = priceBooks.nameFilter || ''
  const setNameFilter = priceBooks.setNameFilter || (() => {})
  const [bookForm, setBookForm] = useState({ id: '', name: '', status: 'ACTIVE', isDefault: false, currency: 'CNY' })
  const [selectedBookId, setSelectedBookId] = useState('')
  const [items, setItems] = useState([])
  const [itemForm, setItemForm] = useState({ productId: '', price: '0', taxRate: '0', currency: 'CNY' })
  const itemsAbortRef = useRef(null)

  const token = apiContext?.token
  const lang = apiContext?.lang || 'en'
  const effectiveSelectedBookId = selectedBookId || books[0]?.id || ''

  const refreshSelf = useCallback(async () => {
    await refreshPage('priceBooks', 'panel_action')
  }, [refreshPage])

  const loadItems = useCallback(async (bookId = effectiveSelectedBookId) => {
    if (!token || !bookId) { setItems([]); return }
    itemsAbortRef.current?.abort()
    const controller = new AbortController()
    itemsAbortRef.current = controller
    try {
      const data = await api(`/v1/price-books/${bookId}/items`, { signal: controller.signal }, token, lang)
      setItems(data.items || [])
    } catch (err) {
      if (err?.name === 'AbortError') return
      setError(err.requestId ? `${err.message} [${err.requestId}]` : err.message)
    }
  }, [token, lang, effectiveSelectedBookId, setError])
  // Sync: load items when book selection changes
  useEffect(() => { if (activePage === 'priceBooks' && effectiveSelectedBookId) loadItems(effectiveSelectedBookId) }, [activePage, effectiveSelectedBookId, loadItems])
  useEffect(() => () => { itemsAbortRef.current?.abort() }, [])
  const filteredBooks = useMemo(() => {
    const name = nameFilter.trim().toLowerCase()
    return (books || []).filter((row) => {
      if (name && !String(row.name || '').toLowerCase().includes(name)) return false
      return true
    })
  }, [books, nameFilter])
  const resetFilters = () => {
    setStatusFilter('')
    setNameFilter('')
  }

  const saveBook = async () => {
    if (!canWrite) return
    try {
      if (bookForm.id) {
        await api('/v1/price-books', { method: 'PATCH', body: JSON.stringify(bookForm) }, token, lang)
      } else {
        await api('/v1/price-books', { method: 'POST', body: JSON.stringify(bookForm) }, token, lang)
      }
      setBookForm({ id: '', name: '', status: 'ACTIVE', isDefault: false, currency: 'CNY' })
      await refreshSelf()
    } catch (err) {
      setError(err.requestId ? `${err.message} [${err.requestId}]` : err.message)
    }
  }

  const saveItem = async () => {
    if (!canWrite || !effectiveSelectedBookId) return
    try {
      await api(`/v1/price-books/${effectiveSelectedBookId}/items`, {
        method: 'POST',
        body: JSON.stringify({ ...itemForm, price: Number(itemForm.price || 0), taxRate: Number(itemForm.taxRate || 0) }),
      }, token, lang)
      setItemForm({ productId: '', price: '0', taxRate: '0', currency: 'CNY' })
      await loadItems(effectiveSelectedBookId)
    } catch (err) {
      setError(err.requestId ? `${err.message} [${err.requestId}]` : err.message)
    }
  }

  if (activePage !== 'priceBooks') return null

  return (
    <section className="panel">
      <div className="panel-head"><h2>{t('priceBooks')}</h2><button className="mini-btn" onClick={refreshSelf}>{t('refresh')}</button></div>
      {error && <div className="field-error" style={{ marginBottom: 8 }}>{error}</div>}
      {!!lastLoadedAt && stale && <div className="info-banner" style={{ marginBottom: 8 }}>{t('loading')}</div>}
      <div className="filter-row" style={{ marginBottom: 8 }}>
        <input className="tool-input" placeholder={t('title')} value={nameFilter} onChange={(e) => setNameFilter(e.target.value)} />
        <select className="tool-input" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">{t('allStatuses')}</option>
          <option value="ACTIVE">{translateStatus(t, 'ACTIVE')}</option>
          <option value="INACTIVE">{translateStatus(t, 'INACTIVE')}</option>
        </select>
      </div>
      <div className="filter-bar">
        <button className="mini-btn" onClick={refreshSelf}>{t('query')}</button>
        <button className="mini-btn" onClick={resetFilters}>{t('clearFilters')}</button>
      </div>
      <div className="inline-tools" style={{ marginBottom: 10 }}>
        <input className="tool-input" placeholder={t('title')} value={bookForm.name} onChange={(e) => setBookForm((p) => ({ ...p, name: e.target.value }))} />
        <select className="tool-input" value={bookForm.status} onChange={(e) => setBookForm((p) => ({ ...p, status: e.target.value }))}>
          <option value="ACTIVE">{translateStatus(t, 'ACTIVE')}</option><option value="INACTIVE">{translateStatus(t, 'INACTIVE')}</option>
        </select>
        <label className="switch-inline"><input type="checkbox" checked={!!bookForm.isDefault} onChange={(e) => setBookForm((p) => ({ ...p, isDefault: e.target.checked }))} />{t('defaultLabel')}</label>
        <button className="mini-btn" disabled={!canWrite} onClick={saveBook}>{bookForm.id ? t('save') : t('create')}</button>
      </div>
      <div className="table-row table-head-row"><span>{t('title')}</span><span>{t('status')}</span><span>{t('defaultLabel')}</span><span>{t('action')}</span></div>
      <ListState loading={loading} empty={!loading && filteredBooks.length === 0} emptyText={t('noData')} />
      {!loading && filteredBooks.length > 0 && (
        <VirtualListTable
          rows={filteredBooks}
          rowHeight={42}
          viewportHeight={336}
          getRowKey={(row) => row.id}
          renderRow={(row) => (
            <div key={row.id} className="table-row">
              <span>{row.name}</span><span>{translateStatus(t, row.status)}</span><span>{row.isDefault ? t('yes') : t('no')}</span>
              <span>
                <button className="mini-btn" onClick={() => { setBookForm({ id: row.id, name: row.name || '', status: row.status || 'ACTIVE', isDefault: !!row.isDefault, currency: row.currency || 'CNY' }); setSelectedBookId(row.id) }}>{t('detail')}</button>
              </span>
            </div>
          )}
        />
      )}

      <div className="panel" style={{ marginTop: 12, boxShadow: 'none' }}>
        <div className="panel-head"><h2>{t('priceBookItems')}</h2><span>{effectiveSelectedBookId || '-'}</span></div>
        <div className="inline-tools" style={{ marginBottom: 10 }}>
          <input className="tool-input" placeholder={t('productIdLabel')} value={itemForm.productId} onChange={(e) => setItemForm((p) => ({ ...p, productId: e.target.value }))} />
          <input className="tool-input" placeholder={t('amount')} value={itemForm.price} onChange={(e) => setItemForm((p) => ({ ...p, price: e.target.value }))} />
          <input className="tool-input" placeholder={t('taxRateLabel')} value={itemForm.taxRate} onChange={(e) => setItemForm((p) => ({ ...p, taxRate: e.target.value }))} />
          <button className="mini-btn" disabled={!canWrite || !effectiveSelectedBookId} onClick={saveItem}>{t('save')}</button>
        </div>
        <div className="table-row table-head-row"><span>{t('productIdLabel')}</span><span>{t('amount')}</span><span>{t('taxRateLabel')}</span><span>{t('status')}</span></div>
        {items.length > 0 && (
          <VirtualListTable
            rows={items}
            rowHeight={42}
            viewportHeight={294}
            getRowKey={(row) => row.id}
            renderRow={(row) => (
              <div key={row.id} className="table-row"><span>{row.productId}</span><span>{formatMoney(row.price)}</span><span>{row.taxRate}</span><span>{row.currency}</span></div>
            )}
          />
        )}
        {items.length === 0 && <div className="empty-tip">{t('noData')}</div>}
      </div>
    </section>
  )
}

export default memo(PriceBooksPanel)
