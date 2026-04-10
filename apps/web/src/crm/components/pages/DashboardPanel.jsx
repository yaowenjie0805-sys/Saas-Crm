import { memo, useMemo } from 'react'
import {
  AiCommentReplySection,
  AiFollowUpSummarySection,
  AiMarketingEmailSection,
  AiSalesAdviceSection,
  ReportExportJobsSection,
  ReportsSection,
  StatsSection,
  WorkbenchSection,
} from './dashboard/sections'
import { formatStatValue, translateRole, translateStatLabel } from '../../shared'

function DashboardPanel({
  activePage,
  apiContext,
  refreshPage,
  stats,
  reports,
  workbenchToday,
  onWorkbenchNavigate,
  quickCreateTask,
  trackWorkbenchEvent,
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
  reportExportJobsPage,
  setReportExportJobsPage,
  reportExportJobsTotalPages,
  reportExportJobsSize,
  setReportExportJobsSize,
  autoRefreshReportJobs,
  setAutoRefreshReportJobs,
  retryReportExportJob,
  downloadReportExportJob,
}) {
  const showDashboardOrReports = ['dashboard', 'reports'].includes(activePage)

  // Prefer the dedicated stats payload; only fall back to report summary fields when needed.
  const normalizedStats = useMemo(() => {
    const primaryStats = Array.isArray(stats) ? stats.filter(Boolean) : []
    const fallbackStats = []
    const summary = reports?.summary

    if (primaryStats.length === 0 && summary && typeof summary === 'object') {
      if (summary.customers != null) {
        fallbackStats.push({ label: 'TOTAL_CUSTOMERS', value: summary.customers })
      }
      if (summary.revenue != null) {
        fallbackStats.push({ label: 'PROJECTED_REVENUE', value: summary.revenue })
      }
    }

    return (primaryStats.length > 0 ? primaryStats : fallbackStats).map((item) => ({
      ...item,
      label: translateStatLabel(t, item.label),
      value: formatStatValue(t, item.label, item.value),
    }))
  }, [reports, stats, t])

  // Reports section handlers
  const handleExportReportCsv = () => exportReportCsv?.()
  const refreshReportJobs = () => refreshPage('reports', 'panel_action')

  if (!showDashboardOrReports) return null

  return (
    <>
      {activePage === 'dashboard' && (
        <>
          <StatsSection stats={normalizedStats} t={t} />

          <WorkbenchSection
            workbenchToday={workbenchToday}
            onWorkbenchNavigate={onWorkbenchNavigate}
            quickCreateTask={quickCreateTask}
            trackWorkbenchEvent={trackWorkbenchEvent}
            t={t}
          />
          <AiFollowUpSummarySection t={t} apiContext={apiContext} />
          <AiCommentReplySection t={t} apiContext={apiContext} />
          <AiMarketingEmailSection t={t} apiContext={apiContext} />
          <AiSalesAdviceSection t={t} apiContext={apiContext} />
        </>
      )}

      <section className="panel">
        <div className="panel-head">
          <h2>{t('reports')}</h2>
        </div>
        {canViewReports && (
          <div className="inline-tools filter-row" style={{ marginBottom: 8 }}>
            <span className="muted-filter">
              {t('filters')}: {auditFrom || '-'} ~ {auditTo || '-'} | {t('role')}:{' '}
              {auditRole ? translateRole(t, auditRole) : t('allRoles')}
            </span>
            <input
              className="tool-input"
              placeholder={t('reportOwner')}
              value={reportOwner}
              onChange={(e) => setReportOwner(e.target.value)}
            />
            <input
              className="tool-input"
              placeholder={t('reportDepartment')}
              value={reportDepartment}
              onChange={(e) => setReportDepartment(e.target.value)}
            />
            <input
              className="tool-input"
              placeholder={t('reportTimezone')}
              value={reportTimezone}
              onChange={(e) => setReportTimezone(e.target.value)}
            />
            <input
              className="tool-input"
              placeholder={t('reportCurrency')}
              value={reportCurrency}
              onChange={(e) => setReportCurrency(e.target.value.toUpperCase())}
            />
          </div>
        )}
        {canViewReports && (
          <div className="inline-tools filter-bar" style={{ marginBottom: 8 }}>
            <button className="mini-btn" onClick={handleExportReportCsv}>
              {t('exportReportCsv')}
            </button>
          </div>
        )}
        <ReportsSection reports={reports} reportCurrency={reportCurrency} t={t} />
      </section>

      {canViewReports && (
        <ReportExportJobsSection
          reportExportJobs={reportExportJobs}
          reportExportStatusFilter={reportExportStatusFilter}
          setReportExportStatusFilter={setReportExportStatusFilter}
          reportExportJobsPage={reportExportJobsPage}
          setReportExportJobsPage={setReportExportJobsPage}
          reportExportJobsTotalPages={reportExportJobsTotalPages}
          reportExportJobsSize={reportExportJobsSize}
          setReportExportJobsSize={setReportExportJobsSize}
          autoRefreshReportJobs={autoRefreshReportJobs}
          setAutoRefreshReportJobs={setAutoRefreshReportJobs}
          refreshReportJobs={refreshReportJobs}
          retryReportExportJob={retryReportExportJob}
          downloadReportExportJob={downloadReportExportJob}
          t={t}
        />
      )}
    </>
  )
}

export default memo(DashboardPanel)
