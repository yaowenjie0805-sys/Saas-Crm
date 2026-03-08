function LoginView({
  lang,
  setLang,
  t,
  submitLogin,
  loginForm,
  setLoginForm,
  formErrors,
  setFormErrors,
  submitSsoLogin,
  ssoConfig,
  ssoForm,
  setSsoForm,
  oidcAuthorizing,
  startOidcLogin,
  openActivate,
  error,
}) {
  return (
    <div className="login-wrap">
      <div className="language-switch floating">
        <button className={lang === 'zh' ? 'active' : ''} onClick={() => setLang('zh')}>ZH</button>
        <button className={lang === 'en' ? 'active' : ''} onClick={() => setLang('en')}>EN</button>
      </div>
      <div className="login-card">
        <form onSubmit={submitLogin} style={{ display: 'grid', gap: 12 }}>
          <h1>{t('loginTitle')}</h1>
          <p>{t('loginHint')}</p>
          <div>
            <input
              className={formErrors.login.tenantId ? 'input-invalid' : ''}
              placeholder={t('tenantId')}
              value={loginForm.tenantId}
              onChange={(e) => {
                const value = e.target.value
                setLoginForm((p) => ({ ...p, tenantId: value }))
                if (value.trim()) setFormErrors((p) => ({ ...p, login: { ...p.login, tenantId: '' } }))
              }}
            />
            {formErrors.login.tenantId && <div className="field-error">{formErrors.login.tenantId}</div>}
          </div>
          <div>
            <input
              className={formErrors.login.username ? 'input-invalid' : ''}
              placeholder={t('username')}
              value={loginForm.username}
              onChange={(e) => {
                const value = e.target.value
                setLoginForm((p) => ({ ...p, username: value }))
                if (value.trim()) setFormErrors((p) => ({ ...p, login: { ...p.login, username: '' } }))
              }}
              autoComplete="username"
            />
            {formErrors.login.username && <div className="field-error">{formErrors.login.username}</div>}
          </div>
          <div>
            <input
              type="password"
              className={formErrors.login.password ? 'input-invalid' : ''}
              placeholder={t('password')}
              value={loginForm.password}
              onChange={(e) => {
                const value = e.target.value
                setLoginForm((p) => ({ ...p, password: value }))
                if (value.trim()) setFormErrors((p) => ({ ...p, login: { ...p.login, password: '' } }))
              }}
              autoComplete="current-password"
            />
            {formErrors.login.password && <div className="field-error">{formErrors.login.password}</div>}
          </div>
          <input
            placeholder={t('mfaCode')}
            value={loginForm.mfaCode}
            onChange={(e) => setLoginForm((p) => ({ ...p, mfaCode: e.target.value }))}
            autoComplete="one-time-code"
          />
          <button className="primary-btn" type="submit">{t('login')}</button>
          <button className="mini-btn" type="button" onClick={openActivate}>{t('inviteActivateBtn')}</button>
        </form>
        {ssoConfig?.enabled && (
          ssoConfig.mode === 'oidc' ? (
            <div style={{ display: 'grid', gap: 12, marginTop: 16, paddingTop: 16, borderTop: '1px solid var(--line)' }}>
              <strong style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>{t('ssoProvider')}: {ssoConfig.providerName || t('ssoProviderDefault')}</strong>
              <button className="primary-btn" type="button" onClick={startOidcLogin} disabled={oidcAuthorizing}>
                {oidcAuthorizing ? t('ssoAuthorizing') : t('ssoRedirect')}
              </button>
            </div>
          ) : (
            <form onSubmit={submitSsoLogin} style={{ display: 'grid', gap: 12, marginTop: 16, paddingTop: 16, borderTop: '1px solid var(--line)' }}>
              <strong style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>{t('ssoProvider')}: {ssoConfig.providerName || t('ssoProviderDefault')}</strong>
              <div>
                <input
                  className={formErrors.sso.username ? 'input-invalid' : ''}
                  placeholder={t('ssoUser')}
                  value={ssoForm.username}
                  onChange={(e) => {
                    const value = e.target.value
                    setSsoForm((p) => ({ ...p, username: value }))
                    if (value.trim()) setFormErrors((p) => ({ ...p, sso: { ...p.sso, username: '' } }))
                  }}
                />
                {formErrors.sso.username && <div className="field-error">{formErrors.sso.username}</div>}
              </div>
              <div>
                <input
                  className={formErrors.sso.code ? 'input-invalid' : ''}
                  placeholder={t('ssoCode')}
                  value={ssoForm.code}
                  onChange={(e) => {
                    const value = e.target.value
                    setSsoForm((p) => ({ ...p, code: value }))
                    if (value.trim()) setFormErrors((p) => ({ ...p, sso: { ...p.sso, code: '' } }))
                  }}
                />
                {formErrors.sso.code && <div className="field-error">{formErrors.sso.code}</div>}
              </div>
              <button className="primary-btn" type="submit">{t('ssoLogin')}</button>
            </form>
          )
        )}
        {error && <div className="error-banner" style={{ marginTop: 12 }}>{error}</div>}
      </div>
    </div>
  )
}

export default LoginView
