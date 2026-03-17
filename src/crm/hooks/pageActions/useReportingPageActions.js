import { API_BASE, api } from '../../shared'

export function useReportingPageActions(params) {
  const {
    auth,
    lang,
    t,
    canViewReports,
    handleError,
    setError,
    refreshPage,
    hasInvalidAuditRange,
    setAuditRangeError,
    auditUser,
    auditRole,
    auditAction,
    auditFrom,
    auditTo,
    reportOwner,
    reportDepartment,
    reportTimezone,
    reportCurrency,
    reportDesignerForm,
    setReportDesignerForm,
    setDesignerRunResult,
  } = params

  const createExportJob = async () => {
    try {
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
      })
      await api('/audit-logs/export-jobs?' + q, { method: 'POST' }, auth.token, lang)
      await refreshPage('audit', 'panel_action')
    } catch (err) {
      handleError(err)
    }
  }

  const retryExportJob = async (jobId) => {
    try {
      await api('/audit-logs/export-jobs/' + jobId + '/retry', { method: 'POST' }, auth.token, lang)
      await refreshPage('audit', 'panel_action')
    } catch (err) {
      handleError(err)
    }
  }

  const downloadExportJob = async (jobId) => {
    try {
      const res = await fetch(`${API_BASE}/audit-logs/export-jobs/${jobId}/download`, {
        headers: { Authorization: `Bearer ${auth.token}`, 'Accept-Language': lang },
      })
      if (!res.ok) throw new Error(t('downloadFailed'))
      const blob = await res.blob()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `audit-${jobId}.csv`
      document.body.appendChild(a)
      a.click()
      a.remove()
      URL.revokeObjectURL(url)
    } catch (err) {
      handleError(err)
    }
  }

  const createReportExportJob = async () => {
    if (!canViewReports) return
    try {
      if (hasInvalidAuditRange()) {
        setAuditRangeError(t('dateRangeInvalid'))
        return
      }
      setAuditRangeError('')
      const q = new URLSearchParams({
        from: auditFrom,
        to: auditTo,
        role: auditRole,
        owner: reportOwner,
        department: reportDepartment,
        timezone: reportTimezone,
        currency: reportCurrency,
      })
      await api('/v1/reports/export-jobs?' + q, { method: 'POST' }, auth.token, lang)
      await refreshPage('reports', 'panel_action')
    } catch (err) {
      handleError(err)
    }
  }

  const retryReportExportJob = async (jobId) => {
    try {
      await api('/v1/reports/export-jobs/' + jobId + '/retry', { method: 'POST' }, auth.token, lang)
      await refreshPage('reports', 'panel_action')
    } catch (err) {
      handleError(err)
    }
  }

  const downloadReportExportJob = async (jobId) => {
    try {
      const res = await fetch(`${API_BASE}/v1/reports/export-jobs/${jobId}/download`, {
        headers: { Authorization: `Bearer ${auth.token}`, 'Accept-Language': lang },
      })
      if (!res.ok) throw new Error(t('downloadFailed'))
      const blob = await res.blob()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `report-${jobId}.csv`
      document.body.appendChild(a)
      a.click()
      a.remove()
      URL.revokeObjectURL(url)
    } catch (err) {
      handleError(err)
    }
  }

  const createReportDesignerTemplate = async () => {
    try {
      const payload = {
        name: String(reportDesignerForm.name || '').trim(),
        dataset: String(reportDesignerForm.dataset || 'LEADS').trim(),
        visibility: String(reportDesignerForm.visibility || 'PRIVATE').trim(),
        config: { limit: Number(reportDesignerForm.limit || 100) },
      }
      if (!payload.name) {
        setError(t('fieldRequired'))
        return
      }
      await api('/v1/reports/designer/templates', { method: 'POST', body: JSON.stringify(payload) }, auth.token, lang)
      setReportDesignerForm((p) => ({ ...p, name: '' }))
      await refreshPage('reportDesigner', 'panel_action')
    } catch (err) {
      handleError(err)
    }
  }

  const updateReportDesignerTemplate = async (row) => {
    try {
      await api('/v1/reports/designer/templates/' + row.id, {
        method: 'PATCH',
        body: JSON.stringify({
          name: row.name,
          dataset: row.dataset,
          visibility: row.visibility,
          department: row.department || null,
          config: row.config || { limit: 100 },
        }),
      }, auth.token, lang)
      await refreshPage('reportDesigner', 'panel_action')
    } catch (err) {
      handleError(err)
    }
  }

  const runReportDesignerTemplate = async (id) => {
    try {
      const result = await api('/v1/reports/designer/templates/' + id + '/run', { method: 'POST', body: JSON.stringify({}) }, auth.token, lang)
      setDesignerRunResult(result)
    } catch (err) {
      handleError(err)
    }
  }

  const exportReportCsv = async () => {
    await createReportExportJob()
  }

  return {
    exportReportCsv,
    createExportJob,
    retryExportJob,
    downloadExportJob,
    createReportExportJob,
    retryReportExportJob,
    downloadReportExportJob,
    createReportDesignerTemplate,
    updateReportDesignerTemplate,
    runReportDesignerTemplate,
  }
}
