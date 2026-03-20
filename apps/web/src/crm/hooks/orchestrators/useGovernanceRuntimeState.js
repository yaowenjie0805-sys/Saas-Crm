import { useMemo } from 'react'
import { useRuntimeSectionFields } from './useRuntimeSectionFields'

export function useGovernanceRuntimeState() {
  const defaults = useMemo(() => ({
    permissionMatrix: [],
    permissionConflicts: [],
    permissionRole: 'SALES',
    permissionPreview: null,
    pendingPack: '',
    leadAssignmentRules: [],
    assignmentRuleForm: { id: '', name: '', enabled: true, membersText: 'sales:1' },
    automationRules: [],
    automationRuleForm: {
      id: '',
      name: '',
      triggerType: 'LEAD_CREATED',
      triggerExpr: '{}',
      actionType: 'CREATE_TASK',
      actionPayload: '{"title":"Follow up lead"}',
      enabled: true,
    },
    adminUsers: [],
    tenantForm: {
      name: '',
      quotaUsers: '100',
      timezone: 'Asia/Shanghai',
      currency: 'CNY',
      status: 'ACTIVE',
      dateFormat: 'yyyy-MM-dd',
      marketProfile: 'CN',
      taxRule: 'VAT_CN',
      approvalMode: 'STRICT',
      channels: '["WECOM","DINGTALK"]',
      dataResidency: 'CN',
      maskLevel: 'STANDARD',
    },
    lastCreatedTenant: null,
    tenantRows: [],
    inviteForm: { username: '', role: 'SALES', ownerScope: '', department: 'DEFAULT', dataScope: 'SELF' },
    inviteResult: null,
  }), [])

  return useRuntimeSectionFields('governanceDomain', 'ui', defaults)
}
