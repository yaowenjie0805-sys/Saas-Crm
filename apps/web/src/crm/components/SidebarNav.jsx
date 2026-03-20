import { useEffect, useMemo, useState } from 'react'
import { translateOwnerAlias, translateRole } from '../shared'

const ICONS = {
  dashboard: 'DB',
  customers: 'CU',
  contacts: 'CT',
  pipeline: 'OP',
  contracts: 'CN',
  payments: 'PM',
  followUps: 'FU',
  tasks: 'TK',
  audit: 'AU',
  permissions: 'PR',
  usersAdmin: 'UG',
}

function SidebarNav({ auth, navGroups, activePage, onNavigate, onPrefetch, onLogout, t }) {
  const [collapsed, setCollapsed] = useState({})
  const [mobileMenuOpen, setMobileMenuOpen] = useState(() =>
    typeof window === 'undefined' ? true : window.innerWidth > 900
  )
  const orderedGroups = useMemo(() => Object.entries(navGroups || {}), [navGroups])
  const accountName = translateOwnerAlias(t, auth?.displayName || auth?.username || t('defaultLabel'))
  const accountRole = translateRole(t, auth?.role)

  const toggleGroup = (groupName) => {
    setCollapsed((prev) => ({ ...prev, [groupName]: !prev[groupName] }))
  }

  const navigateAndCloseIfMobile = (key) => {
    onNavigate(key)
    if (typeof window !== 'undefined' && window.innerWidth <= 900) {
      setMobileMenuOpen(false)
    }
  }

  useEffect(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return undefined
    const media = window.matchMedia('(max-width: 900px)')
    const syncMenu = (event) => setMobileMenuOpen(!event.matches)
    syncMenu(media)
    media.addEventListener('change', syncMenu)
    return () => media.removeEventListener('change', syncMenu)
  }, [])

  useEffect(() => {
    const onEscape = (event) => {
      if (event.key !== 'Escape') return
      if (typeof window === 'undefined' || window.innerWidth > 900) return
      setMobileMenuOpen(false)
    }
    window.addEventListener('keydown', onEscape)
    return () => window.removeEventListener('keydown', onEscape)
  }, [])

  return (
    <aside className="sidebar" data-testid="app-sidebar">
      <div className="sidebar-head">
        <div className="brand-wrap">
          <div className="brand-mark">A</div>
          <div>
            <div className="brand">Aster CRM</div>
            <div className="brand-sub">{t('workspaceLabel')}</div>
          </div>
        </div>
        <button
          className="mobile-menu-toggle"
          aria-label={mobileMenuOpen ? t('menuClose') : t('menuOpen')}
          onClick={() => setMobileMenuOpen((v) => !v)}
        >
          {mobileMenuOpen ? t('menuClose') : t('menuOpen')}
        </button>
        <div className="role-pill" data-testid="account-pill">{accountName} | {accountRole}</div>
      </div>

      <nav className={`menu grouped ${mobileMenuOpen ? 'mobile-open' : ''}`}>
        {orderedGroups.map(([groupName, groupItems]) => {
          const isCollapsed = !!collapsed[groupName]
          return (
            <section key={groupName} className="menu-group">
              <button className="menu-group-toggle" onClick={() => toggleGroup(groupName)}>
                <span className="menu-group-title">{groupName}</span>
                <span className={`menu-group-arrow ${isCollapsed ? 'collapsed' : ''}`}>v</span>
              </button>
              {!isCollapsed && (
                <div className="menu-items">
                  {groupItems.map((item) => (
                    <button
                      key={item.key}
                      data-testid={`nav-${item.key}`}
                      className={activePage === item.key ? 'active' : ''}
                      onMouseEnter={() => onPrefetch?.(item.key)}
                      onFocus={() => onPrefetch?.(item.key)}
                      onClick={() => navigateAndCloseIfMobile(item.key)}
                    >
                      <span className="menu-icon">{ICONS[item.key] || '--'}</span>
                      <span>{item.label}</span>
                    </button>
                  ))}
                </div>
              )}
            </section>
          )
        })}
      </nav>

      {mobileMenuOpen && (
        <button
          type="button"
          className="mobile-menu-backdrop"
          aria-label={t('menuClose')}
          onClick={() => setMobileMenuOpen(false)}
        />
      )}

      <div className="sidebar-foot sidebar-foot-quiet">
        <button className="logout-btn sidebar-logout-btn" onClick={onLogout}>
          {t('logout')}
        </button>
      </div>
    </aside>
  )
}

export default SidebarNav