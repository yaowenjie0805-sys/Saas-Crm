import { ROLES, api, READ_OPS, WRITE_OPS } from '../../shared'

function parseRuleMembers(text) {
  return String(text || '').split(',').map((pair) => {
    const [usernameRaw, weightRaw] = pair.split(':')
    const username = String(usernameRaw || '').trim()
    const weight = Number(weightRaw || 1)
    if (!username) return null
    return { username, weight: Number.isFinite(weight) && weight > 0 ? Math.floor(weight) : 1 }
  }).filter((row) => row.username)
}

export function useGovernancePageActions(params) {
  const {
    auth,
    lang,
    t,
    normalizeDateFormat,
    setError,
    handleError,
    assignmentRuleForm,
    setAssignmentRuleForm,
    loadLeadAssignmentRules,
    automationRuleForm,
    setAutomationRuleForm,
    loadAutomationRulesV1,
    canManagePermissions,
    permissionRole,
    pendingPack,
    setPendingPack,
    setPermissionPreview,
    setPermissionMatrix,
    loadPermissionConflicts,
    setAdminUsers,
    setInviteResult,
    setInviteForm,
    inviteForm,
    loadAdminUsers,
    tenantForm,
    setTenantForm,
    setLastCreatedTenant,
    loadTenants,
    setTenantRows,
  } = params

  const saveLeadAssignmentRule = async () => {
    try {
      const payload = {
        name: String(assignmentRuleForm.name || '').trim(),
        enabled: !!assignmentRuleForm.enabled,
        members: parseRuleMembers(assignmentRuleForm.membersText),
      }
      if (!payload.name || payload.members.length === 0) { setError(t('fieldRequired')); return }
      if (assignmentRuleForm.id) {
        await api('/v1/leads/assignment-rules/' + assignmentRuleForm.id, { method: 'PATCH', body: JSON.stringify(payload) }, auth.token, lang)
      } else {
        await api('/v1/leads/assignment-rules', { method: 'POST', body: JSON.stringify(payload) }, auth.token, lang)
      }
      setAssignmentRuleForm({ id: '', name: '', enabled: true, membersText: 'sales:1' })
      await loadLeadAssignmentRules()
    } catch (err) { handleError(err) }
  }

  const saveAutomationRule = async () => {
    try {
      const payload = {
        name: String(automationRuleForm.name || '').trim(),
        triggerType: String(automationRuleForm.triggerType || '').trim(),
        triggerExpr: String(automationRuleForm.triggerExpr || '{}').trim(),
        actionType: String(automationRuleForm.actionType || '').trim(),
        actionPayload: String(automationRuleForm.actionPayload || '{}').trim(),
        enabled: !!automationRuleForm.enabled,
      }
      if (!payload.name) { setError(t('fieldRequired')); return }
      if (automationRuleForm.id) {
        await api('/v1/automation/rules/' + automationRuleForm.id, { method: 'PATCH', body: JSON.stringify(payload) }, auth.token, lang)
      } else {
        await api('/v1/automation/rules', { method: 'POST', body: JSON.stringify(payload) }, auth.token, lang)
      }
      setAutomationRuleForm({ id: '', name: '', triggerType: 'LEAD_CREATED', triggerExpr: '{}', actionType: 'CREATE_TASK', actionPayload: '{"title":"Follow up lead"}', enabled: true })
      await loadAutomationRulesV1()
    } catch (err) { handleError(err) }
  }

  const changePermission = async (roleKey, opKey, grant) => {
    if (!canManagePermissions) return
    try {
      const payload = grant ? { grant: [opKey], revoke: [] } : { grant: [], revoke: [opKey] }
      const d = await api('/permissions/roles/' + roleKey, { method: 'PATCH', body: JSON.stringify(payload) }, auth.token, lang)
      setPermissionMatrix(d.matrix || [])
      await loadPermissionConflicts()
    } catch (err) { handleError(err) }
  }
  const previewPermissionPack = async (type) => {
    if (!canManagePermissions) return
    try {
      const payload = type === 'grant-read' ? { grant: READ_OPS, revoke: [] } : { grant: [], revoke: WRITE_OPS }
      const d = await api('/permissions/roles/' + permissionRole + '/preview', { method: 'POST', body: JSON.stringify(payload) }, auth.token, lang)
      setPermissionPreview(d)
      setPendingPack(type)
    } catch (err) { handleError(err) }
  }
  const applyPermissionPack = async (type) => {
    if (!canManagePermissions) return
    try {
      const payload = type === 'grant-read' ? { grant: READ_OPS, revoke: [] } : { grant: [], revoke: WRITE_OPS }
      const d = await api('/permissions/roles/' + permissionRole, { method: 'PATCH', body: JSON.stringify(payload) }, auth.token, lang)
      setPermissionMatrix(d.matrix || [])
      await loadPermissionConflicts()
    } catch (err) { handleError(err) }
  }
  const commitPendingPack = async () => {
    if (!pendingPack) return
    await applyPermissionPack(pendingPack)
    setPendingPack('')
    setPermissionPreview(null)
  }
  const rollbackPermissionRole = async () => {
    try {
      const d = await api('/permissions/roles/' + permissionRole + '/rollback', { method: 'POST' }, auth.token, lang)
      setPermissionMatrix(d.matrix || [])
      await loadPermissionConflicts()
      setPendingPack('')
      setPermissionPreview(null)
    } catch (err) { handleError(err) }
  }

  const saveAdminUser = async (u) => {
    const roleValue = String(u.role || '').trim().toUpperCase()
    const ownerScopeValue = String(u.ownerScope || '').trim()
    if (!ROLES.includes(roleValue)) { setError(t('invalidRoleText')); return }
    if (roleValue === 'SALES' && !ownerScopeValue) { setError(t('ownerScopeRequired')); return }
    if (ownerScopeValue.length > 64) { setError(t('ownerScopeTooLong')); return }
    try {
      const d = await api('/v1/admin/users/' + u.id, { method: 'PATCH', body: JSON.stringify({ role: roleValue, ownerScope: ownerScopeValue, enabled: !!u.enabled }) }, auth.token, lang)
      setAdminUsers((prev) => prev.map((x) => x.id === d.id ? d : x))
      setError('')
    } catch (err) { handleError(err) }
  }
  const unlockAdminUser = async (id) => {
    try {
      const d = await api('/v1/admin/users/' + id + '/unlock', { method: 'POST' }, auth.token, lang)
      setAdminUsers((prev) => prev.map((x) => x.id === d.id ? d : x))
    } catch (err) { handleError(err) }
  }
  const getAdminUserError = (u) => {
    const roleValue = String(u?.role || '').trim().toUpperCase()
    const ownerScopeValue = String(u?.ownerScope || '').trim()
    if (!ROLES.includes(roleValue)) return t('invalidRoleText')
    if (roleValue === 'SALES' && !ownerScopeValue) return t('ownerScopeRequired')
    if (ownerScopeValue.length > 64) return t('ownerScopeTooLong')
    return ''
  }
  const inviteUser = async () => {
    try {
      const invited = await api('/v1/admin/users/invite', { method: 'POST', body: JSON.stringify(inviteForm) }, auth.token, lang)
      setInviteResult(invited)
      setInviteForm((p) => ({ ...p, username: '', ownerScope: '' }))
      await loadAdminUsers()
    } catch (err) { handleError(err) }
  }

  const createTenant = async () => {
    try {
      const created = await api('/v1/tenants', {
        method: 'POST',
        body: JSON.stringify({
          name: tenantForm.name,
          status: tenantForm.status,
          quotaUsers: Number(tenantForm.quotaUsers || 100),
          timezone: tenantForm.timezone,
          currency: tenantForm.currency,
          dateFormat: normalizeDateFormat(tenantForm.dateFormat),
          marketProfile: tenantForm.marketProfile || 'CN',
          taxRule: tenantForm.taxRule || 'VAT_CN',
          approvalMode: tenantForm.approvalMode || 'STRICT',
          channels: tenantForm.channels || '[\"WECOM\",\"DINGTALK\"]',
          dataResidency: tenantForm.dataResidency || 'CN',
          maskLevel: tenantForm.maskLevel || 'STANDARD',
        }),
      }, auth.token, lang)
      setLastCreatedTenant(created)
      setTenantForm((p) => ({ ...p, name: '' }))
      await loadTenants()
    } catch (err) { handleError(err) }
  }

  const updateTenant = async (row) => {
    try {
      const normalizedDateFormat = normalizeDateFormat(row.dateFormat)
      const updated = await api('/v1/tenants/' + row.id, {
        method: 'PATCH',
        body: JSON.stringify({
          name: row.name,
          status: row.status,
          quotaUsers: Number(row.quotaUsers || 0),
          timezone: row.timezone,
          currency: row.currency,
          dateFormat: normalizedDateFormat,
          marketProfile: row.marketProfile || 'CN',
          taxRule: row.taxRule || 'VAT_CN',
          approvalMode: row.approvalMode || 'STRICT',
          channels: row.channels || '[\"WECOM\",\"DINGTALK\"]',
          dataResidency: row.dataResidency || 'CN',
          maskLevel: row.maskLevel || 'STANDARD',
        }),
      }, auth.token, lang)
      if (row.id === auth?.tenantId) {
        await api('/v2/tenant-config', {
          method: 'PATCH',
          body: JSON.stringify({
            marketProfile: row.marketProfile || 'CN',
            taxRule: row.taxRule || 'VAT_CN',
            approvalMode: row.approvalMode || 'STRICT',
            channels: row.channels || '[\"WECOM\",\"DINGTALK\"]',
            dataResidency: row.dataResidency || 'CN',
            maskLevel: row.maskLevel || 'STANDARD',
          }),
        }, auth.token, lang)
      }
      setTenantRows((prev) => prev.map((x) => (x.id === updated.id ? {
        ...x,
        ...updated,
        dateFormat: normalizeDateFormat(updated.dateFormat || normalizedDateFormat),
        marketProfile: row.marketProfile || x.marketProfile || 'CN',
        taxRule: row.taxRule || x.taxRule || 'VAT_CN',
        approvalMode: row.approvalMode || x.approvalMode || 'STRICT',
        channels: row.channels || x.channels || '[\"WECOM\",\"DINGTALK\"]',
        dataResidency: row.dataResidency || x.dataResidency || 'CN',
        maskLevel: row.maskLevel || x.maskLevel || 'STANDARD',
      } : x)))
    } catch (err) { handleError(err) }
  }

  return {
    saveLeadAssignmentRule,
    saveAutomationRule,
    changePermission,
    previewPermissionPack,
    commitPendingPack,
    rollbackPermissionRole,
    saveAdminUser,
    unlockAdminUser,
    getAdminUserError,
    inviteUser,
    createTenant,
    updateTenant,
  }
}
