import { memo, useMemo, useState } from 'react'
import { api, formatMoney, OPPORTUNITY_STAGE_OPTIONS, translateStage, translateStatus } from '../../shared'
import ListState from '../ListState'
import RowDetailDrawer from '../RowDetailDrawer'
import ServerPager from '../ServerPager'
import { useBatchActions } from '../useBatchActions'
import BatchResultModal from '../BatchResultModal'
import VirtualListTable from '../VirtualListTable'
import { useSelectionSet } from '../../hooks/useSelectionSet'

const OpportunityRow = memo(function OpportunityRow({
  row,
  checked,
  onToggle,
  t,
  openDetail,
  editOpportunity,
  canDeleteOpportunity,
  removeOpportunity,
}) {
  return (
    <div className="table-row table-row-6">
      <span><input type="checkbox" checked={checked} onChange={onToggle} /></span>
      <span>{translateStage(t, row.stage)}</span>
      <span>{row.owner}</span>
      <span>{row.progress}%</span>
      <span>{formatMoney(row.amount)}</span>
      <span>
        <button className="mini-btn" onClick={() => openDetail(row)}>{t('detail')}</button>
        <button className="mini-btn" onClick={() => editOpportunity(row)}>{t('save')}</button>
        {canDeleteOpportunity ? <button className="danger-btn" onClick={() => removeOpportunity(row.id)}>{t('delete')}</button> : null}
      </span>
    </div>
  )
})

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
  onRefresh,
  createQuoteFromOpportunity,
  viewOrdersFromOpportunity,
  loadTimeline,
  timeline,
  quickCreateFollowUp,
  quickCreateTask,
  quickUrgeApproval,
  apiContext,
}) {
  const [sortBy, setSortBy] = useState('amountDesc')
  const [detail, setDetail] = useState(null)
  const [batchOwner, setBatchOwner] = useState('')
  const [batchStage, setBatchStage] = useState('')
  const [batchModalOpen, setBatchModalOpen] = useState(false)
  const [timelineLoading, setTimelineLoading] = useState(false)
  const token = apiContext?.token
  const lang = apiContext?.lang || 'en'
  const { summary: batchSummary, toastMessage: batchMessage, runBatch, clearSummary } = useBatchActions({ t })
  const refreshSelf = onRefresh

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
  const selection = useSelectionSet(rows, (row) => row.id)
  const { selectedIds, selectedCount, allChecked, clearSelection, selectPage, toggleAll, toggleOne } = selection
  const byId = useMemo(() => new Map((opportunities || []).map((row) => [row.id, row])), [opportunities])

  const updateOne = async (id, patch) => {
    const row = byId.get(id)
    if (!row) return
    const payload = {
      stage: String(patch.stage ?? row.stage ?? '').trim(),
      count: Number(row.count || 0),
      amount: Number(row.amount || 0),
      progress: Number(row.progress || 0),
      owner: String(patch.owner ?? row.owner ?? '').trim(),
    }
    await api('/opportunities/' + id, { method: 'PATCH', body: JSON.stringify(payload) }, token, lang)
  }

  const batchDelete = async () => {
    if (!canDeleteOpportunity) return
    const result = await runBatch({
      ids: [...selectedIds],
      worker: (id) => api('/opportunities/' + id, { method: 'DELETE' }, token, lang),
      batch: { path: '/v1/opportunities/batch-actions', action: 'DELETE', token, lang },
      canRun: canDeleteOpportunity,
    })
    if (result?.failed > 0) setBatchModalOpen(true)
    clearSelection()
    await refreshSelf()
  }

  const batchAssign = async () => {
    if (!batchOwner.trim()) return
    const result = await runBatch({
      ids: [...selectedIds],
      worker: (id) => updateOne(id, { owner: batchOwner.trim() }),
      batch: { path: '/v1/opportunities/batch-actions', action: 'ASSIGN_OWNER', payload: { owner: batchOwner.trim() }, token, lang },
    })
    if (result?.failed > 0) setBatchModalOpen(true)
    clearSelection()
    await refreshSelf()
  }

  const batchChangeStage = async () => {
    if (!batchStage) return
    const result = await runBatch({
      ids: [...selectedIds],
      worker: (id) => updateOne(id, { stage: batchStage }),
      batch: { path: '/v1/opportunities/batch-actions', action: 'UPDATE_STATUS', payload: { stage: batchStage }, token, lang },
    })
    if (result?.failed > 0) setBatchModalOpen(true)
    clearSelection()
    await refreshSelf()
  }

  const openDetail = async (row) => {
    setDetail(row)
    if (!loadTimeline || !row?.id) return
    setTimelineLoading(true)
    try {
      await loadTimeline(row.id)
    } catch {
      // timeline loading errors are surfaced by global banner
    } finally {
      setTimelineLoading(false)
    }
  }

  if (activePage !== 'pipeline') return null

  return (
    <section className="panel">
      <div className="panel-head"><h2>{t('pipeline')}</h2></div>
      <div className="inline-tools filter-row" style={{ marginBottom: 10 }}>
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
      <div className="inline-tools filter-row" style={{ marginBottom: 8 }}>
        <select className="tool-input" value={oppStage} onChange={(e) => setOppStage(e.target.value)}>
          <option value="">{t('selectPlaceholder')}</option>
          {OPPORTUNITY_STAGE_OPTIONS.map((stage) => <option key={stage} value={stage}>{translateStage(t, stage)}</option>)}
        </select>
      </div>
      <div className="inline-tools filter-bar" style={{ marginBottom: 10 }}>
        <button className="mini-btn" onClick={() => onPageChange(1)}>{t('search')}</button>
        <button className="mini-btn" onClick={refreshSelf}>{t('refresh')}</button>
      </div>
      <div className="inline-tools filter-bar" style={{ marginBottom: 10 }}>
        <span className="muted-filter">{t('batchSelectedCount')}: {selectedCount}</span>
        <button className="mini-btn" onClick={selectPage}>{t('selectPage')}</button>
        <button className="mini-btn" onClick={clearSelection}>{t('clearSelection')}</button>
        <input className="tool-input" placeholder={t('batchOwnerPlaceholder')} value={batchOwner} onChange={(e) => setBatchOwner(e.target.value)} />
        <select className="tool-input" value={batchStage} onChange={(e) => setBatchStage(e.target.value)}>
          <option value="">{t('batchSetStage')}</option>
          {OPPORTUNITY_STAGE_OPTIONS.map((stage) => <option key={stage} value={stage}>{translateStage(t, stage)}</option>)}
        </select>
        <button className="mini-btn" disabled={!canWrite} onClick={batchAssign}>{t('batchAssignOwner')}</button>
        <button className="mini-btn" disabled={!canWrite} onClick={batchChangeStage}>{t('batchSetStage')}</button>
        {canDeleteOpportunity && <button className="danger-btn" onClick={batchDelete}>{t('batchDelete')}</button>}
        {batchSummary?.failed > 0 && <button className="mini-btn" onClick={() => setBatchModalOpen(true)}>{t('batchResultTitle')}</button>}
      </div>
      {batchMessage && <div className="info-banner" style={{ marginBottom: 8 }}>{batchMessage}</div>}
      <div className="table-row table-head-row table-row-6">
        <span><input type="checkbox" checked={allChecked} onChange={(e) => toggleAll(e.target.checked)} /></span>
        <span>{t('stage')}</span>
        <span>{t('owner')}</span>
        <button className="table-head-btn" onClick={() => toggleSort('progress')}>{t('progress')}</button>
        <button className="table-head-btn" onClick={() => toggleSort('amount')}>{t('amount')}</button>
        <span>{t('action')}</span>
      </div>
      <ListState loading={loading} empty={!loading && rows.length === 0} emptyText={t('noData')} />
      {!loading && (
        <VirtualListTable
          rows={rows}
          viewportHeight={460}
          getRowKey={(row) => row.id}
          renderRow={(row) => (
            <OpportunityRow
              key={row.id}
              row={row}
              checked={selectedIds.has(row.id)}
              onToggle={(e) => toggleOne(row.id, e.target.checked)}
              t={t}
              openDetail={openDetail}
              editOpportunity={editOpportunity}
              canDeleteOpportunity={canDeleteOpportunity}
              removeOpportunity={removeOpportunity}
            />
          )}
        />
      )}
      {!loading && rows.length > 0 && <ServerPager t={t} page={page} totalPages={totalPages} size={pagination?.size || 8} onPageChange={onPageChange} onSizeChange={onSizeChange} />}
      <RowDetailDrawer open={!!detail} title={t('pipeline')} t={t} onClose={() => setDetail(null)} rows={[
        { label: t('idLabel'), value: detail?.id },
        { label: t('stage'), value: translateStage(t, detail?.stage) },
        { label: t('owner'), value: detail?.owner },
        { label: t('progress'), value: detail ? `${detail.progress}%` : '-' },
        { label: t('amount'), value: detail ? formatMoney(detail.amount) : '-' },
      ]} actions={[
        { label: t('quickCreateQuote'), onClick: () => detail && createQuoteFromOpportunity && createQuoteFromOpportunity(detail) },
        { label: t('quickViewOrders'), onClick: () => detail && viewOrdersFromOpportunity && viewOrdersFromOpportunity(detail) },
        { label: t('quickCreateFollowUp'), onClick: () => detail && quickCreateFollowUp && quickCreateFollowUp(detail) },
        { label: t('quickCreateTask'), onClick: () => detail && quickCreateTask && quickCreateTask(detail) },
        { label: t('quickUrgeApproval'), onClick: () => detail && quickUrgeApproval && quickUrgeApproval(detail) },
      ]} extra={
        <div className="drawer-timeline">
          <h4>{t('timeline')}</h4>
          {timelineLoading && <div className="empty-tip">{t('loading')}</div>}
          {!timelineLoading && !(timeline || []).length && <div className="empty-tip">{t('noData')}</div>}
          {!timelineLoading && (timeline || []).map((item, idx) => (
            <div key={`${item.sourceId || idx}-${idx}`} className="drawer-timeline-item">
              <div>{item.title}</div>
              <small>{item.time ? String(item.time).replace('T', ' ').slice(0, 16) : '-'} · {translateStatus(t, item.status)}</small>
            </div>
          ))}
        </div>
      } />
      <BatchResultModal t={t} open={batchModalOpen} summary={batchSummary} onClose={() => { setBatchModalOpen(false); clearSummary() }} />
    </section>
  )
}

export default PipelinePanel
