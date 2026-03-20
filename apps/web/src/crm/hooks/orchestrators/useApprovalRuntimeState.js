import { useMemo } from 'react'
import { useRuntimeSectionFields } from './useRuntimeSectionFields'

export function useApprovalRuntimeState({ readPageSize }) {
  const defaults = useMemo(() => ({
    approvalTemplateForm: { bizType: 'CONTRACT', name: '', approverRoles: 'MANAGER,ADMIN' },
    approvalInstanceForm: { bizType: 'CONTRACT', bizId: '', amount: '' },
    approvalTemplates: [],
    approvalStats: null,
    approvalTasks: [],
    approvalInstances: [],
    approvalDetail: null,
    approvalTemplateVersions: [],
    approvalVersionTemplateId: '',
    approvalTaskStatus: 'PENDING',
    approvalOverdueOnly: false,
    approvalEscalatedOnly: false,
    approvalActionComment: '',
    approvalTransferTo: '',
    approvalActionResult: null,
    approvalPendingTaskIds: {},
    notificationJobs: [],
    notificationStatusFilter: 'ALL',
    notificationPage: 1,
    notificationTotalPages: 1,
    notificationSize: () => readPageSize('crm_page_size_notification_jobs', 10),
    selectedNotificationJobs: [],
  }), [readPageSize])

  return useRuntimeSectionFields('approvalDomain', 'ui', defaults)
}
