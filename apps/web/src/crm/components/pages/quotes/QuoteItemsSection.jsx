import { formatMoney } from '../../../shared'

function QuoteItemsSection({
  t,
  canWrite,
  effectiveSelectedQuoteId,
  itemJson,
  setItemJson,
  saveItems,
  quoteItems,
}) {
  return (
    <div className="panel" style={{ marginTop: 12, boxShadow: 'none' }} data-testid="quote-items-panel">
      <div className="panel-head"><h2>{t('quoteItems')}</h2><span>{effectiveSelectedQuoteId || '-'}</span></div>
      <textarea className="tool-input" style={{ width: '100%', minHeight: 120 }} value={itemJson} onChange={(e) => setItemJson(e.target.value)} />
      <div className="inline-tools" style={{ marginTop: 8 }}>
        <button className="mini-btn" disabled={!canWrite || !effectiveSelectedQuoteId} onClick={saveItems}>{t('save')}</button>
      </div>
      <div className="table-row table-head-row"><span>{t('product')}</span><span>{t('qty')}</span><span>{t('price')}</span><span>{t('amount')}</span></div>
      {quoteItems.map((row) => (
        <div key={row.id} className="table-row">
          <span>{row.productName || row.productId}</span>
          <span>{row.quantity}</span>
          <span>{formatMoney(row.unitPrice)}</span>
          <span>{formatMoney(row.totalAmount)}</span>
        </div>
      ))}
      {quoteItems.length === 0 && <div className="empty-tip">{t('noData')}</div>}
    </div>
  )
}

export default QuoteItemsSection
