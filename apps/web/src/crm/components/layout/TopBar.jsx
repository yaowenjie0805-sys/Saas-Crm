import { memo, useEffect, useRef, useState } from 'react'

function TopBar({
  t,
  currentPageLabel,
  lang,
  setLang,
  accountLabel,
  tenantId,
  onLogout,
  onRefreshCurrentPage,
  onSearchSubmit,
}) {
  const [userMenuOpen, setUserMenuOpen] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const userMenuRef = useRef(null)
  const searchInputRef = useRef(null)

  useEffect(() => {
    const onDocClick = (event) => {
      if (!userMenuRef.current) return
      if (!userMenuRef.current.contains(event.target)) setUserMenuOpen(false)
    }
    document.addEventListener('mousedown', onDocClick)
    return () => document.removeEventListener('mousedown', onDocClick)
  }, [])

  useEffect(() => {
    const onKeyDown = (event) => {
      const isSearchShortcut = (event.ctrlKey || event.metaKey) && String(event.key || '').toLowerCase() === 'k'
      if (isSearchShortcut) {
        event.preventDefault()
        searchInputRef.current?.focus()
      }
      if (event.key === 'Escape') {
        setUserMenuOpen(false)
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [])

  const triggerSearch = () => {
    onSearchSubmit?.(searchQuery)
  }

  return (
    <header className="topbar" data-testid="topbar">
      <div className="topbar-title-wrap">
        <h1 data-testid="page-title">{currentPageLabel}</h1>
      </div>
      <div className="top-actions">
        <div className="topbar-search-zone">
          <input
            ref={searchInputRef}
            data-testid="topbar-search-input"
            className="tool-input top-search-input topbar-search-input"
            placeholder={t('globalSearch')}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') triggerSearch()
            }}
          />
        </div>
        <div className="topbar-secondary-actions">
          <button className="mini-btn topbar-quiet-btn">{t('notifications')}</button>
          <button
            className="mini-btn topbar-quiet-btn"
            data-testid="topbar-search-clear"
            onClick={() => {
              setSearchQuery('')
              searchInputRef.current?.focus()
            }}
          >
            {t('reset')}
          </button>
          <div className="language-switch topbar-lang-switch">
            <button className={lang === 'zh' ? 'active' : ''} onClick={() => setLang('zh')}>ZH</button>
            <button className={lang === 'en' ? 'active' : ''} onClick={() => setLang('en')}>EN</button>
          </div>
          <div className="user-menu-wrap" ref={userMenuRef}>
            <button className="mini-btn account-btn topbar-account-btn" onClick={() => setUserMenuOpen((v) => !v)}>
              {accountLabel}
            </button>
            {userMenuOpen && (
              <div className="user-menu">
                <div className="user-menu-meta">
                  <strong>{t('logoutMenu')}</strong>
                  <span>{accountLabel}</span>
                  <span>{tenantId || '-'}</span>
                </div>
                <button className="danger-btn" onClick={onLogout}>{t('logout')}</button>
              </div>
            )}
          </div>
        </div>
        <div className="topbar-primary-actions">
          <button className="primary-btn topbar-refresh-btn" data-testid="topbar-refresh" onClick={onRefreshCurrentPage}>{t('refresh')}</button>
          <button className="danger-btn top-logout-btn topbar-danger-btn" onClick={onLogout}>{t('logout')}</button>
        </div>
      </div>
    </header>
  )
}

export default memo(TopBar)
