import { useCallback } from 'react'
import { api } from '../shared'

export function useApprovalDomainLoaders({
  authToken,
  lang,
  approvalTaskStatus,
  approvalOverdueOnly,
  approvalEscalatedOnly,
  notificationStatusFilter,
  notificationPage,
  notificationSize,
  setApprovalTemplates,
  setApprovalStats,
  setApprovalTasks,
  setApprovalInstances,
  setApprovalDetail,
  setApprovalTemplateVersions,
  setApprovalVersionTemplateId,
  setNotificationJobs,
  setNotificationPage,
  setNotificationTotalPages,
  setSelectedNotificationJobs,
}) {
  const loadApprovalTemplates = useCallback(async () => {
    const q = new URLSearchParams({ limit: '100' })
    const d = await api('/v1/approval/templates?' + q, {}, authToken, lang)
    setApprovalTemplates(d.items || [])
  }, [authToken, lang, setApprovalTemplates])

  const loadApprovalStats = useCallback(async () => {
    const d = await api('/v1/approval/stats', {}, authToken, lang)
    setApprovalStats(d || null)
  }, [authToken, lang, setApprovalStats])

  const loadApprovalTasks = useCallback(async () => {
    const q = new URLSearchParams({ status: approvalTaskStatus, limit: '20' })
    if (approvalOverdueOnly) q.set('overdue', 'true')
    if (approvalEscalatedOnly) q.set('escalated', 'true')
    const d = await api('/v1/approval/tasks?' + q, {}, authToken, lang)
    setApprovalTasks(d.items || [])
  }, [approvalTaskStatus, approvalOverdueOnly, approvalEscalatedOnly, authToken, lang, setApprovalTasks])

  const loadApprovalInstances = useCallback(async () => {
    const d = await api('/v1/approval/instances?limit=12', {}, authToken, lang)
    setApprovalInstances(d.items || [])
  }, [authToken, lang, setApprovalInstances])

  const loadApprovalDetail = useCallback(async (id) => {
    const d = await api('/v1/approval/instances/' + id, {}, authToken, lang)
    setApprovalDetail(d)
  }, [authToken, lang, setApprovalDetail])

  const loadApprovalTemplateVersions = useCallback(async (templateId) => {
    const d = await api('/v1/approval/templates/' + templateId + '/versions', {}, authToken, lang)
    setApprovalTemplateVersions(d.items || [])
    setApprovalVersionTemplateId(templateId)
  }, [authToken, lang, setApprovalTemplateVersions, setApprovalVersionTemplateId])

  const loadNotificationJobs = useCallback(async (page = notificationPage, size = notificationSize) => {
    const q = new URLSearchParams({ page: String(page), size: String(size) })
    if (notificationStatusFilter !== 'ALL') q.set('status', notificationStatusFilter)
    const d = await api('/v1/integrations/notifications/jobs?' + q, {}, authToken, lang)
    setNotificationJobs(d.items || [])
    setNotificationPage(d.page || page)
    setNotificationTotalPages(Math.max(1, d.totalPages || 1))
    setSelectedNotificationJobs([])
  }, [notificationStatusFilter, notificationPage, notificationSize, authToken, lang, setNotificationJobs, setNotificationPage, setNotificationTotalPages, setSelectedNotificationJobs])

  return {
    loadApprovalTemplates,
    loadApprovalStats,
    loadApprovalTasks,
    loadApprovalInstances,
    loadApprovalDetail,
    loadApprovalTemplateVersions,
    loadNotificationJobs,
  }
}

