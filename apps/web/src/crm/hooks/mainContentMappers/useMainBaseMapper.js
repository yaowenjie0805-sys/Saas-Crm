import { useMemo } from 'react'

export function useMainBaseMapper(params) {
  const {
    currentPageLabel, lang, setLang, refreshPage, t, canWrite, role, error, loading, activePage,
    stats, reports, workbenchToday, canViewReports, auditFrom, auditTo, auditRole, reportOwner, setReportOwner,
    reportDepartment, setReportDepartment, reportTimezone, setReportTimezone, reportCurrency, setReportCurrency,
    exportReportCsv, reportExportJobs, reportExportStatusFilter, setReportExportStatusFilter, reportExportJobsPage,
    setReportExportJobsPage, reportExportJobsTotalPages, reportExportJobsSize, setReportExportJobsSize,
    autoRefreshReportJobs, setAutoRefreshReportJobs, loadReportExportJobs, retryReportExportJob,
    downloadReportExportJob, quoteOpportunityFilter, orderOpportunityFilter, quotePrefill, consumeQuotePrefill,
    auth, performLogout, navigateToWorkbenchTarget, createTaskShortcut, trackWorkbenchEvent,
  } = params

  const mainBase = useMemo(() => ({
    currentPageLabel, lang, setLang, refreshPage, t, canWrite, role, error, loading, activePage, stats, reports, workbenchToday, canViewReports, auditFrom, auditTo, auditRole, reportOwner, setReportOwner, reportDepartment, setReportDepartment, reportTimezone, setReportTimezone, reportCurrency, setReportCurrency, exportReportCsv, reportExportJobs, reportExportStatusFilter, setReportExportStatusFilter, reportExportJobsPage, setReportExportJobsPage, reportExportJobsTotalPages, reportExportJobsSize, setReportExportJobsSize, autoRefreshReportJobs, setAutoRefreshReportJobs, loadReportExportJobs, retryReportExportJob, downloadReportExportJob, quoteOpportunityFilter, orderOpportunityFilter, quotePrefill, consumeQuotePrefill, auth, onLogout: performLogout, onWorkbenchNavigate: navigateToWorkbenchTarget, quickCreateTask: createTaskShortcut, trackWorkbenchEvent,
  }), [currentPageLabel, lang, setLang, refreshPage, t, canWrite, role, error, loading, activePage, stats, reports, workbenchToday, canViewReports, auditFrom, auditTo, auditRole, reportOwner, setReportOwner, reportDepartment, setReportDepartment, reportTimezone, setReportTimezone, reportCurrency, setReportCurrency, exportReportCsv, reportExportJobs, reportExportStatusFilter, setReportExportStatusFilter, reportExportJobsPage, setReportExportJobsPage, reportExportJobsTotalPages, reportExportJobsSize, setReportExportJobsSize, autoRefreshReportJobs, setAutoRefreshReportJobs, loadReportExportJobs, retryReportExportJob, downloadReportExportJob, quoteOpportunityFilter, orderOpportunityFilter, quotePrefill, consumeQuotePrefill, auth, performLogout, navigateToWorkbenchTarget, createTaskShortcut, trackWorkbenchEvent])

  return { mainBase }
}

