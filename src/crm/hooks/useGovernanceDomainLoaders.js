import { useCallback } from 'react'
import { api } from '../shared'

export function useGovernanceDomainLoaders({
  canManageUsers,
  canManageSalesAutomation,
  authToken,
  lang,
  setPermissionMatrix,
  setPermissionConflicts,
  setLeadAssignmentRules,
  setAutomationRules,
  setAdminUsers,
  setTenantRows,
  normalizeDateFormat,
}) {
  const loadPermissionMatrix = useCallback(async () => {
    const d = await api('/permissions/matrix', {}, authToken, lang)
    setPermissionMatrix(d.matrix || [])
  }, [authToken, lang, setPermissionMatrix])

  const loadPermissionConflicts = useCallback(async () => {
    const d = await api('/permissions/conflicts', {}, authToken, lang)
    setPermissionConflicts(d.items || [])
  }, [authToken, lang, setPermissionConflicts])

  const loadLeadAssignmentRules = useCallback(async () => {
    if (!canManageSalesAutomation) return
    const d = await api('/v1/leads/assignment-rules', {}, authToken, lang)
    setLeadAssignmentRules(d.items || [])
  }, [canManageSalesAutomation, authToken, lang, setLeadAssignmentRules])

  const loadAutomationRulesV1 = useCallback(async () => {
    if (!canManageSalesAutomation) return
    const d = await api('/v1/automation/rules', {}, authToken, lang)
    setAutomationRules(d.items || [])
  }, [canManageSalesAutomation, authToken, lang, setAutomationRules])

  const loadAdminUsers = useCallback(async () => {
    if (!canManageUsers) return
    const d = await api('/v1/admin/users', {}, authToken, lang)
    setAdminUsers(d.items || [])
  }, [canManageUsers, authToken, lang, setAdminUsers])

  const loadTenants = useCallback(async () => {
    if (!canManageUsers) return
    const d = await api('/v1/tenants', {}, authToken, lang)
    let currentTenantConfig = null
    try {
      currentTenantConfig = await api('/v2/tenant-config', {}, authToken, lang)
    } catch {
      currentTenantConfig = null
    }
    setTenantRows((d.items || []).map((row) => {
      const normalized = { ...row, dateFormat: normalizeDateFormat(row.dateFormat) }
      if (currentTenantConfig && currentTenantConfig.tenantId === row.id) {
        return {
          ...normalized,
          marketProfile: currentTenantConfig.marketProfile || row.marketProfile || 'CN',
          taxRule: currentTenantConfig.taxRule || row.taxRule || 'VAT_CN',
          approvalMode: currentTenantConfig.approvalMode || row.approvalMode || 'STRICT',
          channels: currentTenantConfig.channels || row.channels || '["WECOM","DINGTALK"]',
          dataResidency: currentTenantConfig.dataResidency || row.dataResidency || 'CN',
          maskLevel: currentTenantConfig.maskLevel || row.maskLevel || 'STANDARD',
        }
      }
      return normalized
    }))
  }, [canManageUsers, authToken, lang, setTenantRows, normalizeDateFormat])

  return {
    loadPermissionMatrix,
    loadPermissionConflicts,
    loadLeadAssignmentRules,
    loadAutomationRulesV1,
    loadAdminUsers,
    loadTenants,
  }
}
