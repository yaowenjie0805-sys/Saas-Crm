import { api } from '../../shared'

export function useApprovalPageActions(params) {
  const {
    auth,
    lang,
    role,
    handleError,
    approvalTemplateForm,
    setApprovalTemplateForm,
    setApprovalTemplates,
    loadApprovalTemplates,
    loadApprovalTemplateVersions,
    loadApprovalStats,
    loadApprovalInstances,
    loadApprovalTasks,
    loadApprovalDetail,
    approvalInstanceForm,
    setApprovalInstanceForm,
    approvalDetail,
    approvalActionComment,
    setApprovalActionComment,
    approvalTransferTo,
    setApprovalTransferTo,
    loadNotificationJobs,
    setSelectedNotificationJobs,
    notificationJobs,
    selectedNotificationJobs,
    notificationStatusFilter,
    notificationPage,
    notificationSize,
    setError,
    setApprovalActionResult,
    approvalPendingTaskIds,
    setApprovalPendingTaskIds,
    t,
  } = params

  const setTaskPending = (taskId, pending) => {
    if (!taskId) return
    setApprovalPendingTaskIds((prev) => {
      const next = { ...(prev || {}) }
      if (pending) next[taskId] = true
      else delete next[taskId]
      return next
    })
  }

  const isTaskPending = (taskId) => !!(taskId && approvalPendingTaskIds?.[taskId])

  const withRequestId = (err) => {
    const code = String(err?.code || '').trim().toLowerCase()
    const reason = String(err?.details?.reason || '').trim().toLowerCase()
    let message = String(err?.message || '')
    if (code === 'approval_task_closed') {
      if (reason === 'urge_cooldown') {
        const until = String(err?.details?.cooldownUntil || '').trim()
        message = until ? `${t('approvalUrgeCooldownHint')} (${until})` : t('approvalUrgeCooldownHint')
      } else if (reason === 'urge_daily_limit') {
        const count = Number(err?.details?.dailyCount || 0)
        const limit = Number(err?.details?.dailyLimit || 10)
        message = `${t('approvalUrgeDailyLimitHint')} (${count}/${limit})`
      } else {
        message = t('approvalTaskClosedHint')
      }
    }
    const requestId = String(err?.requestId || '').trim()
    return requestId ? `${message} [${requestId}]` : message
  }

  const createApprovalTemplate = async () => {
    try {
      await api('/v1/approval/templates', {
        method: 'POST',
        body: JSON.stringify({
          bizType: approvalTemplateForm.bizType,
          name: approvalTemplateForm.name,
          approverRoles: approvalTemplateForm.approverRoles,
        }),
      }, auth.token, lang)
      setApprovalTemplateForm((p) => ({ ...p, name: '' }))
      await loadApprovalTemplates()
      await loadApprovalStats()
      await loadApprovalInstances()
      await loadApprovalTasks()
    } catch (err) {
      setError(withRequestId(err))
      handleError(err)
    }
  }

  const submitApprovalInstance = async () => {
    try {
      await api(`/v1/approval/instances/${approvalInstanceForm.bizType}/${approvalInstanceForm.bizId}/submit`, {
        method: 'POST',
        body: JSON.stringify({
          amount: Number(approvalInstanceForm.amount || 0),
          role,
          department: auth?.department || 'DEFAULT',
          comment: 'Submitted from web panel',
        }),
      }, auth.token, lang)
      setApprovalInstanceForm((p) => ({ ...p, bizId: '', amount: '' }))
      await loadApprovalStats()
      await loadApprovalInstances()
      await loadApprovalTasks()
    } catch (err) {
      setError(withRequestId(err))
      handleError(err)
    }
  }

  const updateApprovalTemplate = async (row) => {
    try {
      const updated = await api('/v1/approval/templates/' + row.id, {
        method: 'PATCH',
        body: JSON.stringify({
          name: row.name,
          amountMin: row.amountMin === '' || row.amountMin === null || row.amountMin === undefined ? null : Number(row.amountMin),
          amountMax: row.amountMax === '' || row.amountMax === null || row.amountMax === undefined ? null : Number(row.amountMax),
          role: row.role || '',
          department: row.department || '',
          approverRoles: row.approverRoles || '',
          flowDefinition: row.flowDefinition || null,
          status: row.status || undefined,
          version: row.version || undefined,
          enabled: !!row.enabled,
        }),
      }, auth.token, lang)
      setApprovalTemplates((prev) => prev.map((x) => x.id === updated.id ? updated : x))
      await loadApprovalStats()
    } catch (err) {
      setError(withRequestId(err))
      handleError(err)
    }
  }

  const actApprovalTask = async (taskId, action) => {
    if (isTaskPending(taskId)) return
    setTaskPending(taskId, true)
    try {
      const payload = { comment: approvalActionComment }
      if (action === 'transfer') payload.transferTo = approvalTransferTo
      const result = await api('/v1/approval/tasks/' + taskId + '/' + action, { method: 'POST', body: JSON.stringify(payload) }, auth.token, lang)
      await loadApprovalStats()
      await loadApprovalTasks()
      await loadApprovalInstances()
      if (result?.instance?.id) await loadApprovalDetail(result.instance.id)
      setApprovalActionResult({
        action: String(action || '').toUpperCase(),
        taskId,
        result: result?.result || result?.status || '',
        instanceId: result?.instance?.id || '',
        bizType: result?.bizWriteback?.bizType || '',
        bizId: result?.bizWriteback?.bizId || '',
        bizStatus: result?.bizWriteback?.status || '',
        requestId: result?.requestId || result?.requestIdRef || '',
      })
      setApprovalActionComment('')
      if (action === 'transfer') setApprovalTransferTo('')
    } catch (err) {
      setError(withRequestId(err))
      if (String(err?.code || '').toLowerCase() === 'approval_task_closed') {
        await loadApprovalTasks()
        if (approvalDetail?.id) await loadApprovalDetail(approvalDetail.id)
      }
      handleError(err)
    } finally {
      setTaskPending(taskId, false)
    }
  }

  const urgeApprovalTask = async (taskId) => {
    if (isTaskPending(taskId)) return
    setTaskPending(taskId, true)
    try {
      const result = await api('/v1/approval/tasks/' + taskId + '/urge', { method: 'POST', body: JSON.stringify({ comment: approvalActionComment, urgeChannel: 'IN_APP' }) }, auth.token, lang)
      await loadApprovalStats()
      await loadApprovalTasks()
      await loadNotificationJobs()
      if (result?.instance?.id) await loadApprovalDetail(result.instance.id)
      setApprovalActionResult({
        action: 'URGE',
        taskId,
        result: result?.result || '',
        instanceId: result?.instance?.id || '',
        requestId: result?.requestId || result?.requestIdRef || '',
      })
      setApprovalActionComment('')
    } catch (err) {
      setError(withRequestId(err))
      if (String(err?.code || '').toLowerCase() === 'approval_task_closed') {
        await loadApprovalTasks()
        if (approvalDetail?.id) await loadApprovalDetail(approvalDetail.id)
      }
      handleError(err)
    } finally {
      setTaskPending(taskId, false)
    }
  }

  const publishApprovalTemplate = async (templateId) => {
    try {
      await api('/v1/approval/templates/' + templateId + '/publish', { method: 'POST' }, auth.token, lang)
      await loadApprovalTemplates()
      await loadApprovalStats()
      await loadApprovalTemplateVersions(templateId)
    } catch (err) {
      setError(withRequestId(err))
      handleError(err)
    }
  }

  const rollbackApprovalTemplate = async (templateId, version) => {
    try {
      await api('/v1/approval/templates/' + templateId + '/rollback/' + version, { method: 'POST' }, auth.token, lang)
      await loadApprovalTemplates()
      await loadApprovalStats()
      await loadApprovalTemplateVersions(templateId)
    } catch (err) {
      setError(withRequestId(err))
      handleError(err)
    }
  }

  const retryNotificationJob = async (jobId) => {
    try {
      await api('/v1/integrations/notifications/jobs/' + jobId + '/retry', { method: 'POST' }, auth.token, lang)
      await loadNotificationJobs()
    } catch (err) {
      setError(withRequestId(err))
      handleError(err)
    }
  }

  const toggleNotificationJob = (jobId, checked) => {
    setSelectedNotificationJobs((prev) => {
      const set = new Set(prev)
      if (checked) set.add(jobId)
      else set.delete(jobId)
      return Array.from(set)
    })
  }

  const toggleAllNotificationJobs = (checked) => {
    if (!checked) { setSelectedNotificationJobs([]); return }
    setSelectedNotificationJobs((notificationJobs || []).map((j) => j.jobId))
  }

  const retryNotificationJobsByIds = async () => {
    try {
      if ((selectedNotificationJobs || []).length === 0) return
      const result = await api('/v1/integrations/notifications/jobs/batch-retry', {
        method: 'POST',
        body: JSON.stringify({ jobIds: selectedNotificationJobs }),
      }, auth.token, lang)
      setError(`${t('retry')}: requested=${result.requested}, succeeded=${result.succeeded}, skipped=${result.skipped}`)
      await loadNotificationJobs(notificationPage, notificationSize)
    } catch (err) {
      setError(withRequestId(err))
      handleError(err)
    }
  }

  const retryNotificationJobsByFilter = async () => {
    try {
      const result = await api('/v1/integrations/notifications/jobs/retry-by-filter', {
        method: 'POST',
        body: JSON.stringify({ status: notificationStatusFilter, page: notificationPage, size: notificationSize }),
      }, auth.token, lang)
      setError(`${t('retry')}: requested=${result.requested}, succeeded=${result.succeeded}, skipped=${result.skipped}`)
      await loadNotificationJobs(notificationPage, notificationSize)
    } catch (err) {
      setError(withRequestId(err))
      handleError(err)
    }
  }

  return {
    createApprovalTemplate,
    submitApprovalInstance,
    updateApprovalTemplate,
    actApprovalTask,
    urgeApprovalTask,
    publishApprovalTemplate,
    rollbackApprovalTemplate,
    retryNotificationJob,
    toggleNotificationJob,
    toggleAllNotificationJobs,
    retryNotificationJobsByIds,
    retryNotificationJobsByFilter,
  }
}
