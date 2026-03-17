import { useCallback } from 'react'
import { API_BASE, api } from '../shared'

export function useLeadImportActions({
  auth,
  lang,
  t,
  canViewOpsMetrics,
  handleError,
  refreshPage,
  setLeadImportJob,
  setLeadImportFailedRows,
  setLeadImportStatusFilter,
  setLeadImportPage,
  setLeadImportSize,
  setLeadImportExportStatusFilter,
  setLeadImportExportPage,
  setLeadImportExportSize,
  setCrudError,
}) {
  const withRequestId = useCallback((err, fallback = t('loadFailed')) => {
    const message = String(err?.message || fallback || '').trim()
    const requestId = String(err?.requestId || '').trim()
    return requestId ? `${message} [${requestId}]` : message
  }, [t])

  const handleLeadImportError = useCallback((err, fallback) => {
    const message = withRequestId(err, fallback)
    setCrudError?.('lead', message)
    handleError(err)
  }, [handleError, setCrudError, withRequestId])

  const importLeadsCsv = useCallback(async (file) => {
    if (!file) return
    try {
      setCrudError?.('lead', '')
      const formData = new FormData()
      formData.append('file', file)
      const job = await api('/v1/leads/import-jobs', { method: 'POST', body: formData }, auth.token, lang)
      setLeadImportJob(job)
      setLeadImportFailedRows([])
      await refreshPage('leads', 'panel_action')
      if (canViewOpsMetrics) await refreshPage('dashboard', 'panel_action')
    } catch (err) { handleLeadImportError(err, t('leadImportCsv')) }
  }, [auth?.token, canViewOpsMetrics, handleLeadImportError, lang, refreshPage, setCrudError, setLeadImportFailedRows, setLeadImportJob, t])

  const selectLeadImportJob = useCallback(async (job) => {
    if (!job) return
    setLeadImportJob(job)
    await refreshPage('leads', 'panel_action')
  }, [refreshPage, setLeadImportJob])

  const cancelLeadImportJob = useCallback(async (jobId) => {
    try {
      setCrudError?.('lead', '')
      const d = await api(`/v1/leads/import-jobs/${jobId}/cancel`, { method: 'POST' }, auth.token, lang)
      setLeadImportJob(d)
      await refreshPage('leads', 'panel_action')
    } catch (err) { handleLeadImportError(err, t('close')) }
  }, [auth?.token, handleLeadImportError, lang, refreshPage, setCrudError, setLeadImportJob, t])

  const retryLeadImportJob = useCallback(async (jobId) => {
    try {
      setCrudError?.('lead', '')
      const d = await api(`/v1/leads/import-jobs/${jobId}/retry`, { method: 'POST' }, auth.token, lang)
      setLeadImportJob(d)
      await refreshPage('leads', 'panel_action')
    } catch (err) { handleLeadImportError(err, t('retry')) }
  }, [auth?.token, handleLeadImportError, lang, refreshPage, setCrudError, setLeadImportJob, t])

  const createLeadImportFailedRowsExportJob = useCallback(async (jobId) => {
    if (!jobId) return
    try {
      setCrudError?.('lead', '')
      await api(`/v1/leads/import-jobs/${jobId}/failed-rows/export-jobs`, { method: 'POST' }, auth.token, lang)
      await refreshPage('leads', 'panel_action')
    } catch (err) { handleLeadImportError(err, t('leadImportExportCreate')) }
  }, [auth?.token, handleLeadImportError, lang, refreshPage, setCrudError, t])

  const updateLeadImportExportStatusFilter = useCallback((value) => {
    setLeadImportExportStatusFilter(value)
  }, [setLeadImportExportStatusFilter])

  const onLeadImportExportPageChange = useCallback((value) => {
    setLeadImportExportPage(value)
  }, [setLeadImportExportPage])

  const onLeadImportExportSizeChange = useCallback((value) => {
    setLeadImportExportSize(value)
  }, [setLeadImportExportSize])

  const downloadLeadImportFailedRowsExportJob = useCallback(async (jobId, exportJobId) => {
    try {
      const headers = { 'Accept-Language': lang }
      if (auth?.token && auth.token !== 'COOKIE_SESSION') headers.Authorization = `Bearer ${auth.token}`
      if (auth?.tenantId) headers['X-Tenant-Id'] = auth.tenantId
      const res = await fetch(`${API_BASE}/v1/leads/import-jobs/${jobId}/failed-rows/export-jobs/${exportJobId}/download`, { credentials: 'include', headers })
      if (!res.ok) {
        const body = await res.json().catch(() => ({}))
        const message = body.message || t('downloadFailed')
        const requestId = body.requestId || ''
        throw new Error(requestId ? `${message} [${requestId}]` : message)
      }
      const blob = await res.blob()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `lead-import-failed-rows-${exportJobId}.csv`
      document.body.appendChild(a)
      a.click()
      a.remove()
      URL.revokeObjectURL(url)
    } catch (err) { handleLeadImportError(err, t('downloadFailed')) }
  }, [auth?.tenantId, auth?.token, handleLeadImportError, lang, t])

  const updateLeadImportStatusFilter = useCallback((value) => {
    setLeadImportStatusFilter(value)
    setLeadImportFailedRows([])
  }, [setLeadImportFailedRows, setLeadImportStatusFilter])

  const onLeadImportPageChange = useCallback((value) => {
    setLeadImportPage(value)
    setLeadImportFailedRows([])
  }, [setLeadImportFailedRows, setLeadImportPage])

  const onLeadImportSizeChange = useCallback((value) => {
    setLeadImportSize(value)
    setLeadImportFailedRows([])
  }, [setLeadImportFailedRows, setLeadImportSize])

  const downloadLeadImportTemplate = useCallback(async () => {
    try {
      const headers = { 'Accept-Language': lang }
      if (auth?.token && auth.token !== 'COOKIE_SESSION') headers.Authorization = `Bearer ${auth.token}`
      if (auth?.tenantId) headers['X-Tenant-Id'] = auth.tenantId
      const res = await fetch(`${API_BASE}/v1/leads/import-template`, { credentials: 'include', headers })
      if (!res.ok) throw new Error(t('downloadFailed'))
      const blob = await res.blob()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'lead_import_template.csv'
      document.body.appendChild(a)
      a.click()
      a.remove()
      URL.revokeObjectURL(url)
    } catch (err) { handleLeadImportError(err, t('downloadFailed')) }
  }, [auth?.tenantId, auth?.token, handleLeadImportError, lang, t])

  return {
    importLeadsCsv,
    selectLeadImportJob,
    cancelLeadImportJob,
    retryLeadImportJob,
    createLeadImportFailedRowsExportJob,
    updateLeadImportExportStatusFilter,
    onLeadImportExportPageChange,
    onLeadImportExportSizeChange,
    downloadLeadImportFailedRowsExportJob,
    updateLeadImportStatusFilter,
    onLeadImportPageChange,
    onLeadImportSizeChange,
    downloadLeadImportTemplate,
  }
}
