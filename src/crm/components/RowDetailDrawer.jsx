function RowDetailDrawer({ open, title, rows, onClose, t }) {
  if (!open) return null

  return (
    <div className="drawer-mask" onClick={onClose}>
      <aside className="detail-drawer" onClick={(e) => e.stopPropagation()}>
        <div className="detail-drawer-head">
          <h3>{title}</h3>
          <button className="mini-btn" onClick={onClose}>{t('close')}</button>
        </div>
        <div className="detail-list">
          {(rows || []).map((r, idx) => (
            <div key={`${r.label}-${idx}`} className="detail-row">
              <span>{r.label}</span>
              <strong>{String(r.value ?? '-')}</strong>
            </div>
          ))}
        </div>
      </aside>
    </div>
  )
}

export default RowDetailDrawer
