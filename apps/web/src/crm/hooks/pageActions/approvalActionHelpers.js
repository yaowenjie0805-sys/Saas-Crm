export function updatePendingTaskIds(prev, taskId, pending) {
  if (!taskId) return prev || {}
  const next = { ...(prev || {}) }
  if (pending) next[taskId] = true
  else delete next[taskId]
  return next
}

export function isApprovalTaskPending(pendingTaskIds, taskId) {
  return !!(taskId && pendingTaskIds?.[taskId])
}

export function withApprovalRequestId(err, t) {
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
  const requestId = String(err?.requestId || err?.requestIdRef || '').trim()
  return requestId ? `${message} [${requestId}]` : message
}

export function createApprovalActionErrorResult({ taskId, action, err, approvalDetailId }) {
  const requestId = String(err?.requestId || err?.requestIdRef || '').trim()
  const code = String(err?.code || '').trim().toLowerCase()
  const reason = String(err?.details?.reason || '').trim().toLowerCase()
  return {
    action: String(action || '').toUpperCase(),
    taskId: taskId || String(err?.details?.taskId || '').trim(),
    result: code === 'approval_task_closed' ? 'CONFLICT' : 'FAILED',
    instanceId: String(approvalDetailId || err?.details?.instanceId || '').trim(),
    requestId,
    errorCode: code || '',
    reason,
  }
}

export function updateNotificationSelection(prev, jobId, checked) {
  const set = new Set(prev || [])
  if (checked) set.add(jobId)
  else set.delete(jobId)
  return Array.from(set)
}

export function createAllNotificationSelection(notificationJobs, checked) {
  if (!checked) return []
  return (notificationJobs || []).map((j) => j.jobId)
}

export function normalizeApprovalAmount(value) {
  return value === '' || value === null || value === undefined ? null : Number(value)
}
