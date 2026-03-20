import { useMemo } from 'react'
import { useRuntimeSectionFields } from './useRuntimeSectionFields'

export function useReportingRuntimeState({ persisted, readPageSize }) {
  const defaults = useMemo(() => ({
    auditLogs: [],
    reports: null,
    exportJobs: [],
    exportJobsPage: 1,
    exportJobsTotalPages: 1,
    exportJobsSize: () => readPageSize('crm_page_size_audit_export_jobs', 8),
    autoRefreshJobs: true,
    exportStatusFilter: 'ALL',
    reportExportJobs: [],
    reportExportJobsPage: 1,
    reportExportJobsTotalPages: 1,
    reportExportJobsSize: () => readPageSize('crm_page_size_report_export_jobs', 8),
    designerTemplates: [],
    designerRunResult: null,
    autoRefreshReportJobs: true,
    reportExportStatusFilter: 'ALL',
    auditUser: persisted.auditUser || '',
    auditRole: persisted.auditRole || '',
    auditAction: persisted.auditAction || '',
    auditFrom: persisted.auditFrom || '',
    auditTo: persisted.auditTo || '',
    reportOwner: persisted.reportOwner || '',
    reportDepartment: persisted.reportDepartment || '',
    reportTimezone: persisted.reportTimezone || (Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Shanghai'),
    reportCurrency: persisted.reportCurrency || 'CNY',
    auditRangeError: '',
    reportDesignerForm: { name: '', dataset: 'LEADS', visibility: 'PRIVATE', limit: '100' },
  }), [persisted, readPageSize])

  return useRuntimeSectionFields('reportingDomain', 'ui', defaults)
}
