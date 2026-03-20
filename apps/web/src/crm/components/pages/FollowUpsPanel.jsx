import { memo, useMemo } from 'react'
import { FOLLOW_UP_CHANNEL_OPTIONS, translateChannel } from '../../shared'
import VirtualListTable from '../VirtualListTable'

const FollowUpRow = memo(function FollowUpRow({
  row,
  t,
  editFollowUp,
  removeFollowUp,
}) {
  return (
    <div className="table-row table-row-5">
      <span>{row.customerId}</span>
      <span>{row.summary}</span>
      <span>{translateChannel(t, row.channel)}</span>
      <span>{row.nextActionDate || '-'}</span>
      <span>
        <button className="mini-btn" onClick={() => editFollowUp(row)}>{t('save')}</button>
        <button className="danger-btn" onClick={() => removeFollowUp(row.id)}>{t('delete')}</button>
      </span>
    </div>
  )
})

function FollowUpsPanel({
  activePage,
  t,
  canWrite,
  followUpForm,
  setFollowUpForm,
  saveFollowUp,
  formError,
  fieldErrors,
  followUps,
  editFollowUp,
  removeFollowUp,
  followCustomerId,
  setFollowCustomerId,
  followQ,
  setFollowQ,
  onRefresh,
}) {
  const rows = useMemo(() => (followUps || []), [followUps])
  if (activePage !== 'followUps') return null

  const resetFilters = () => {
    setFollowCustomerId('')
    setFollowQ('')
  }
  return (
    <section className="panel">
      <div className="panel-head"><h2>{t('followUps')}</h2></div>
      <div className="inline-tools filter-row" style={{ marginBottom: 8 }}>
        <input className="tool-input" placeholder={t('customerId')} value={followCustomerId || ''} onChange={(e) => setFollowCustomerId(e.target.value)} />
        <input className="tool-input" placeholder={t('search')} value={followQ || ''} onChange={(e) => setFollowQ(e.target.value)} />
      </div>
      <div className="inline-tools filter-bar" style={{ marginBottom: 10 }}>
        <button className="mini-btn" onClick={() => onRefresh?.()}>{t('query')}</button>
        <button className="mini-btn" onClick={resetFilters}>{t('clearFilters')}</button>
      </div>
      <div className="inline-tools filter-row" style={{ marginBottom: 10 }}>
        <input className={fieldErrors?.customerId ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('customerId')} value={followUpForm.customerId} onChange={(e) => setFollowUpForm((p) => ({ ...p, customerId: e.target.value }))} />
        <input className={fieldErrors?.summary ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('summary')} value={followUpForm.summary} onChange={(e) => setFollowUpForm((p) => ({ ...p, summary: e.target.value }))} />
        <select className={fieldErrors?.channel ? 'tool-input input-invalid' : 'tool-input'} value={followUpForm.channel} onChange={(e) => setFollowUpForm((p) => ({ ...p, channel: e.target.value }))}>
          <option value="">{t('selectPlaceholder')}</option>
          {FOLLOW_UP_CHANNEL_OPTIONS.map((channel) => <option key={channel} value={channel}>{translateChannel(t, channel)}</option>)}
        </select>
        <input className={fieldErrors?.nextActionDate ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('dateFormatHint')} value={followUpForm.nextActionDate} onChange={(e) => setFollowUpForm((p) => ({ ...p, nextActionDate: e.target.value }))} />
      </div>
      <div className="inline-tools filter-bar" style={{ marginBottom: 10 }}>
        <button className="mini-btn" disabled={!canWrite} onClick={saveFollowUp}>{followUpForm.id ? t('save') : t('create')}</button>
        <button className="mini-btn" onClick={() => setFollowUpForm({ id: '', customerId: '', summary: '', channel: '', result: '', nextActionDate: '' })}>{t('reset')}</button>
      </div>
      {(fieldErrors?.customerId || fieldErrors?.summary || fieldErrors?.channel || fieldErrors?.nextActionDate) && (
        <div className="field-error" style={{ marginBottom: 8 }}>
          {fieldErrors?.customerId || fieldErrors?.summary || fieldErrors?.channel || fieldErrors?.nextActionDate}
        </div>
      )}
      {formError && <div className="field-error" style={{ marginBottom: 8 }}>{formError}</div>}
      <div className="table-row table-head-row table-row-5">
        <span>{t('customerId')}</span>
        <span>{t('summary')}</span>
        <span>{t('channel')}</span>
        <span>{t('nextActionDate')}</span>
        <span>{t('action')}</span>
      </div>
      {rows.length === 0 && <div className="empty-tip">{t('noData')}</div>}
      {rows.length > 0 && (
        <VirtualListTable
          rows={rows}
          viewportHeight={420}
          getRowKey={(row) => row.id}
          renderRow={(row) => (
            <FollowUpRow
              key={row.id}
              row={row}
              t={t}
              editFollowUp={editFollowUp}
              removeFollowUp={removeFollowUp}
            />
          )}
        />
      )}
    </section>
  )
}

export default memo(FollowUpsPanel)
