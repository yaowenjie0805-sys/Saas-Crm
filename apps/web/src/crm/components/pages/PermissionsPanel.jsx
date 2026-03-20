import { ROLES, translateRole } from '../../shared'

function PermissionsPanel({
  activePage,
  t,
  permissionRole,
  setPermissionRole,
  canManagePermissions,
  previewPermissionPack,
  pendingPack,
  commitPendingPack,
  rollbackPermissionRole,
  permissionPreview,
  permissionMatrix,
  changePermission,
  permissionConflicts,
}) {
  if (activePage !== 'permissions') return null

  return (
    <section className="panel">
      <div className="panel-head">
        <h2>{t('permissions')}</h2>
        <span className="muted-note">{t('permissionDesc')}</span>
      </div>
      <div className="inline-tools" style={{ marginBottom: 10 }}>
        <span className="muted-note">{t('permissionRole')}</span>
        <select className="tool-input" value={permissionRole} onChange={(e) => setPermissionRole(e.target.value)}>
          {ROLES.map((r) => <option key={r} value={r}>{translateRole(t, r)}</option>)}
        </select>
        <button className="mini-btn tone-read" disabled={!canManagePermissions} onClick={() => previewPermissionPack('grant-read')}>{t('grantReadPack')}</button>
        <button className="mini-btn tone-write" disabled={!canManagePermissions} onClick={() => previewPermissionPack('revoke-write')}>{t('revokeWritePack')}</button>
        <button className="mini-btn" disabled={!canManagePermissions || !pendingPack} onClick={commitPendingPack}>{t('applyChange')}</button>
        <button className="mini-btn" disabled={!canManagePermissions} onClick={rollbackPermissionRole}>{t('rollbackRole')}</button>
      </div>
      <div className="perm-legend"><span className="dot ok"></span>{t('allowed')}<span className="dot no"></span>{t('denied')}<span className="dot risk"></span>{t('conflicts')}</div>
      {permissionPreview && <div className="empty-tip">{t('previewChange')}: {translateRole(t, permissionPreview.role)} | {permissionPreview.permissions?.length || 0}</div>}
      <div className="permission-grid">
        <div className="permission-head">{t('action')}</div>
        {ROLES.map((r) => <div key={r} className="permission-head">{translateRole(t, r)}</div>)}
        {permissionMatrix.map((item) => (
          <div key={item.key} className="permission-row">
            <div className="permission-cell label">{t(item.key)}</div>
            {ROLES.map((r) => {
              const ok = item.roles?.includes(r)
              return (
                <button key={`${item.key}-${r}`} className={`permission-cell ${ok ? 'ok' : 'no'}`} disabled={!canManagePermissions} onClick={() => changePermission(r, item.key, !ok)}>
                  <span className="role-tag">{translateRole(t, r)}: </span>{ok ? t('allowed') : t('denied')}
                </button>
              )
            })}
          </div>
        ))}
      </div>
      <div className="panel" style={{ marginTop: 10, boxShadow: 'none' }}>
        <div className="panel-head"><h2>{t('conflicts')}</h2></div>
        {permissionConflicts.length === 0 && <div className="empty-tip">{t('noConflicts')}</div>}
        {permissionConflicts.map((c, i) => <div key={`${c.role}-${i}`} className="audit-row"><strong>{translateRole(t, c.role)}</strong><span>{t(c.message)}</span><span>-</span><small>-</small></div>)}
      </div>
    </section>
  )
}

export default PermissionsPanel
