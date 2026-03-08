import { useMemo, useState } from 'react'
import { api } from '../shared'

function InvitationAcceptView({ lang, setLang, t, onBackToLogin }) {
  const params = useMemo(() => new URLSearchParams(window.location.search), [])
  const tokenFromUrl = params.get('token') || ''
  const [form, setForm] = useState({ token: tokenFromUrl, password: '', confirmPassword: '', displayName: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    if (!form.token.trim() || !form.password.trim() || !form.confirmPassword.trim()) {
      setError(t('fieldRequired'))
      return
    }
    if (form.password !== form.confirmPassword) {
      setError(t('passwordNotMatch'))
      return
    }
    try {
      setLoading(true)
      await api('/v1/auth/invitations/accept', {
        method: 'POST',
        body: JSON.stringify({
          token: form.token.trim(),
          password: form.password,
          confirmPassword: form.confirmPassword,
          displayName: form.displayName.trim(),
        }),
      }, null, lang)
      setSuccess(t('inviteActivateSuccess'))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-wrap">
      <div className="language-switch floating">
        <button className={lang === 'zh' ? 'active' : ''} onClick={() => setLang('zh')}>ZH</button>
        <button className={lang === 'en' ? 'active' : ''} onClick={() => setLang('en')}>EN</button>
      </div>
      <div className="login-card">
        <form onSubmit={submit} style={{ display: 'grid', gap: 12 }}>
          <h1>{t('inviteActivateTitle')}</h1>
          <p>{t('inviteActivateHint')}</p>
          <input placeholder={t('inviteToken')} value={form.token} onChange={(e) => setForm((p) => ({ ...p, token: e.target.value }))} />
          <input type="password" placeholder={t('password')} value={form.password} onChange={(e) => setForm((p) => ({ ...p, password: e.target.value }))} autoComplete="new-password" />
          <input type="password" placeholder={t('confirmPassword')} value={form.confirmPassword} onChange={(e) => setForm((p) => ({ ...p, confirmPassword: e.target.value }))} autoComplete="new-password" />
          <input placeholder={t('displayName')} value={form.displayName} onChange={(e) => setForm((p) => ({ ...p, displayName: e.target.value }))} autoComplete="name" />
          <button className="primary-btn" type="submit" disabled={loading}>{loading ? t('loading') : t('inviteActivateBtn')}</button>
          <button className="mini-btn" type="button" onClick={onBackToLogin}>{t('toLogin')}</button>
        </form>
        {error && <div className="error-banner" style={{ marginTop: 12 }}>{error}</div>}
        {success && <div className="info-banner" style={{ marginTop: 12 }}>{success}</div>}
      </div>
    </div>
  )
}

export default InvitationAcceptView
