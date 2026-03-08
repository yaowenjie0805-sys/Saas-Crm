import { useMemo, useState } from 'react'
import { formatMoney, OPPORTUNITY_STAGE_OPTIONS, translateStage } from '../../shared'
import ListState from '../ListState'
import RowDetailDrawer from '../RowDetailDrawer'
import ServerPager from '../ServerPager'

function PipelinePanel({
  activePage,
  t,
  canWrite,
  opportunityForm,
  setOpportunityForm,
  saveOpportunity,
  formError,
  fieldErrors,
  opportunities,
  editOpportunity,
  canDeleteOpportunity,
  removeOpportunity,
  loading,
  oppStage,
  setOppStage,
  pagination,
  onPageChange,
  onSizeChange,
  reload,
}) {
  const [sortBy, setSortBy] = useState('amountDesc')
  const [detail, setDetail] = useState(null)

  const toggleSort = (key) => {
    setSortBy((prev) => {
      if (prev === `${key}Asc`) return `${key}Desc`
      return `${key}Asc`
    })
  }

  const rows = useMemo(() => {
    const sorted = [...(opportunities || [])]
    if (sortBy === 'amountDesc') sorted.sort((a, b) => Number(b.amount || 0) - Number(a.amount || 0))
    if (sortBy === 'amountAsc') sorted.sort((a, b) => Number(a.amount || 0) - Number(b.amount || 0))
    if (sortBy === 'progressDesc') sorted.sort((a, b) => Number(b.progress || 0) - Number(a.progress || 0))
    if (sortBy === 'progressAsc') sorted.sort((a, b) => Number(a.progress || 0) - Number(b.progress || 0))
    return sorted
  }, [opportunities, sortBy])

  const page = pagination?.page || 1
  const totalPages = Math.max(1, pagination?.totalPages || 1)

  if (activePage !== 'pipeline') return null

  return (
    <section className="panel">
      <div className="panel-head"><h2>{t('pipeline')}</h2></div>
      <div className="inline-tools" style={{ marginBottom: 10 }}>
        <select className={fieldErrors?.stage ? 'tool-input input-invalid' : 'tool-input'} value={opportunityForm.stage} onChange={(e) => setOpportunityForm((p) => ({ ...p, stage: e.target.value }))}>
          <option value="">{t('selectPlaceholder')}</option>
          {OPPORTUNITY_STAGE_OPTIONS.map((stage) => <option key={stage} value={stage}>{translateStage(t, stage)}</option>)}
        </select>
        <input className={fieldErrors?.progress ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('progress')} value={opportunityForm.progress} onChange={(e) => setOpportunityForm((p) => ({ ...p, progress: e.target.value }))} />
        <input className={fieldErrors?.amount ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('amount')} value={opportunityForm.amount} onChange={(e) => setOpportunityForm((p) => ({ ...p, amount: e.target.value }))} />
        <input className={fieldErrors?.owner ? 'tool-input input-invalid' : 'tool-input'} placeholder={t('owner')} value={opportunityForm.owner} onChange={(e) => setOpportunityForm((p) => ({ ...p, owner: e.target.value }))} />
        <button className="mini-btn" disabled={!canWrite} onClick={saveOpportunity}>{opportunityForm.id ? t('save') : t('create')}</button>
        <button className="mini-btn" onClick={() => setOpportunityForm({ id: '', stage: '', count: '', amount: '', progress: '', owner: '' })}>{t('reset')}</button>
      </div>
      {(fieldErrors?.stage || fieldErrors?.count || fieldErrors?.amount || fieldErrors?.progress || fieldErrors?.owner) && (
        <div className="field-error" style={{ marginBottom: 8 }}>
          {fieldErrors?.stage || fieldErrors?.count || fieldErrors?.amount || fieldErrors?.progress || fieldErrors?.owner}
        </div>
      )}
      {formError && <div className="field-error" style={{ marginBottom: 8 }}>{formError}</div>}
      <div className="inline-tools" style={{ marginBottom: 10 }}>
        <select className="tool-input" value={oppStage} onChange={(e) => setOppStage(e.target.value)}>
          <option value="">{t('selectPlaceholder')}</option>
          {OPPORTUNITY_STAGE_OPTIONS.map((stage) => <option key={stage} value={stage}>{translateStage(t, stage)}</option>)}
        </select>
        <button className="mini-btn" onClick={() => onPageChange(1)}>{t('search')}</button>
        <button className="mini-btn" onClick={() => reload(page)}>{t('refresh')}</button>
      </div>
      <div className="table-row table-head-row">
        <span>{t('stage')}</span>
        <span>{t('owner')}</span>
        <button className="table-head-btn" onClick={() => toggleSort('progress')}>{t('progress')}</button>
        <button className="table-head-btn" onClick={() => toggleSort('amount')}>{t('amount')}</button>
        <span>{t('action')}</span>
      </div>
      <ListState loading={loading} empty={!loading && rows.length === 0} emptyText={t('noData')} />
      {!loading && rows.map((o) => <div key={o.id} className="table-row"><span>{translateStage(t, o.stage)}</span><span>{o.owner}</span><span>{o.progress}%</span><span>{formatMoney(o.amount)}</span><span><button className="mini-btn" onClick={() => setDetail(o)}>{t('detail')}</button><button className="mini-btn" onClick={() => editOpportunity(o)}>{t('save')}</button>{canDeleteOpportunity ? <button className="danger-btn" onClick={() => removeOpportunity(o.id)}>{t('delete')}</button> : null}</span></div>)}
      {!loading && rows.length > 0 && <ServerPager t={t} page={page} totalPages={totalPages} size={pagination?.size || 8} onPageChange={onPageChange} onSizeChange={onSizeChange} />}
      <RowDetailDrawer open={!!detail} title={t('pipeline')} t={t} onClose={() => setDetail(null)} rows={[
        { label: t('idLabel'), value: detail?.id },
        { label: t('stage'), value: translateStage(t, detail?.stage) },
        { label: t('owner'), value: detail?.owner },
        { label: t('progress'), value: detail ? `${detail.progress}%` : '-' },
        { label: t('amount'), value: detail ? formatMoney(detail.amount) : '-' },
      ]} />
    </section>
  )
}

export default PipelinePanel
