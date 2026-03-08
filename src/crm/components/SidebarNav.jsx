import { useMemo, useState } from 'react'

const ICONS = {
  dashboard: 'DB',
  customers: 'CU',
  contacts: 'CT',
  pipeline: 'OP',
  contracts: 'CN',
  payments: 'PM',
  followUps: 'FU',
  tasks: 'TK',
  approvals: 'AP',
  reports: 'RP',
  audit: 'AU',
  permissions: 'PR',
  usersAdmin: 'UG',
  adminTenants: 'TN',
}

function SidebarNav({ auth, navGroups, activePage, onNavigate, saveAuth, t }) {
  const [collapsed, setCollapsed] = useState({})
  const [mobileMenuOpen, setMobileMenuOpen] = useState(() => (typeof window === 'undefined' ? true : window.innerWidth > 900))
  const orderedGroups = useMemo(() => Object.entries(navGroups || {}), [navGroups])

  const toggleGroup = (groupName) => {
    setCollapsed((prev) => ({ ...prev, [groupName]: !prev[groupName] }))
  }

  const navigateAndCloseIfMobile = (key) => {
    onNavigate(key)
    if (typeof window !== 'undefined' && window.innerWidth <= 900) setMobileMenuOpen(false)
  }

  return (
    <aside className="sidebar">
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
          {mobileMenuOpen ? 'Close' : 'Menu'}
        </button>
        <div className="role-pill">{auth.displayName} | {auth.role}</div>
      </div>

      <nav className={`menu grouped ${mobileMenuOpen ? 'mobile-open' : ''}`}>
        {orderedGroups.map(([groupName, groupItems]) => {
          const isCollapsed = !!collapsed[groupName]
          return (
            <section key={groupName} className="menu-group">
              <button className="menu-group-toggle" onClick={() => toggleGroup(groupName)}>
                <span className="menu-group-title">{groupName}</span>
                <span className={`menu-group-arrow ${isCollapsed ? 'collapsed' : ''}`}>▾</span>
              </button>
              {!isCollapsed && (
                <div className="menu-items">
                  {groupItems.map((item) => (
                    <button
                      key={item.key}
                      className={activePage === item.key ? 'active' : ''}
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

      <button
        className={`logout-btn ${mobileMenuOpen ? '' : 'mobile-hidden'}`}
        onClick={() => saveAuth(null)}
      >
        {t('logout')}
      </button>
    </aside>
  )
}

export default SidebarNav
