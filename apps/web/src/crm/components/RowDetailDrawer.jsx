function RowDetailDrawer({ open, title, rows, actions, extra, onClose, t }) {
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
        {(actions || []).length > 0 && (
          <div className="inline-tools" style={{ marginTop: 10 }}>
            {actions.map((action) => (
              <button key={action.label} className={action.className || 'mini-btn'} onClick={action.onClick}>
                {action.label}
              </button>
            ))}
          </div>
        )}
        {extra ? <div style={{ marginTop: 10 }}>{extra}</div> : null}
      </aside>
    </div>
  )
}

export default RowDetailDrawer
