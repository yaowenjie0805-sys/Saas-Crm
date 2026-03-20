import { useCallback } from 'react'
import { api } from '../shared'

export function useLeadImportDomainLoaders({
  authToken,
  lang,
  canViewOpsMetrics,
  leadImportStatusFilter,
  leadImportPage,
  leadImportSize,
  leadImportJob,
  setLeadImportJobs,
  setLeadImportPage,
  setLeadImportTotalPages,
  setLeadImportJob,
  setLeadImportFailedRows,
  setLeadImportMetrics,
  leadImportExportStatusFilter,
  leadImportExportPage,
  leadImportExportSize,
  setLeadImportExportJobs,
  setLeadImportExportPage,
  setLeadImportExportTotalPages,
}) {
  const loadLeadImportJobs = useCallback(async (page = leadImportPage, size = leadImportSize, options = {}) => {
    const q = new URLSearchParams({ status: leadImportStatusFilter, page: String(page), size: String(size) })
    const d = await api('/v1/leads/import-jobs?' + q, { signal: options.signal }, authToken, lang)
    const items = d.items || []
    setLeadImportJobs(items)
    setLeadImportPage(d.page || page)
    setLeadImportTotalPages(Math.max(1, d.totalPages || 1))
    if (!leadImportJob && items.length > 0) setLeadImportJob(items[0])
  }, [authToken, lang, leadImportJob, leadImportPage, leadImportSize, leadImportStatusFilter, setLeadImportJob, setLeadImportJobs, setLeadImportPage, setLeadImportTotalPages])

  const loadLeadImportFailedRows = useCallback(async (jobId, options = {}) => {
    if (!jobId) { setLeadImportFailedRows([]); return }
    const d = await api(`/v1/leads/import-jobs/${jobId}/failed-rows?page=1&size=20`, { signal: options.signal }, authToken, lang)
    setLeadImportFailedRows(d.items || [])
  }, [authToken, lang, setLeadImportFailedRows])

  const loadLeadImportMetrics = useCallback(async (options = {}) => {
    if (!canViewOpsMetrics) { setLeadImportMetrics(null); return }
    const d = await api('/v1/ops/metrics/summary', { signal: options.signal }, authToken, lang)
    setLeadImportMetrics(d.importMetrics || null)
  }, [canViewOpsMetrics, authToken, lang, setLeadImportMetrics])

  const loadLeadImportExportJobs = useCallback(async (jobId, page = leadImportExportPage, size = leadImportExportSize, options = {}) => {
    if (!jobId) {
      setLeadImportExportJobs([])
      setLeadImportExportPage(1)
      setLeadImportExportTotalPages(1)
      return
    }
    const q = new URLSearchParams({ status: leadImportExportStatusFilter, page: String(page), size: String(size) })
    const d = await api(`/v1/leads/import-jobs/${jobId}/failed-rows/export-jobs?` + q, { signal: options.signal }, authToken, lang)
    setLeadImportExportJobs(d.items || [])
    setLeadImportExportPage(d.page || page)
    setLeadImportExportTotalPages(Math.max(1, d.totalPages || 1))
  }, [authToken, lang, leadImportExportPage, leadImportExportSize, leadImportExportStatusFilter, setLeadImportExportJobs, setLeadImportExportPage, setLeadImportExportTotalPages])

  return {
    loadLeadImportJobs,
    loadLeadImportFailedRows,
    loadLeadImportMetrics,
    loadLeadImportExportJobs,
  }
}

