import { memo } from 'react'
import { translateStatus } from '../../../shared'
import { formatQuoteAmount } from './quotePanelHelpers'

export const QuoteRow = memo(function QuoteRow({ row, checked, onToggle, t, act, openEdit, setSelectedQuoteId, pending }) {
  return (
    <div className="table-row table-row-6">
      <span><input type="checkbox" checked={checked} onChange={onToggle} /></span>
      <span>{row.quoteNo}</span>
      <span>{row.owner}</span>
      <span>{translateStatus(t, row.status)}</span>
      <span>{formatQuoteAmount(row.totalAmount)}</span>
      <span>
        <button className="mini-btn" onClick={() => { setSelectedQuoteId(row.id); openEdit(row) }}>{t('detail')}</button>
        <button className="mini-btn" disabled={pending} onClick={() => act(row.id, 'submit')}>{t('submit')}</button>
        <button className="mini-btn" disabled={pending} onClick={() => act(row.id, 'accept')}>{t('accept')}</button>
        <button className="mini-btn" disabled={pending} onClick={() => act(row.id, 'to-order')}>{t('toOrder')}</button>
      </span>
    </div>
  )
})
