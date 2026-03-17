import { memo, useMemo } from 'react'
import { ROLES, translateRole } from '../../../shared'
import ServerPager from '../../ServerPager'
import VirtualListTable from '../../VirtualListTable'
import { useGovernanceTableState } from '../../../hooks/useGovernanceTableState'

const PAGE_SIZES = [5, 8, 12, 20]

function UsersGovernanceSection({ t, users, onRefresh }) {
  const tableState = useGovernanceTableState({ page: 1, size: 8 })
  const userQuery = tableState.query
  const setUserQuery = tableState.setQuery
  const userRoleFilter = tableState.filter
  const setUserRoleFilter = tableState.setFilter
  const userPage = tableState.page
  const setUserPage = tableState.setPage
  const userSize = tableState.size
  const setUserSize = tableState.setSize

  const usersFiltered = useMemo(() => {
    const q = String(userQuery || '').trim().toLowerCase()
    return (users.adminUsers || []).filter((row) => {
      if (userRoleFilter && String(row.role || '') !== userRoleFilter) return false
      if (!q) return true
      return String(row.username || '').toLowerCase().includes(q)
        || String(row.ownerScope || '').toLowerCase().includes(q)
    })
  }, [users.adminUsers, userQuery, userRoleFilter])

  const userTotalPages = Math.max(1, Math.ceil(usersFiltered.length / userSize))
  const userRows = usersFiltered.slice((userPage - 1) * userSize, userPage * userSize)

  return (
    <>
      <div className="panel-head"><h2>{t('usersAdmin')}</h2><button className="mini-btn" onClick={onRefresh}>{t('refresh')}</button></div>

      <div className="inline-tools filter-row" style={{ marginBottom: 8 }}>
        <input className="tool-input" placeholder={t('username')} value={users.inviteForm.username} onChange={(e) => users.setInviteForm((p) => ({ ...p, username: e.target.value }))} />
        <select className="tool-input" value={users.inviteForm.role} onChange={(e) => users.setInviteForm((p) => ({ ...p, role: e.target.value }))}>
          {ROLES.map((r) => <option key={r} value={r}>{translateRole(t, r)}</option>)}
        </select>
        <input className="tool-input" placeholder={t('ownerScope')} value={users.inviteForm.ownerScope} onChange={(e) => users.setInviteForm((p) => ({ ...p, ownerScope: e.target.value }))} />
      </div>

      <div className="inline-tools filter-bar" style={{ marginBottom: 8 }}>
        <button className="mini-btn" onClick={users.inviteUser}>{t('inviteActivateBtn')}</button>
        <input className="tool-input" placeholder={t('search')} value={userQuery} onChange={(e) => setUserQuery(e.target.value)} />
        <select className="tool-input" value={userRoleFilter} onChange={(e) => setUserRoleFilter(e.target.value)}>
          <option value="">{t('allRoles')}</option>
          {ROLES.map((r) => <option key={r} value={r}>{translateRole(t, r)}</option>)}
        </select>
        <button className="mini-btn" onClick={tableState.reset}>{t('reset')}</button>
      </div>

      {users.inviteResult?.inviteLink && <div className="info-banner" style={{ marginBottom: 10 }}>{t('inviteLink')}: {users.inviteResult.inviteLink}</div>}

      <div className="table-row table-head-row table-row-7 compact">
        <span>{t('username')}</span>
        <span>{t('role')}</span>
        <span>{t('ownerScope')}</span>
        <span>{t('enabled')}</span>
        <span>{t('status')}</span>
        <span>{t('duration')}</span>
        <span>{t('action')}</span>
      </div>

      <VirtualListTable
        rows={userRows}
        rowHeight={52}
        viewportHeight={Math.min(420, Math.max(160, userRows.length * 52))}
        getRowKey={(u) => u.id || u.username}
        renderRow={(u) => (
          <div key={u.id || u.username} className="table-row table-row-7 compact">
            <span>{u.username}</span>
            <span>
              <select className="tool-input" value={u.role} onChange={(e) => users.setAdminUsers((prev) => prev.map((x) => x.id === u.id ? { ...x, role: e.target.value } : x))}>
                {ROLES.map((r) => <option key={r} value={r}>{translateRole(t, r)}</option>)}
              </select>
            </span>
            <span><input className={users.getAdminUserError(u) ? 'tool-input input-invalid' : 'tool-input'} value={u.ownerScope || ''} placeholder={t('ownerScope')} onChange={(e) => users.setAdminUsers((prev) => prev.map((x) => x.id === u.id ? { ...x, ownerScope: e.target.value } : x))} /></span>
            <span><label className="switch-inline"><input type="checkbox" checked={!!u.enabled} onChange={(e) => users.setAdminUsers((prev) => prev.map((x) => x.id === u.id ? { ...x, enabled: e.target.checked } : x))} />{t('enabled')}</label></span>
            <span>{u.locked ? t('locked') : '-'}</span>
            <span>{u.lockRemainingSeconds || 0}s</span>
            <span className="inline-tools"><button className="mini-btn" disabled={!!users.getAdminUserError(u)} onClick={() => users.saveAdminUser(u)}>{t('save')}</button><button className="mini-btn" onClick={() => users.unlockAdminUser(u.id)}>{t('unlock')}</button></span>
          </div>
        )}
      />
      {usersFiltered.length === 0 && <div className="empty-tip">{t('noData')}</div>}
      {usersFiltered.length > 0 && <ServerPager t={t} page={Math.min(userPage, userTotalPages)} totalPages={userTotalPages} size={userSize} onPageChange={setUserPage} onSizeChange={setUserSize} sizeOptions={PAGE_SIZES} />}
    </>
  )
}

export default memo(UsersGovernanceSection)
