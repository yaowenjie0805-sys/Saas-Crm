import { useCallback, useState } from 'react'
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
  const [pendingActions, setPendingActions] = useState({})
  const pendingKey = useCallback((action, id = '') => `${String(action || '').trim()}:${String(id || '').trim()}`, [])
  const markPending = useCallback((action, id, value) => {
    const key = pendingKey(action, id)
    setPendingActions((prev) => {
      if (value) return { ...prev, [key]: true }
      if (!prev[key]) return prev
      const next = { ...prev }
      delete next[key]
      return next
    })
  }, [pendingKey])
  const isLeadImportActionPending = useCallback((action, id = '') => {
    return Boolean(pendingActions[pendingKey(action, id)])
  }, [pendingActions, pendingKey])

  const resolveLeadImportMessage = useCallback((err, fallback = t('loadFailed')) => {
    const code = String(err?.code || '').trim().toLowerCase()
    if (code === 'lead_import_status_transition_invalid') {
      return lang === 'zh' ? '导入任务状态已变化，请刷新后重试' : 'Import job state changed, please refresh and retry'
    }
    if (code === 'lead_import_retry_no_pending_chunks') {
      return lang === 'zh' ? '没有可重试的导入分片，请刷新后重试' : 'No retryable import chunks found, please refresh and retry'
    }
    if (code === 'lead_import_concurrent_limit_exceeded') {
      return lang === 'zh' ? '导入任务并发已达上限，请稍后重试' : 'Import concurrency limit reached, please retry later'
    }
    return String(err?.message || fallback || '').trim()
  }, [lang, t])

  const withRequestId = useCallback((err, fallback = t('loadFailed')) => {
    const message = resolveLeadImportMessage(err, fallback)
    const requestId = String(err?.requestId || '').trim()
    return requestId ? `${message} [${requestId}]` : message
  }, [resolveLeadImportMessage, t])

  const handleLeadImportError = useCallback(async (err, fallback, options = {}) => {
    const message = withRequestId(err, fallback)
    setCrudError?.('lead', message)
    handleError(err)
    if (options.refreshOnConflict && Number(err?.status) === 409) {
      await refreshPage('leads', 'panel_action')
    }
  }, [handleError, refreshPage, setCrudError, withRequestId])

  const importLeadsCsv = useCallback(async (file) => {
    if (!file) return
    if (isLeadImportActionPending('upload')) return
    markPending('upload', '', true)
    try {
      setCrudError?.('lead', '')
      const formData = new FormData()
      formData.append('file', file)
      const job = await api('/v1/leads/import-jobs', { method: 'POST', body: formData }, auth.token, lang)
      setLeadImportJob(job)
      setLeadImportFailedRows([])
      await refreshPage('leads', 'panel_action')
      if (canViewOpsMetrics) await refreshPage('dashboard', 'panel_action')
    } catch (err) {
      handleLeadImportError(err, t('leadImportCsv'))
    } finally {
      markPending('upload', '', false)
    }
  }, [auth?.token, canViewOpsMetrics, handleLeadImportError, isLeadImportActionPending, lang, markPending, refreshPage, setCrudError, setLeadImportFailedRows, setLeadImportJob, t])

  const selectLeadImportJob = useCallback(async (job) => {
    if (!job) return
    setLeadImportJob(job)
    await refreshPage('leads', 'panel_action')
  }, [refreshPage, setLeadImportJob])

  const cancelLeadImportJob = useCallback(async (jobId) => {
    if (!jobId || isLeadImportActionPending('cancel', jobId)) return
    markPending('cancel', jobId, true)
    try {
      setCrudError?.('lead', '')
      const d = await api(`/v1/leads/import-jobs/${jobId}/cancel`, { method: 'POST' }, auth.token, lang)
      setLeadImportJob(d)
      await refreshPage('leads', 'panel_action')
    } catch (err) {
      await handleLeadImportError(err, t('close'), { refreshOnConflict: true })
    } finally {
      markPending('cancel', jobId, false)
    }
  }, [auth?.token, handleLeadImportError, isLeadImportActionPending, lang, markPending, refreshPage, setCrudError, setLeadImportJob, t])

  const retryLeadImportJob = useCallback(async (jobId) => {
    if (!jobId || isLeadImportActionPending('retry', jobId)) return
    markPending('retry', jobId, true)
    try {
      setCrudError?.('lead', '')
      const d = await api(`/v1/leads/import-jobs/${jobId}/retry`, { method: 'POST' }, auth.token, lang)
      setLeadImportJob(d)
      await refreshPage('leads', 'panel_action')
    } catch (err) {
      await handleLeadImportError(err, t('retry'), { refreshOnConflict: true })
    } finally {
      markPending('retry', jobId, false)
    }
  }, [auth?.token, handleLeadImportError, isLeadImportActionPending, lang, markPending, refreshPage, setCrudError, setLeadImportJob, t])

  const createLeadImportFailedRowsExportJob = useCallback(async (jobId) => {
    if (!jobId) return
    if (isLeadImportActionPending('export-create', jobId)) return
    markPending('export-create', jobId, true)
    try {
      setCrudError?.('lead', '')
      await api(`/v1/leads/import-jobs/${jobId}/failed-rows/export-jobs`, { method: 'POST' }, auth.token, lang)
      await refreshPage('leads', 'panel_action')
    } catch (err) {
      await handleLeadImportError(err, t('leadImportExportCreate'), { refreshOnConflict: true })
    } finally {
      markPending('export-create', jobId, false)
    }
  }, [auth?.token, handleLeadImportError, isLeadImportActionPending, lang, markPending, refreshPage, setCrudError, t])

  const updateLeadImportExportStatusFilter = useCallback((value) => {
    setLeadImportExportStatusFilter(value)
    setLeadImportExportPage(1)
  }, [setLeadImportExportPage, setLeadImportExportStatusFilter])

  const onLeadImportExportPageChange = useCallback((value) => {
    setLeadImportExportPage(value)
  }, [setLeadImportExportPage])

  const onLeadImportExportSizeChange = useCallback((value) => {
    setLeadImportExportSize(value)
  }, [setLeadImportExportSize])

  const downloadLeadImportBlob = useCallback(async ({ path, filename, fallbackMessage = t('downloadFailed') }) => {
    const headers = { 'Accept-Language': lang }
    if (auth?.token && auth.token !== 'COOKIE_SESSION') headers.Authorization = `Bearer ${auth.token}`
    if (auth?.tenantId) headers['X-Tenant-Id'] = auth.tenantId
    const res = await fetch(`${API_BASE}${path}`, { credentials: 'include', headers })
    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      const message = body.message || fallbackMessage
      const requestId = body.requestId || ''
      throw new Error(requestId ? `${message} [${requestId}]` : message)
    }
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    try {
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      document.body.appendChild(a)
      a.click()
      a.remove()
    } finally {
      URL.revokeObjectURL(url)
    }
  }, [auth?.tenantId, auth?.token, lang, t])

  const downloadLeadImportFailedRowsExportJob = useCallback(async (jobId, exportJobId) => {
    if (!jobId || !exportJobId || isLeadImportActionPending('export-download', exportJobId)) return
    markPending('export-download', exportJobId, true)
    try {
      await downloadLeadImportBlob({
        path: `/v1/leads/import-jobs/${jobId}/failed-rows/export-jobs/${exportJobId}/download`,
        filename: `lead-import-failed-rows-${exportJobId}.csv`,
        fallbackMessage: t('downloadFailed'),
      })
    } catch (err) {
      handleLeadImportError(err, t('downloadFailed'))
    } finally {
      markPending('export-download', exportJobId, false)
    }
  }, [downloadLeadImportBlob, handleLeadImportError, isLeadImportActionPending, markPending, t])

  const updateLeadImportStatusFilter = useCallback((value) => {
    setLeadImportStatusFilter(value)
    setLeadImportPage(1)
    setLeadImportFailedRows([])
  }, [setLeadImportFailedRows, setLeadImportPage, setLeadImportStatusFilter])

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
      await downloadLeadImportBlob({
        path: '/v1/leads/import-template',
        filename: 'lead_import_template.csv',
        fallbackMessage: t('downloadFailed'),
      })
    } catch (err) { handleLeadImportError(err, t('downloadFailed')) }
  }, [downloadLeadImportBlob, handleLeadImportError, t])

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
    isLeadImportActionPending,
  }
}
