import BarChartRow from '../BarChartRow'
import { formatDateTime, formatMoney, formatStatValue, mapToBars, translateChannel, translateRole, translateStage, translateStatLabel, translateStatus } from '../../shared'

function DashboardPanel({
  activePage,
  stats,
  reports,
  t,
  canViewReports,
  auditFrom,
  auditTo,
  auditRole,
  reportOwner,
  setReportOwner,
  reportDepartment,
  setReportDepartment,
  reportTimezone,
  setReportTimezone,
  reportCurrency,
  setReportCurrency,
  exportReportCsv,
  reportExportJobs,
  reportExportStatusFilter,
  setReportExportStatusFilter,
  autoRefreshReportJobs,
  setAutoRefreshReportJobs,
  loadReportExportJobs,
  retryReportExportJob,
  downloadReportExportJob,
}) {
  if (!['dashboard', 'reports'].includes(activePage)) return null

  const reportSummary = reports?.summary || {}
  const ownerBars = mapToBars(reports?.customerByOwner)
  const statusBars = mapToBars(reports?.revenueByStatus).map((item) => ({ ...item, label: translateStatus(t, item.label) }))
  const stageBars = mapToBars(reports?.opportunityByStage).map((item) => ({ ...item, label: translateStage(t, item.label) }))
  const channelBars = mapToBars(reports?.followUpByChannel).map((item) => ({ ...item, label: translateChannel(t, item.label) }))
  const normalizedStats = (stats || []).map((item) => ({
    ...item,
    label: translateStatLabel(t, item.label),
    value: formatStatValue(t, item.label, item.value),
  }))
  const statusLabel = (s) => s === 'PENDING' ? t('exportPending') : s === 'RUNNING' ? t('exportRunning') : s === 'DONE' ? t('exportDone') : s === 'FAILED' ? t('exportFailed') : (s || '-')

  const jobs = (reportExportJobs || []).filter((job) => {
    if (reportExportStatusFilter === 'ALL') return true
    return String(job.status || '') === reportExportStatusFilter
  })

  return (
    <>
      {activePage === 'dashboard' && (
        <section className="stats-grid">
          {normalizedStats.map((s) => (
            <article key={s.label} className="stat-card">
              <p>{s.label}</p>
              <h3>{s.value}</h3>
              <span>{s.trend}</span>
            </article>
          ))}
        </section>
      )}

      <section className="panel">
        <div className="panel-head">
          <h2>{t('reports')}</h2>
          {canViewReports && (
            <div className="inline-tools">
              <span className="muted-filter">{t('filters')}: {auditFrom || '-'} ~ {auditTo || '-'} | {t('role')}: {auditRole ? translateRole(t, auditRole) : t('allRoles')}</span>
              <input className="tool-input" placeholder={t('reportOwner')} value={reportOwner} onChange={(e) => setReportOwner(e.target.value)} />
              <input className="tool-input" placeholder={t('reportDepartment')} value={reportDepartment} onChange={(e) => setReportDepartment(e.target.value)} />
              <input className="tool-input" placeholder={t('reportTimezone')} value={reportTimezone} onChange={(e) => setReportTimezone(e.target.value)} />
              <input className="tool-input" placeholder={t('reportCurrency')} value={reportCurrency} onChange={(e) => setReportCurrency(e.target.value.toUpperCase())} />
              <button className="mini-btn" onClick={exportReportCsv}>{t('exportReportCsv')}</button>
            </div>
          )}
        </div>
        {!reports ? (
          <div className="empty-tip">{t('loadingReports')}</div>
        ) : (
          <>
            <div className="report-summary">
              <div><b>{t('reportsSummary')}</b></div>
              <div>{t('customers')}: {reportSummary.customers || 0}</div>
              <div>{t('amount')}: {formatMoney(reportSummary.revenue || 0)}</div>
              <div>{t('pipeline')}: {reportSummary.opportunities || 0}</div>
              <div>{t('taskDoneRate')}: {reportSummary.taskDoneRate || 0}%</div>
              <div>{t('winRate')}: {reportSummary.winRate || 0}%</div>
            </div>
            <div className="report-grid">
              <div className="report-card"><h4>{t('customerByOwner')}</h4>{ownerBars.map((b) => <BarChartRow key={b.label} label={b.label} value={b.value} />)}</div>
              <div className="report-card"><h4>{t('revenueByStatus')}</h4>{statusBars.map((b) => <BarChartRow key={b.label} label={b.label} value={b.value} money />)}</div>
              <div className="report-card"><h4>{t('opportunityByStage')}</h4>{stageBars.map((b) => <BarChartRow key={b.label} label={b.label} value={b.value} />)}</div>
              <div className="report-card"><h4>{t('followByChannel')}</h4>{channelBars.map((b) => <BarChartRow key={b.label} label={b.label} value={b.value} />)}</div>
            </div>
          </>
        )}
      </section>

      {canViewReports && (
        <section className="panel">
          <div className="panel-head">
            <h2>{t('reportExportJobs')}</h2>
            <div className="inline-tools">
              <button className={`mini-btn ${reportExportStatusFilter === 'ALL' ? 'active' : ''}`} onClick={() => setReportExportStatusFilter('ALL')}>{t('filterAll')}</button>
              <button className={`mini-btn ${reportExportStatusFilter === 'RUNNING' ? 'active' : ''}`} onClick={() => setReportExportStatusFilter('RUNNING')}>{t('filterRunning')}</button>
              <button className={`mini-btn ${reportExportStatusFilter === 'FAILED' ? 'active' : ''}`} onClick={() => setReportExportStatusFilter('FAILED')}>{t('filterFailed')}</button>
              <button className={`mini-btn ${reportExportStatusFilter === 'DONE' ? 'active' : ''}`} onClick={() => setReportExportStatusFilter('DONE')}>{t('filterDone')}</button>
              <button className="mini-btn" onClick={loadReportExportJobs}>{t('refresh')}</button>
              <label className="switch-inline"><input type="checkbox" checked={autoRefreshReportJobs} onChange={(e) => setAutoRefreshReportJobs(e.target.checked)} />{t('autoRefresh')}</label>
            </div>
          </div>
          {jobs.length === 0 && <div className="empty-tip">{t('noData')}</div>}
          <div className="export-jobs-grid">
            {jobs.map((job) => (
              <article key={job.jobId} className={`export-job-card ${String(job.status || '').toLowerCase()}`}>
                <div className="job-top"><strong>{job.jobId}</strong><span>{statusLabel(job.status)}</span></div>
                <div className="job-meta">
                  <span>{job.progress || 0}%</span>
                  <span>{t('sourceJob')}: {job.sourceJobId || '-'}</span>
                  <span>{t('createdAt')}: {formatDateTime(job.createdAt)}</span>
                  <span>{t('filtersSummary')}: {job.filters ? `${job.filters.role ? translateRole(t, job.filters.role) : '-'}|${job.filters.from || '-'}~${job.filters.to || '-'}` : '-'}</span>
                </div>
                {job.error && <div className="job-err">{t('errorReason')}: {job.error}</div>}
                <div className="job-actions">{job.downloadReady ? <button className="mini-btn" onClick={() => downloadReportExportJob(job.jobId)}>{t('exportDownload')}</button> : <button className="mini-btn" onClick={() => retryReportExportJob(job.jobId)}>{t('retry')}</button>}</div>
              </article>
            ))}
          </div>
        </section>
      )}
    </>
  )
}

export default DashboardPanel
