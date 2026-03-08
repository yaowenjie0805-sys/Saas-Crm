import { FOLLOW_UP_CHANNEL_OPTIONS, translateChannel } from '../../shared'

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
}) {
  if (activePage !== 'followUps') return null

  return (
    <section className="panel">
      <div className="panel-head"><h2>{t('followUps')}</h2></div>
      <div className="inline-tools" style={{ marginBottom: 10 }}>
        <input className={fieldErrors?.customerId ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('customerId')} value={followUpForm.customerId} onChange={(e) => setFollowUpForm((p) => ({ ...p, customerId: e.target.value }))} />
        <input className={fieldErrors?.summary ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('summary')} value={followUpForm.summary} onChange={(e) => setFollowUpForm((p) => ({ ...p, summary: e.target.value }))} />
        <select className={fieldErrors?.channel ? 'tool-input input-invalid' : 'tool-input'} value={followUpForm.channel} onChange={(e) => setFollowUpForm((p) => ({ ...p, channel: e.target.value }))}>
          <option value="">{t('selectPlaceholder')}</option>
          {FOLLOW_UP_CHANNEL_OPTIONS.map((channel) => <option key={channel} value={channel}>{translateChannel(t, channel)}</option>)}
        </select>
        <input className={fieldErrors?.nextActionDate ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('dateFormatHint')} value={followUpForm.nextActionDate} onChange={(e) => setFollowUpForm((p) => ({ ...p, nextActionDate: e.target.value }))} />
        <button className="mini-btn" disabled={!canWrite} onClick={saveFollowUp}>{followUpForm.id ? t('save') : t('create')}</button>
        <button className="mini-btn" onClick={() => setFollowUpForm({ id: '', customerId: '', summary: '', channel: '', result: '', nextActionDate: '' })}>{t('reset')}</button>
      </div>
      {(fieldErrors?.customerId || fieldErrors?.summary || fieldErrors?.channel || fieldErrors?.nextActionDate) && (
        <div className="field-error" style={{ marginBottom: 8 }}>
          {fieldErrors?.customerId || fieldErrors?.summary || fieldErrors?.channel || fieldErrors?.nextActionDate}
        </div>
      )}
      {formError && <div className="field-error" style={{ marginBottom: 8 }}>{formError}</div>}
      {followUps.map((f) => <div key={f.id} className="table-row"><span>{f.customerId}</span><span>{f.summary}</span><span>{translateChannel(t, f.channel)}</span><span>{f.nextActionDate || '-'}</span><span><button className="mini-btn" onClick={() => editFollowUp(f)}>{t('save')}</button><button className="danger-btn" onClick={() => removeFollowUp(f.id)}>{t('delete')}</button></span></div>)}
    </section>
  )
}

export default FollowUpsPanel
