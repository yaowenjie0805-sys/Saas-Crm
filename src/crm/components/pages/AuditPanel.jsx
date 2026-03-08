import { ROLES, formatDateTime, translateRole } from '../../shared'

function AuditPanel({
  activePage,
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
  loadExportJobs,
  autoRefreshJobs,
  setAutoRefreshJobs,
  auditLogs,
  exportStatusFilter,
  setExportStatusFilter,
  exportJobs,
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

  const exportJobsFiltered = exportJobs.filter((job) => {
    if (exportStatusFilter === 'ALL') return true
    return String(job.status || '') === exportStatusFilter
  })

  return (
    <section className="panel">
      <div className="panel-head">
        <h2>{t('audit')}</h2>
        <div className="inline-tools">
          <input className="tool-input" placeholder={t('user')} value={auditUser} onChange={(e) => setAuditUser(e.target.value)} />
          <select className="tool-input" value={auditRole} onChange={(e) => setAuditRole(e.target.value)}>
            <option value="">{t('allRoles')}</option>
            {ROLES.map((r) => <option key={r} value={r}>{translateRole(t, r)}</option>)}
          </select>
          <input className="tool-input" placeholder={t('action')} value={auditAction} onChange={(e) => setAuditAction(e.target.value)} />
          <input className={auditRangeError ? 'tool-input input-invalid' : 'tool-input'} type="date" value={auditFrom} onChange={(e) => { setAuditFrom(e.target.value); setAuditRangeError('') }} />
          <input className={auditRangeError ? 'tool-input input-invalid' : 'tool-input'} type="date" value={auditTo} onChange={(e) => { setAuditTo(e.target.value); setAuditRangeError('') }} />
          <button className="mini-btn" onClick={() => applyQuickRange(1)}>{t('today')}</button>
          <button className="mini-btn" onClick={() => applyQuickRange(7)}>{t('last7Days')}</button>
          <button className="mini-btn" onClick={() => applyQuickRange(30)}>{t('last30Days')}</button>
          {auditRangeError && <span className="field-error">{auditRangeError}</span>}
          <button className="mini-btn" disabled={hasInvalidAuditRange()} onClick={loadAudit}>{t('search')}</button>
          <button className="mini-btn" disabled={hasInvalidAuditRange()} onClick={createExportJob}>{t('filteredExport')}</button>
          <button className="mini-btn" onClick={loadExportJobs}>{t('refresh')}</button>
          <label className="switch-inline"><input type="checkbox" checked={autoRefreshJobs} onChange={(e) => setAutoRefreshJobs(e.target.checked)} />{t('autoRefresh')}</label>
        </div>
      </div>
      {auditLogs.slice(0, 8).map((log) => <div key={log.id} className="audit-row"><strong>{log.action}</strong><span>{log.resource}</span><span>{log.username}</span><small>{formatDateTime(log.createdAt)}</small></div>)}
      <div className="panel" style={{ marginTop: 10, boxShadow: 'none' }}>
        <div className="panel-head">
          <h2>{t('exportJob')}</h2>
          <div className="export-filter-tabs">
            <button className={`mini-btn ${exportStatusFilter === 'ALL' ? 'active' : ''}`} onClick={() => setExportStatusFilter('ALL')}>{t('filterAll')}</button>
            <button className={`mini-btn ${exportStatusFilter === 'RUNNING' ? 'active' : ''}`} onClick={() => setExportStatusFilter('RUNNING')}>{t('filterRunning')}</button>
            <button className={`mini-btn ${exportStatusFilter === 'FAILED' ? 'active' : ''}`} onClick={() => setExportStatusFilter('FAILED')}>{t('filterFailed')}</button>
            <button className={`mini-btn ${exportStatusFilter === 'DONE' ? 'active' : ''}`} onClick={() => setExportStatusFilter('DONE')}>{t('filterDone')}</button>
          </div>
        </div>
        {exportJobsFiltered.length === 0 && <div className="empty-tip">{t('noData')}</div>}
        <div className="export-jobs-grid">
          {exportJobsFiltered.map((job) => (
            <article key={job.jobId} className={`export-job-card ${String(job.status || '').toLowerCase()}`}>
              <div className="job-top"><strong>{job.jobId}</strong><span>{statusLabel(job.status)}</span></div>
              <div className="job-meta">
                <span>{job.progress || 0}%</span>
                <span>{t('sourceJob')}: {job.sourceJobId || '-'}</span>
                <span>{t('createdAt')}: {formatDateTime(job.createdAt)}</span>
                <span>{t('duration')}: {durationLabel(job)}</span>
                <span>{t('filtersSummary')}: {job.filters ? `${job.filters.username || '-'}|${job.filters.role ? translateRole(t, job.filters.role) : '-'}|${job.filters.action || '-'}|${job.filters.from || '-'}~${job.filters.to || '-'}` : '-'}</span>
              </div>
              {job.error && <div className="job-err">{t('errorReason')}: {job.error}</div>}
              <div className="job-actions">{job.downloadReady ? <button className="mini-btn" onClick={() => downloadExportJob(job.jobId)}>{t('exportDownload')}</button> : <button className="mini-btn" onClick={() => retryExportJob(job.jobId)}>{t('retry')}</button>}</div>
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}

export default AuditPanel
