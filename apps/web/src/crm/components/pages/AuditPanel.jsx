import { memo } from 'react'
import { ROLES, formatDateTime, translateRole } from '../../shared'
import ServerPager from '../ServerPager'
import VirtualListTable from '../VirtualListTable'

function AuditPanel({
  activePage,
  refreshPage,
  canViewAudit,
  t,
  auditUser,
  setAuditUser,
  auditRole,
  setAuditRole,
  auditAction,
  setAuditAction,
  auditFrom,
  setAuditFrom,
  auditTo,
  setAuditTo,
  auditRangeError,
  setAuditRangeError,
  hasInvalidAuditRange,
  loadAudit,
  createExportJob,
  autoRefreshJobs,
  setAutoRefreshJobs,
  auditLogs,
  exportStatusFilter,
  setExportStatusFilter,
  exportJobs,
  exportJobsPage,
  setExportJobsPage,
  exportJobsTotalPages,
  exportJobsSize,
  setExportJobsSize,
  downloadExportJob,
  retryExportJob,
}) {
  if (!(activePage === 'audit' && canViewAudit)) return null

  const statusLabel = (s) => s === 'PENDING' ? t('exportPending') : s === 'RUNNING' ? t('exportRunning') : s === 'DONE' ? t('exportDone') : s === 'FAILED' ? t('exportFailed') : (s || '-')
  const durationLabel = (job) => {
    const start = job.createdAt ? Date.parse(job.createdAt) : NaN
    const end = job.finishedAt ? Date.parse(job.finishedAt) : start
    if (Number.isNaN(start)) return '-'
    const sec = Math.max(0, Math.floor((end - start) / 1000))
    if (sec < 60) return `${sec}s`
    const min = Math.floor(sec / 60)
    const rem = sec % 60
    return `${min}m ${rem}s`
  }

  const toYmd = (d) => {
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${y}-${m}-${day}`
  }

  const applyQuickRange = (days) => {
    const end = new Date()
    const start = new Date()
    start.setDate(end.getDate() - days + 1)
    setAuditFrom(toYmd(start))
    setAuditTo(toYmd(end))
    setAuditRangeError('')
  }

  const exportJobsFiltered = exportJobs || []
  const refreshSelf = () => refreshPage('audit', 'panel_action')

  return (
    <section className="panel">
      <div className="panel-head">
        <h2>{t('audit')}</h2>
      </div>
      <div className="inline-tools filter-row" style={{ marginBottom: 8 }}>
        <input className="tool-input" placeholder={t('user')} value={auditUser} onChange={(e) => setAuditUser(e.target.value)} />
        <select className="tool-input" value={auditRole} onChange={(e) => setAuditRole(e.target.value)}>
          <option value="">{t('allRoles')}</option>
          {ROLES.map((r) => <option key={r} value={r}>{translateRole(t, r)}</option>)}
        </select>
        <input className="tool-input" placeholder={t('action')} value={auditAction} onChange={(e) => setAuditAction(e.target.value)} />
        <input className={auditRangeError ? 'tool-input input-invalid' : 'tool-input'} type="date" value={auditFrom} onChange={(e) => { setAuditFrom(e.target.value); setAuditRangeError('') }} />
        <input className={auditRangeError ? 'tool-input input-invalid' : 'tool-input'} type="date" value={auditTo} onChange={(e) => { setAuditTo(e.target.value); setAuditRangeError('') }} />
      </div>
      <div className="inline-tools filter-bar" style={{ marginBottom: 8 }}>
        <span className="muted-filter">{t('filters')}: {auditFrom || '-'} ~ {auditTo || '-'} | {t('role')}: {auditRole ? translateRole(t, auditRole) : t('allRoles')}</span>
      </div>
      <div className="inline-tools filter-bar">
          <button className="mini-btn" onClick={() => applyQuickRange(1)}>{t('today')}</button>
          <button className="mini-btn" onClick={() => applyQuickRange(7)}>{t('last7Days')}</button>
          <button className="mini-btn" onClick={() => applyQuickRange(30)}>{t('last30Days')}</button>
          {auditRangeError && <span className="field-error">{auditRangeError}</span>}
          <button className="mini-btn" disabled={hasInvalidAuditRange()} onClick={loadAudit}>{t('search')}</button>
          <button className="mini-btn" disabled={hasInvalidAuditRange()} onClick={createExportJob}>{t('filteredExport')}</button>
          <button className="mini-btn" onClick={refreshSelf}>{t('refresh')}</button>
          <label className="switch-inline"><input type="checkbox" checked={autoRefreshJobs} onChange={(e) => setAutoRefreshJobs(e.target.checked)} />{t('autoRefresh')}</label>
      </div>
      <div className="table-row table-head-row table-row-4 compact">
        <span>{t('action')}</span>
        <span>{t('summary')}</span>
        <span>{t('user')}</span>
        <span>{t('createdAt')}</span>
      </div>
      <VirtualListTable
        rows={auditLogs.slice(0, 8)}
        rowHeight={42}
        viewportHeight={336}
        getRowKey={(row) => row.id}
        renderRow={(log) => (
          <div key={log.id} className="table-row table-row-4 compact">
            <span>{log.action}</span>
            <span>{log.resource}</span>
            <span>{log.username}</span>
            <span>{formatDateTime(log.createdAt)}</span>
          </div>
        )}
      />
      <div className="panel" style={{ marginTop: 10, boxShadow: 'none' }}>
        <div className="panel-head">
          <h2>{t('exportJob')}</h2>
          <div className="export-filter-tabs">
          <button className={`mini-btn ${exportStatusFilter === 'ALL' ? 'active' : ''}`} onClick={() => { setExportStatusFilter('ALL'); setExportJobsPage(1) }}>{t('filterAll')}</button>
            <button className={`mini-btn ${exportStatusFilter === 'RUNNING' ? 'active' : ''}`} onClick={() => { setExportStatusFilter('RUNNING'); setExportJobsPage(1) }}>{t('filterRunning')}</button>
            <button className={`mini-btn ${exportStatusFilter === 'FAILED' ? 'active' : ''}`} onClick={() => { setExportStatusFilter('FAILED'); setExportJobsPage(1) }}>{t('filterFailed')}</button>
            <button className={`mini-btn ${exportStatusFilter === 'DONE' ? 'active' : ''}`} onClick={() => { setExportStatusFilter('DONE'); setExportJobsPage(1) }}>{t('filterDone')}</button>
          </div>
        </div>
        <div className="table-row table-head-row table-row-7 compact">
          <span>{t('idLabel')}</span>
          <span>{t('status')}</span>
          <span>{t('progress')}</span>
          <span>{t('createdAt')}</span>
          <span>{t('duration')}</span>
          <span>{t('filtersSummary')}</span>
          <span>{t('action')}</span>
        </div>
        <VirtualListTable
          rows={exportJobsFiltered}
          rowHeight={44}
          viewportHeight={400}
          getRowKey={(job) => job.jobId}
          renderRow={(job) => (
            <div key={job.jobId} className="table-row table-row-7 compact">
              <span>{job.jobId}</span>
              <span>{statusLabel(job.status)}</span>
              <span>{job.progress || 0}%</span>
              <span>{formatDateTime(job.createdAt)}</span>
              <span>{durationLabel(job)}</span>
              <span>{job.filters ? `${job.filters.username || '-'} | ${job.filters.role ? translateRole(t, job.filters.role) : '-'} | ${job.filters.action || '-'} | ${job.filters.from || '-'}~${job.filters.to || '-'}` : '-'}</span>
              <span>
                {job.downloadReady
                  ? <button className="mini-btn" onClick={() => downloadExportJob(job.jobId)}>{t('exportDownload')}</button>
                  : <button className="mini-btn" onClick={() => retryExportJob(job.jobId)}>{t('retry')}</button>}
              </span>
            </div>
          )}
        />
        {exportJobsFiltered.length === 0 && <div className="empty-tip">{t('noData')}</div>}
        {exportJobsFiltered.length > 0 && (
          <ServerPager
            t={t}
            page={exportJobsPage}
            totalPages={exportJobsTotalPages}
            size={exportJobsSize}
            onPageChange={setExportJobsPage}
            onSizeChange={setExportJobsSize}
          />
        )}
      </div>
    </section>
  )
}

export default memo(AuditPanel)
