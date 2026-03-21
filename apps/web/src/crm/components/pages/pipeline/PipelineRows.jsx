import { memo } from 'react'
import { formatMoney, translateStage } from '../../../shared'

export const OpportunityRow = memo(function OpportunityRow({
  row,
  checked,
  onToggle,
  t,
  openDetail,
  editOpportunity,
  canDeleteOpportunity,
  removeOpportunity,
}) {
  return (
    <div className="table-row table-row-6">
      <span><input type="checkbox" checked={checked} onChange={onToggle} /></span>
      <span>{translateStage(t, row.stage)}</span>
      <span>{row.owner}</span>
      <span>{row.progress}%</span>
      <span>{formatMoney(row.amount)}</span>
      <span>
        <button className="mini-btn" onClick={() => openDetail(row)}>{t('detail')}</button>
        <button className="mini-btn" onClick={() => editOpportunity(row)}>{t('save')}</button>
        {canDeleteOpportunity ? <button className="danger-btn" onClick={() => removeOpportunity(row.id)}>{t('delete')}</button> : null}
      </span>
    </div>
  )
})
