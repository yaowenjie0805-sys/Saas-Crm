import { memo } from 'react'
import { formatMoney, translateStatus } from '../../../shared'

export const OrderRow = memo(function OrderRow({ row, checked, onToggle, t, openEdit, act, actionPending, canWrite }) {
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
