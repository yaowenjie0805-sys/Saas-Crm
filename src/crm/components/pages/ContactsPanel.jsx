import { useMemo, useState } from 'react'
import ListState from '../ListState'
import RowDetailDrawer from '../RowDetailDrawer'
import ServerPager from '../ServerPager'

function ContactsPanel({
  activePage,
  t,
  canWrite,
  contactForm,
  setContactForm,
  saveContact,
  formError,
  fieldErrors,
  contacts,
  editContact,
  removeContact,
  loading,
  contactQ,
  setContactQ,
  pagination,
  onPageChange,
  onSizeChange,
  reload,
}) {
  const [sortBy, setSortBy] = useState('nameAsc')
  const [detail, setDetail] = useState(null)
  const firstFieldError = fieldErrors?.customerId || fieldErrors?.name || fieldErrors?.title || fieldErrors?.phone || fieldErrors?.email || ''

  const toggleSort = (key) => {
    setSortBy((prev) => {
      if (prev === `${key}Asc`) return `${key}Desc`
      return `${key}Asc`
    })
  }

  const rows = useMemo(() => {
    const sorted = [...(contacts || [])]
    if (sortBy === 'nameAsc') sorted.sort((a, b) => String(a.name || '').localeCompare(String(b.name || '')))
    if (sortBy === 'nameDesc') sorted.sort((a, b) => String(b.name || '').localeCompare(String(a.name || '')))
    if (sortBy === 'customerAsc') sorted.sort((a, b) => String(a.customerId || '').localeCompare(String(b.customerId || '')))
    if (sortBy === 'customerDesc') sorted.sort((a, b) => String(b.customerId || '').localeCompare(String(a.customerId || '')))
    return sorted
  }, [contacts, sortBy])

  const page = pagination?.page || 1
  const totalPages = Math.max(1, pagination?.totalPages || 1)

  if (activePage !== 'contacts') return null

  return (
    <section className="panel">
      <div className="panel-head"><h2>{t('contacts')}</h2></div>
      <div className="inline-tools" style={{ marginBottom: 10 }}>
        <input className={`tool-input ${fieldErrors?.customerId ? 'input-invalid' : ''}`.trim()} placeholder={t('customerId')} value={contactForm.customerId} onChange={(e) => setContactForm((p) => ({ ...p, customerId: e.target.value }))} />
        <input className={`tool-input ${fieldErrors?.name ? 'input-invalid' : ''}`.trim()} placeholder={t('contactName')} value={contactForm.name} onChange={(e) => setContactForm((p) => ({ ...p, name: e.target.value }))} />
        <input className={`tool-input ${fieldErrors?.title ? 'input-invalid' : ''}`.trim()} placeholder={t('title')} value={contactForm.title} onChange={(e) => setContactForm((p) => ({ ...p, title: e.target.value }))} />
        <input className={`tool-input ${fieldErrors?.phone ? 'input-invalid' : ''}`.trim()} placeholder={t('phone')} value={contactForm.phone} onChange={(e) => setContactForm((p) => ({ ...p, phone: e.target.value }))} />
        <input className={`tool-input ${fieldErrors?.email ? 'input-invalid' : ''}`.trim()} placeholder={t('email')} value={contactForm.email} onChange={(e) => setContactForm((p) => ({ ...p, email: e.target.value }))} />
        <button className="mini-btn" disabled={!canWrite} onClick={saveContact}>{contactForm.id ? t('save') : t('create')}</button>
        <button className="mini-btn" onClick={() => setContactForm({ id: '', customerId: '', name: '', title: '', phone: '', email: '' })}>{t('reset')}</button>
      </div>
      {firstFieldError && <div className="field-error" style={{ marginBottom: 10 }}>{firstFieldError}</div>}
      {formError && <div className="form-error" style={{ marginBottom: 10 }}>{formError}</div>}
      <div className="inline-tools" style={{ marginBottom: 10 }}>
        <input className="tool-input" placeholder={t('search')} value={contactQ} onChange={(e) => setContactQ(e.target.value)} />
        <button className="mini-btn" onClick={() => onPageChange(1)}>{t('search')}</button>
        <button className="mini-btn" onClick={() => reload(page)}>{t('refresh')}</button>
      </div>
      <div className="table-row table-head-row">
        <button className="table-head-btn" onClick={() => toggleSort('customer')}>{t('customerId')}</button>
        <button className="table-head-btn" onClick={() => toggleSort('name')}>{t('contactName')}</button>
        <span>{t('title')}</span>
        <span>{t('phone')}</span>
        <span>{t('action')}</span>
      </div>
      <ListState loading={loading} empty={!loading && rows.length === 0} emptyText={t('noData')} />
      {!loading && rows.map((c) => <div key={c.id} className="table-row"><span>{c.customerId}</span><span>{c.name}</span><span>{c.title || '-'}</span><span>{c.phone || '-'}</span><span><button className="mini-btn" onClick={() => setDetail(c)}>{t('detail')}</button><button className="mini-btn" onClick={() => editContact(c)}>{t('save')}</button><button className="danger-btn" onClick={() => removeContact(c.id)}>{t('delete')}</button></span></div>)}
      {!loading && rows.length > 0 && <ServerPager t={t} page={page} totalPages={totalPages} size={pagination?.size || 8} onPageChange={onPageChange} onSizeChange={onSizeChange} />}
      <RowDetailDrawer open={!!detail} title={t('contacts')} t={t} onClose={() => setDetail(null)} rows={[
        { label: t('idLabel'), value: detail?.id },
        { label: t('customerId'), value: detail?.customerId },
        { label: t('contactName'), value: detail?.name },
        { label: t('title'), value: detail?.title },
        { label: t('phone'), value: detail?.phone },
        { label: t('email'), value: detail?.email },
      ]} />
    </section>
  )
}

export default ContactsPanel
