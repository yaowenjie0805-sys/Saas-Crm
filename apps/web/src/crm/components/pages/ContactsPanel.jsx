import { memo, useState } from 'react'
import ListState from '../ListState'
import RowDetailDrawer from '../RowDetailDrawer'
import ServerPager from '../ServerPager'
import VirtualListTable from '../VirtualListTable'
import { useSelectionSet } from '../../hooks/useSelectionSet'

const ContactRow = memo(function ContactRow({
  row,
  checked,
  onToggle,
  t,
  setDetail,
  editContact,
  removeContact,
}) {
  return (
    <div className="table-row table-row-6">
      <span><input type="checkbox" checked={checked} onChange={onToggle} /></span>
      <span>{row.customerId}</span>
      <span>{row.name}</span>
      <span>{row.title || '-'}</span>
      <span>{row.phone || '-'}</span>
      <span>
        <button className="mini-btn" onClick={() => setDetail(row)}>{t('detail')}</button>
        <button className="mini-btn" onClick={() => editContact(row)}>{t('save')}</button>
        <button className="danger-btn" onClick={() => removeContact(row.id)}>{t('delete')}</button>
      </span>
    </div>
  )
})

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
  onRefresh,
}) {
  const [detail, setDetail] = useState(null)
  const firstFieldError = fieldErrors?.customerId || fieldErrors?.name || fieldErrors?.title || fieldErrors?.phone || fieldErrors?.email || ''

  const rows = contacts || []

  const page = pagination?.page || 1
  const totalPages = Math.max(1, pagination?.totalPages || 1)
  const { selectedIds, selectedCount, allChecked, clearSelection, selectPage, toggleAll, toggleOne } = useSelectionSet(rows, (row) => row.id)
  const refreshSelf = onRefresh

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
      <div className="inline-tools filter-row" style={{ marginBottom: 8 }}>
        <input className="tool-input" placeholder={t('search')} value={contactQ} onChange={(e) => setContactQ(e.target.value)} />
      </div>
      <div className="inline-tools filter-bar" style={{ marginBottom: 10 }}>
        <button className="mini-btn" onClick={() => onPageChange(1)}>{t('search')}</button>
        <button className="mini-btn" onClick={refreshSelf}>{t('refresh')}</button>
      </div>
      <div className="inline-tools filter-bar" style={{ marginBottom: 10 }}>
        <span className="muted-filter">{t('batchSelectedCount')}: {selectedCount}</span>
        <button className="mini-btn" onClick={selectPage}>{t('selectPage')}</button>
        <button className="mini-btn" onClick={clearSelection}>{t('clearSelection')}</button>
      </div>
      <div className="table-row table-head-row table-row-6">
        <span><input type="checkbox" checked={allChecked} onChange={(e) => toggleAll(e.target.checked)} /></span>
        <span>{t('customerId')}</span>
        <span>{t('contactName')}</span>
        <span>{t('title')}</span>
        <span>{t('phone')}</span>
        <span>{t('action')}</span>
      </div>
      <ListState loading={loading} empty={!loading && rows.length === 0} emptyText={t('noData')} />
      {!loading && rows.length > 0 && (
        <VirtualListTable
          rows={rows}
          viewportHeight={460}
          getRowKey={(row) => row.id}
          renderRow={(row) => (
            <ContactRow
              key={row.id}
              row={row}
              checked={selectedIds.has(row.id)}
              onToggle={(e) => toggleOne(row.id, e.target.checked)}
              t={t}
              setDetail={setDetail}
              editContact={editContact}
              removeContact={removeContact}
            />
          )}
        />
      )}
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

export default memo(ContactsPanel)
