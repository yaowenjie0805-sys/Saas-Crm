import { useCallback } from 'react'
import { api } from '../shared'

const CHANNELS_DEFAULT = {
  CN: '["WECOM","DINGTALK"]',
  GLOBAL: '["EMAIL","SLACK"]',
}

function normalizeTenantRow(row, normalizeDateFormat) {
  const marketProfile = String(row?.marketProfile || 'CN').trim().toUpperCase() === 'GLOBAL' ? 'GLOBAL' : 'CN'
  return {
    ...row,
    dateFormat: normalizeDateFormat(row?.dateFormat),
    marketProfile,
    currency: String(row?.currency || '').trim() || (marketProfile === 'GLOBAL' ? 'USD' : 'CNY'),
    timezone: String(row?.timezone || '').trim() || (marketProfile === 'GLOBAL' ? 'UTC' : 'Asia/Shanghai'),
    taxRule: String(row?.taxRule || '').trim() || (marketProfile === 'GLOBAL' ? 'VAT_GLOBAL' : 'VAT_CN'),
    approvalMode: String(row?.approvalMode || '').trim().toUpperCase() === 'STAGE_GATE' ? 'STAGE_GATE' : 'STRICT',
    channels: String(row?.channels || '').trim() || CHANNELS_DEFAULT[marketProfile],
    dataResidency: String(row?.dataResidency || '').trim() || marketProfile,
    maskLevel: String(row?.maskLevel || '').trim() || 'STANDARD',
  }
}

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
      const normalized = normalizeTenantRow(row, normalizeDateFormat)
      if (currentTenantConfig && currentTenantConfig.tenantId === row.id) {
        return normalizeTenantRow({
          ...normalized,
          marketProfile: currentTenantConfig.marketProfile || row.marketProfile || 'CN',
          taxRule: currentTenantConfig.taxRule || row.taxRule || 'VAT_CN',
          approvalMode: currentTenantConfig.approvalMode || row.approvalMode || 'STRICT',
          channels: currentTenantConfig.channels || row.channels || CHANNELS_DEFAULT[String(row.marketProfile || 'CN').toUpperCase() === 'GLOBAL' ? 'GLOBAL' : 'CN'],
          dataResidency: currentTenantConfig.dataResidency || row.dataResidency || 'CN',
          maskLevel: currentTenantConfig.maskLevel || row.maskLevel || 'STANDARD',
          currency: currentTenantConfig.currency || row.currency || normalized.currency,
          timezone: currentTenantConfig.timezone || row.timezone || normalized.timezone,
          dateFormat: currentTenantConfig.dateFormat || row.dateFormat || normalized.dateFormat,
        }, normalizeDateFormat)
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
