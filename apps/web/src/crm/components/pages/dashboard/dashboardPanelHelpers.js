import { formatDateTime, translateOwnerAlias, translateStatus } from '../../../shared'

export { formatDateTime }

export function createOwnerAliasMap(t) {
  return {
    'Li Jun': t('ownerLiJun'),
    'Wang Ling': t('ownerWangLing'),
    'Chen Xi': t('ownerChenXi'),
    'Zhao Ning': t('ownerZhaoNing'),
  }
}

export function buildStatusLabel(t, status) {
  if (status === 'PENDING') return t('exportPending')
  if (status === 'RUNNING') return t('exportRunning')
  if (status === 'DONE') return t('exportDone')
  if (status === 'FAILED') return t('exportFailed')
  return status || '-'
}

export function buildCardMeta() {
  return {
    todoTasks: { targetPage: 'tasks', payload: {}, actionKey: 'workbenchHandle' },
    overdueFollowUps: { targetPage: 'followUps', payload: {}, actionKey: 'workbenchHandle' },
    pendingApprovals: { targetPage: 'approvals', payload: { status: 'PENDING' }, actionKey: 'workbenchUrge' },
    upcomingContracts: { targetPage: 'contracts', payload: {}, actionKey: 'workbenchView' },
    paymentWarnings: { targetPage: 'payments', payload: { status: 'Overdue' }, actionKey: 'workbenchHandle' },
  }
}

export function mapWorkbenchItems(items, t) {
  return (items || []).map((item) => ({
    ...item,
    statusText: translateStatus(t, item.status),
    ownerText: translateOwnerAlias(t, item.owner || '-'),
  }))
}
