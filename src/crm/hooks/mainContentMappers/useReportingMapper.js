import { useMemo } from 'react'

export function useReportingMapper(params) {
  const {
    tasks,
    toggleTaskDone,
    reportDesignerForm,
    setReportDesignerForm,
    createReportDesignerTemplate,
    designerTemplates,
    updateReportDesignerTemplate,
    runReportDesignerTemplate,
    designerRunResult,
    loadDesignerTemplates,
    canViewAudit,
    auditUser,
    setAuditUser,
    auditRole,
    setAuditRole,
    auditAction,
    setAuditAction,
    auditFrom,
    setAuditFrom,
    auditTo,
    setAuditTo,
    auditRangeError,
    setAuditRangeError,
    hasInvalidAuditRange,
    loadAudit,
    createExportJob,
    loadExportJobs,
    autoRefreshJobs,
    setAutoRefreshJobs,
    auditLogs,
    exportStatusFilter,
    setExportStatusFilter,
    exportJobs,
    exportJobsPage,
    setExportJobsPage,
    exportJobsTotalPages,
    exportJobsSize,
    setExportJobsSize,
    downloadExportJob,
    retryExportJob,
  } = params

  const mainTasks = useMemo(() => ({ tasks, toggleTaskDone }), [tasks, toggleTaskDone])

  const mainReportDesigner = useMemo(() => ({
    templateForm: reportDesignerForm, setTemplateForm: setReportDesignerForm, createTemplate: createReportDesignerTemplate, templates: designerTemplates, updateTemplate: updateReportDesignerTemplate, runTemplate: runReportDesignerTemplate, runResult: designerRunResult, loadTemplates: loadDesignerTemplates,
  }), [reportDesignerForm, setReportDesignerForm, createReportDesignerTemplate, designerTemplates, updateReportDesignerTemplate, runReportDesignerTemplate, designerRunResult, loadDesignerTemplates])

  const mainAudit = useMemo(() => ({
    canViewAudit, auditUser, setAuditUser, auditRole, setAuditRole, auditAction, setAuditAction, auditFrom, setAuditFrom, auditTo, setAuditTo, auditRangeError, setAuditRangeError, hasInvalidAuditRange, loadAudit, createExportJob, loadExportJobs, autoRefreshJobs, setAutoRefreshJobs, auditLogs, exportStatusFilter, setExportStatusFilter, exportJobs, exportJobsPage, setExportJobsPage, exportJobsTotalPages, exportJobsSize, setExportJobsSize, downloadExportJob, retryExportJob,
  }), [canViewAudit, auditUser, setAuditUser, auditRole, setAuditRole, auditAction, setAuditAction, auditFrom, setAuditFrom, auditTo, setAuditTo, auditRangeError, setAuditRangeError, hasInvalidAuditRange, loadAudit, createExportJob, loadExportJobs, autoRefreshJobs, setAutoRefreshJobs, auditLogs, exportStatusFilter, setExportStatusFilter, exportJobs, exportJobsPage, setExportJobsPage, exportJobsTotalPages, exportJobsSize, setExportJobsSize, downloadExportJob, retryExportJob])

  return {
    mainTasks,
    mainReportDesigner,
    mainAudit,
  }
}
