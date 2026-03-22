import { useCallback, useMemo } from 'react'
import { formatMoney } from '../../../shared'

const EMPTY_ROW = { productId: '', quantity: 1, unitPrice: 0, discountRate: 0, taxRate: 0 }

function parseItemRows(json) {
  try {
    const parsed = JSON.parse(json)
    if (Array.isArray(parsed) && parsed.length > 0) return parsed
  } catch { /* ignore */ }
  return [{ ...EMPTY_ROW }]
}

function QuoteItemsSection({
  t,
  canWrite,
  effectiveSelectedQuoteId,
  itemJson,
  setItemJson,
  saveItems,
  quoteItems,
}) {
  const editRows = useMemo(() => parseItemRows(itemJson), [itemJson])

  const updateRows = useCallback((newRows) => {
    setItemJson(JSON.stringify(newRows))
  }, [setItemJson])

  const updateField = useCallback((index, field, value) => {
    const rows = parseItemRows(itemJson)
    const numFields = ['quantity', 'unitPrice', 'discountRate', 'taxRate']
    rows[index] = { ...rows[index], [field]: numFields.includes(field) ? Number(value) || 0 : value }
    updateRows(rows)
  }, [itemJson, updateRows])

  const addRow = useCallback(() => {
    updateRows([...parseItemRows(itemJson), { ...EMPTY_ROW }])
  }, [itemJson, updateRows])

  const removeRow = useCallback((index) => {
    const rows = parseItemRows(itemJson)
    if (rows.length <= 1) return
    rows.splice(index, 1)
    updateRows(rows)
  }, [itemJson, updateRows])

  return (
    <div className="panel" style={{ marginTop: 12, boxShadow: 'none' }} data-testid="quote-items-panel">
      <div className="panel-head">
        <h2>{t('quoteItems')}</h2>
        <span>{effectiveSelectedQuoteId || '-'}</span>
      </div>

      {/* Editable items table */}
      <div style={{ overflowX: 'auto' }}>
        <table className="quote-items-table" style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 8 }}>
          <thead>
            <tr>
              <th style={thStyle}>{t('productIdLabel')}</th>
              <th style={thStyle}>{t('qty')}</th>
              <th style={thStyle}>{t('price')}</th>
              <th style={thStyle}>{t('discountRateLabel')}</th>
              <th style={thStyle}>{t('taxRateLabel')}</th>
              <th style={thStyle}>{t('amount')}</th>
              {canWrite && <th style={thStyle}>{t('action')}</th>}
            </tr>
          </thead>
          <tbody>
            {editRows.map((row, i) => {
              const lineAmount = (row.quantity || 0) * (row.unitPrice || 0) * (1 - (row.discountRate || 0) / 100)
              return (
                <tr key={i}>
                  <td style={tdStyle}>
                    <input
                      className="tool-input"
                      style={inputStyle}
                      value={row.productId || ''}
                      onChange={(e) => updateField(i, 'productId', e.target.value)}
                      disabled={!canWrite}
                      placeholder={t('productIdLabel')}
                    />
                  </td>
                  <td style={tdStyle}>
                    <input
                      className="tool-input"
                      style={{ ...inputStyle, width: 70 }}
                      type="number"
                      min="0"
                      value={row.quantity ?? 1}
                      onChange={(e) => updateField(i, 'quantity', e.target.value)}
                      disabled={!canWrite}
                    />
                  </td>
                  <td style={tdStyle}>
                    <input
                      className="tool-input"
                      style={{ ...inputStyle, width: 100 }}
                      type="number"
                      min="0"
                      step="0.01"
                      value={row.unitPrice ?? 0}
                      onChange={(e) => updateField(i, 'unitPrice', e.target.value)}
                      disabled={!canWrite}
                    />
                  </td>
                  <td style={tdStyle}>
                    <input
                      className="tool-input"
                      style={{ ...inputStyle, width: 80 }}
                      type="number"
                      min="0"
                      max="100"
                      step="0.1"
                      value={row.discountRate ?? 0}
                      onChange={(e) => updateField(i, 'discountRate', e.target.value)}
                      disabled={!canWrite}
                    />
                    <span style={{ marginLeft: 2, fontSize: 12, color: '#888' }}>%</span>
                  </td>
                  <td style={tdStyle}>
                    <input
                      className="tool-input"
                      style={{ ...inputStyle, width: 80 }}
                      type="number"
                      min="0"
                      max="100"
                      step="0.1"
                      value={row.taxRate ?? 0}
                      onChange={(e) => updateField(i, 'taxRate', e.target.value)}
                      disabled={!canWrite}
                    />
                    <span style={{ marginLeft: 2, fontSize: 12, color: '#888' }}>%</span>
                  </td>
                  <td style={{ ...tdStyle, fontWeight: 500, whiteSpace: 'nowrap' }}>
                    {formatMoney(lineAmount)}
                  </td>
                  {canWrite && (
                    <td style={tdStyle}>
                      <button
                        className="mini-btn"
                        style={{ color: '#e74c3c', fontSize: 12 }}
                        disabled={editRows.length <= 1}
                        onClick={() => removeRow(i)}
                      >
                        {t('delete')}
                      </button>
                    </td>
                  )}
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      <div className="inline-tools" style={{ marginTop: 4, gap: 8, display: 'flex' }}>
        {canWrite && (
          <button className="mini-btn" onClick={addRow}>{t('addRow')}</button>
        )}
        <button
          className="primary-btn"
          disabled={!canWrite || !effectiveSelectedQuoteId}
          onClick={saveItems}
        >
          {t('save')}
        </button>
      </div>

      {/* Saved items display */}
      {quoteItems.length > 0 && (
        <div style={{ marginTop: 16 }}>
          <h3 style={{ fontSize: 13, fontWeight: 600, marginBottom: 6, color: '#555' }}>{t('savedItems')}</h3>
          <div className="table-row table-head-row">
            <span>{t('product')}</span>
            <span>{t('qty')}</span>
            <span>{t('price')}</span>
            <span>{t('amount')}</span>
          </div>
          {quoteItems.map((row) => (
            <div key={row.id} className="table-row">
              <span>{row.productName || row.productId}</span>
              <span>{row.quantity}</span>
              <span>{formatMoney(row.unitPrice)}</span>
              <span>{formatMoney(row.totalAmount)}</span>
            </div>
          ))}
        </div>
      )}
      {quoteItems.length === 0 && <div className="empty-tip" style={{ marginTop: 12 }}>{t('noData')}</div>}
    </div>
  )
}

const thStyle = {
  padding: '8px 6px',
  textAlign: 'left',
  fontSize: 12,
  fontWeight: 600,
  color: '#555',
  borderBottom: '2px solid #e2e8f0',
  whiteSpace: 'nowrap',
}

const tdStyle = {
  padding: '6px',
  verticalAlign: 'middle',
  borderBottom: '1px solid #f0f0f0',
}

const inputStyle = {
  width: '100%',
  minWidth: 60,
  padding: '4px 8px',
  fontSize: 13,
  boxSizing: 'border-box',
}

export default QuoteItemsSection
