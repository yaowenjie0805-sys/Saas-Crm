import { memo } from 'react'
import ServerPager from '../../ServerPager'
import VirtualListTable from '../../VirtualListTable'
import { buildStatusLabel, formatDateTime } from './dashboardPanelHelpers'
import { translateRole } from '../../../shared'

/**
 * 状态筛选按钮
 */
function StatusFilterButton({ label, isActive, onClick }) {
  return (
    <button
      className={`mini-btn ${isActive ? 'active' : ''}`}
      onClick={onClick}
    >
      {label}
    </button>
  )
}

/**
 * 报表导出任务列表部分
 */
function ReportExportJobsSection({
  reportExportJobs,
  reportExportStatusFilter,
  setReportExportStatusFilter,
  reportExportJobsPage,
  setReportExportJobsPage,
  reportExportJobsTotalPages,
  reportExportJobsSize,
  setReportExportJobsSize,
  autoRefreshReportJobs,
  setAutoRefreshReportJobs,
  refreshReportJobs,
  retryReportExportJob,
  downloadReportExportJob,
  t,
}) {
  const jobs = (reportExportJobs || []).filter((job) => {
    if (reportExportStatusFilter === 'ALL') return true
    return String(job.status || '') === reportExportStatusFilter
  })

  const handleStatusFilter = (status) => {
    setReportExportStatusFilter(status)
    setReportExportJobsPage(1)
  }

  return (
    <section className="panel">
      <div className="panel-head">
        <h2>{t('reportExportJobs')}</h2>
        <div className="inline-tools filter-bar">
          <StatusFilterButton
            status="ALL"
            label={t('filterAll')}
            isActive={reportExportStatusFilter === 'ALL'}
            onClick={() => handleStatusFilter('ALL')}
          />
          <StatusFilterButton
            status="RUNNING"
            label={t('filterRunning')}
            isActive={reportExportStatusFilter === 'RUNNING'}
            onClick={() => handleStatusFilter('RUNNING')}
          />
          <StatusFilterButton
            status="FAILED"
            label={t('filterFailed')}
            isActive={reportExportStatusFilter === 'FAILED'}
            onClick={() => handleStatusFilter('FAILED')}
          />
          <StatusFilterButton
            status="DONE"
            label={t('filterDone')}
            isActive={reportExportStatusFilter === 'DONE'}
            onClick={() => handleStatusFilter('DONE')}
          />
          <button className="mini-btn" onClick={refreshReportJobs}>
            {t('refresh')}
          </button>
          <label className="switch-inline">
            <input
              type="checkbox"
              checked={autoRefreshReportJobs}
              onChange={(e) => setAutoRefreshReportJobs(e.target.checked)}
            />
            {t('autoRefresh')}
          </label>
        </div>
      </div>

      <div className="table-row table-head-row table-row-6 compact">
        <span>{t('idLabel')}</span>
        <span>{t('status')}</span>
        <span>{t('progress')}</span>
        <span>{t('createdAt')}</span>
        <span>{t('filtersSummary')}</span>
        <span>{t('action')}</span>
      </div>

      <VirtualListTable
        rows={jobs}
        rowHeight={42}
        viewportHeight={336}
        getRowKey={(job) => job.jobId}
        renderRow={(job) => (
          <div key={job.jobId} className="table-row table-row-6 compact">
            <span>{job.jobId}</span>
            <span>{buildStatusLabel(t, job.status)}</span>
            <span>{job.progress || 0}%</span>
            <span>{formatDateTime(job.createdAt)}</span>
            <span>
              {job.filters
                ? `${job.filters.role ? translateRole(t, job.filters.role) : '-'} | ${job.filters.from || '-'}~${job.filters.to || '-'}`
                : '-'}
            </span>
            <span>
              {job.downloadReady ? (
                <button className="mini-btn" onClick={() => downloadReportExportJob(job.jobId)}>
                  {t('exportDownload')}
                </button>
              ) : (
                <button className="mini-btn" onClick={() => retryReportExportJob(job.jobId)}>
                  {t('retry')}
                </button>
              )}
            </span>
          </div>
        )}
      />

      {jobs.length === 0 && <div className="empty-tip">{t('noData')}</div>}

      {jobs.length > 0 && (
        <ServerPager
          t={t}
          page={reportExportJobsPage}
          totalPages={reportExportJobsTotalPages}
          size={reportExportJobsSize}
          onPageChange={setReportExportJobsPage}
          onSizeChange={setReportExportJobsSize}
        />
      )}
    </section>
  )
}

export default memo(ReportExportJobsSection)
