import { memo } from 'react'

const CustomerRow = memo(function CustomerRow({
  row,
  checked,
  onToggle,
  t,
  openDetail,
  editCustomer,
  canDeleteCustomer,
  removeCustomer,
}) {
  return (
    <div className="table-row table-row-6">
      <span><input type="checkbox" checked={checked} onChange={onToggle} /></span>
      <span>{row.name}</span>
      <span>{row.owner}</span>
      <span>{row.statusLabel}</span>
      <span>{row.valueText}</span>
      <span>
        <button className="mini-btn" onClick={() => openDetail(row)}>{t('detail')}</button>
        <button className="mini-btn" onClick={() => editCustomer(row)}>{t('save')}</button>
        {canDeleteCustomer ? <button className="danger-btn" onClick={() => removeCustomer(row.id)}>{t('delete')}</button> : null}
      </span>
    </div>
  )
})

export default CustomerRow
