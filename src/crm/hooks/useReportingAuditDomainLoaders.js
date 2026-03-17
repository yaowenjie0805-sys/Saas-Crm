import { useCallback } from 'react'
import { api } from '../shared'

export function useReportingAuditDomainLoaders({
  canViewAudit,
  canViewReports,
  authToken,
  lang,
  auditUser,
  auditRole,
  auditAction,
  auditFrom,
  auditTo,
  reportOwner,
  reportDepartment,
  reportTimezone,
  reportCurrency,
  exportStatusFilter,
  exportJobsPage,
  exportJobsSize,
  reportExportStatusFilter,
  reportExportJobsPage,
  reportExportJobsSize,
  hasInvalidAuditRange,
  setAuditRangeError,
  t,
  setAuditLogs,
  setReports,
  setExportJobs,
  setExportJobsPage,
  setExportJobsTotalPages,
  setReportExportJobs,
  setReportExportJobsPage,
  setReportExportJobsTotalPages,
  setDesignerTemplates,
}) {
  const loadAudit = useCallback(async () => {
    if (!canViewAudit) return
    if (hasInvalidAuditRange()) {
      setAuditRangeError(t('dateRangeInvalid'))
      return
    }
    setAuditRangeError('')
    const q = new URLSearchParams({
      username: auditUser,
      role: auditRole,
      action: auditAction,
      from: auditFrom,
      to: auditTo,
      page: '1',
      size: '10',
    })
    const d = await api('/audit-logs/search?' + q, {}, authToken, lang)
    setAuditLogs(d.items || [])
  }, [canViewAudit, hasInvalidAuditRange, setAuditRangeError, t, auditUser, auditRole, auditAction, auditFrom, auditTo, authToken, lang, setAuditLogs])

  const loadReports = useCallback(async () => {
    if (!canViewReports) return
    const q = new URLSearchParams({
      from: auditFrom,
      to: auditTo,
      role: auditRole,
      owner: reportOwner,
      department: reportDepartment,
      timezone: reportTimezone,
      currency: reportCurrency,
    })
    const [v1Overview, v2Commerce, v2TenantConfig] = await Promise.all([
      api('/v1/reports/overview?' + q, {}, authToken, lang),
      api('/v2/commerce/overview', {}, authToken, lang).catch(() => null),
      api('/v2/tenant-config', {}, authToken, lang).catch(() => null),
    ])
    const marketContext = {
      marketProfile: v2TenantConfig?.marketProfile || v2Commerce?.marketContext?.marketProfile || 'CN',
      currency: v2TenantConfig?.currency || v2Commerce?.marketContext?.currency || reportCurrency || 'CNY',
      timezone: v2TenantConfig?.timezone || v2Commerce?.marketContext?.timezone || reportTimezone || 'Asia/Shanghai',
      approvalMode: v2TenantConfig?.approvalMode || v2Commerce?.marketContext?.approvalMode || 'STRICT',
    }
    const localizedMetrics = v2Commerce?.localizedMetrics
      ? {
          ...v2Commerce.localizedMetrics,
          pipelineHealth: Number(v2Commerce.localizedMetrics.pipelineHealth || 0),
          arrLike: Number(v2Commerce.localizedMetrics.arrLike || 0),
        }
      : null
    setReports({
      ...(v1Overview || {}),
      marketContext,
      localizedMetrics,
      localizedFallback: !v2Commerce?.localizedMetrics,
      localizedFallbackReason: v2Commerce ? '' : 'v2_commerce_unavailable',
      tenantConfigSynced: !!v2TenantConfig,
    })
  }, [canViewReports, auditFrom, auditTo, auditRole, reportOwner, reportDepartment, reportTimezone, reportCurrency, authToken, lang, setReports])

  const loadExportJobs = useCallback(async (page = exportJobsPage, size = exportJobsSize) => {
    const q = new URLSearchParams({ page: String(page), size: String(size) })
    if (exportStatusFilter !== 'ALL') q.set('status', exportStatusFilter)
    const d = await api('/audit-logs/export-jobs?' + q, {}, authToken, lang)
    setExportJobs(d.items || [])
    setExportJobsPage(d.page || page)
    setExportJobsTotalPages(Math.max(1, d.totalPages || 1))
  }, [exportStatusFilter, exportJobsPage, exportJobsSize, authToken, lang, setExportJobs, setExportJobsPage, setExportJobsTotalPages])

  const loadReportExportJobs = useCallback(async (page = reportExportJobsPage, size = reportExportJobsSize) => {
    const q = new URLSearchParams({ page: String(page), size: String(size) })
    if (reportExportStatusFilter !== 'ALL') q.set('status', reportExportStatusFilter)
    const d = await api('/v1/reports/export-jobs?' + q, {}, authToken, lang)
    setReportExportJobs(d.items || [])
    setReportExportJobsPage(d.page || page)
    setReportExportJobsTotalPages(Math.max(1, d.totalPages || 1))
  }, [reportExportStatusFilter, reportExportJobsPage, reportExportJobsSize, authToken, lang, setReportExportJobs, setReportExportJobsPage, setReportExportJobsTotalPages])

  const loadDesignerTemplates = useCallback(async () => {
    const d = await api('/v1/reports/designer/templates', {}, authToken, lang)
    setDesignerTemplates(d.items || [])
  }, [authToken, lang, setDesignerTemplates])

  return {
    loadAudit,
    loadReports,
    loadExportJobs,
    loadReportExportJobs,
    loadDesignerTemplates,
  }
}
