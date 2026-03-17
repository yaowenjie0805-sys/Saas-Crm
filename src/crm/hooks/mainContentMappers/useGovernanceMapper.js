import { useMemo } from 'react'

export function useGovernanceMapper(params) {
  const {
    permissionRole, setPermissionRole, canManagePermissions, previewPermissionPack, pendingPack, commitPendingPack,
    rollbackPermissionRole, permissionPreview, permissionMatrix, changePermission, permissionConflicts,
    canManageUsers, adminUsers, loadAdminUsers, setAdminUsers, getAdminUserError, saveAdminUser, unlockAdminUser,
    inviteForm, setInviteForm, inviteUser, inviteResult,
    canManageSalesAutomation, leadAssignmentRules, assignmentRuleForm, setAssignmentRuleForm, saveLeadAssignmentRule,
    loadLeadAssignmentRules, automationRules, automationRuleForm, setAutomationRuleForm, saveAutomationRule,
    loadAutomationRulesV1,
    tenantForm, setTenantForm, createTenant, tenantRows, setTenantRows, updateTenant, lastCreatedTenant,
  } = params

  const mainPermissions = useMemo(() => ({
    permissionRole, setPermissionRole, canManagePermissions, previewPermissionPack, pendingPack, commitPendingPack, rollbackPermissionRole, permissionPreview, permissionMatrix, changePermission, permissionConflicts,
  }), [permissionRole, setPermissionRole, canManagePermissions, previewPermissionPack, pendingPack, commitPendingPack, rollbackPermissionRole, permissionPreview, permissionMatrix, changePermission, permissionConflicts])

  const mainUsers = useMemo(() => ({
    canManageUsers,
    adminUsers,
    loadAdminUsers,
    setAdminUsers,
    getAdminUserError,
    saveAdminUser,
    unlockAdminUser,
    inviteForm,
    setInviteForm,
    inviteUser,
    inviteResult,
  }), [canManageUsers, adminUsers, loadAdminUsers, setAdminUsers, getAdminUserError, saveAdminUser, unlockAdminUser, inviteForm, setInviteForm, inviteUser, inviteResult])

  const mainSalesAutomation = useMemo(() => ({
    canManageSalesAutomation,
    assignmentRules: leadAssignmentRules,
    assignmentRuleForm,
    setAssignmentRuleForm,
    saveLeadAssignmentRule,
    reloadLeadAssignmentRules: loadLeadAssignmentRules,
    automationRules,
    automationRuleForm,
    setAutomationRuleForm,
    saveAutomationRule,
    reloadAutomationRules: loadAutomationRulesV1,
  }), [canManageSalesAutomation, leadAssignmentRules, assignmentRuleForm, setAssignmentRuleForm, saveLeadAssignmentRule, loadLeadAssignmentRules, automationRules, automationRuleForm, setAutomationRuleForm, saveAutomationRule, loadAutomationRulesV1])

  const mainTenants = useMemo(() => ({
    form: tenantForm,
    setForm: setTenantForm,
    createTenant,
    rows: tenantRows,
    setRows: setTenantRows,
    updateTenant,
    lastCreated: lastCreatedTenant,
  }), [tenantForm, setTenantForm, createTenant, tenantRows, setTenantRows, updateTenant, lastCreatedTenant])

  return {
    mainPermissions,
    mainUsers,
    mainSalesAutomation,
    mainTenants,
  }
}
