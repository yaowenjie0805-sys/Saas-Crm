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

const DEFAULT_CHANNELS_BY_MARKET = {
  CN: '["WECOM","DINGTALK"]',
  GLOBAL: '["EMAIL","SLACK"]',
}

function emitTenantConfigUpdated(payload) {
  if (typeof window === 'undefined' || typeof window.dispatchEvent !== 'function') return
  try {
    window.dispatchEvent(new CustomEvent('crm:tenant-config-updated', { detail: payload || {} }))
  } catch {
    // keep save flow resilient even if event construction fails
  }
}

function withRequestIdMessage(err, fallback) {
  const message = String(err?.message || fallback || '').trim()
  const requestId = String(err?.requestId || '').trim()
  return requestId ? `${message} [${requestId}]` : message
}

function normalizeTenantPayload(raw, normalizeDateFormat) {
  const marketProfile = String(raw?.marketProfile || 'CN').trim().toUpperCase() === 'GLOBAL' ? 'GLOBAL' : 'CN'
  const currency = String(raw?.currency || '').trim().toUpperCase() || (marketProfile === 'GLOBAL' ? 'USD' : 'CNY')
  const timezone = String(raw?.timezone || '').trim() || 'Asia/Shanghai'
  const dateFormat = normalizeDateFormat(raw?.dateFormat)
  return {
    name: String(raw?.name || '').trim(),
    status: String(raw?.status || 'ACTIVE').trim().toUpperCase() || 'ACTIVE',
    quotaUsers: Number(raw?.quotaUsers || 100),
    timezone,
    currency,
    dateFormat,
    marketProfile,
    taxRule: String(raw?.taxRule || (marketProfile === 'GLOBAL' ? 'VAT_GLOBAL' : 'VAT_CN')).trim(),
    approvalMode: String(raw?.approvalMode || 'STRICT').trim().toUpperCase() === 'STAGE_GATE' ? 'STAGE_GATE' : 'STRICT',
    channels: String(raw?.channels || DEFAULT_CHANNELS_BY_MARKET[marketProfile]).trim() || DEFAULT_CHANNELS_BY_MARKET[marketProfile],
    dataResidency: String(raw?.dataResidency || marketProfile).trim().toUpperCase() || marketProfile,
    maskLevel: String(raw?.maskLevel || 'STANDARD').trim().toUpperCase() || 'STANDARD',
  }
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
      const payload = normalizeTenantPayload(tenantForm, normalizeDateFormat)
      if (!payload.name) { setError(t('fieldRequired')); return }
      const created = await api('/v1/tenants', {
        method: 'POST',
        body: JSON.stringify(payload),
      }, auth.token, lang)
      setLastCreatedTenant(created)
      setTenantForm((p) => ({ ...p, name: '' }))
      await loadTenants()
      emitTenantConfigUpdated({ tenantId: created?.id || '', source: 'create_tenant' })
      setError('')
    } catch (err) {
      setError(withRequestIdMessage(err, t('loadFailed')))
      handleError(err)
    }
  }

  const updateTenant = async (row) => {
    try {
      const payload = normalizeTenantPayload(row, normalizeDateFormat)
      if (!payload.name) { setError(t('fieldRequired')); return }
      const updated = await api('/v1/tenants/' + row.id, {
        method: 'PATCH',
        body: JSON.stringify(payload),
      }, auth.token, lang)
      if (row.id === auth?.tenantId) {
        await api('/v2/tenant-config', {
          method: 'PATCH',
          body: JSON.stringify({
            marketProfile: payload.marketProfile,
            taxRule: payload.taxRule,
            approvalMode: payload.approvalMode,
            channels: payload.channels,
            dataResidency: payload.dataResidency,
            maskLevel: payload.maskLevel,
            currency: payload.currency,
            timezone: payload.timezone,
            dateFormat: payload.dateFormat,
          }),
        }, auth.token, lang)
        emitTenantConfigUpdated({
          tenantId: row.id,
          source: 'update_current_tenant_config',
          marketProfile: payload.marketProfile,
          currency: payload.currency,
          timezone: payload.timezone,
          approvalMode: payload.approvalMode,
        })
      }
      setTenantRows((prev) => prev.map((x) => (x.id === updated.id ? {
        ...x,
        ...updated,
        dateFormat: normalizeDateFormat(updated.dateFormat || payload.dateFormat),
        marketProfile: payload.marketProfile || x.marketProfile || 'CN',
        taxRule: payload.taxRule || x.taxRule || 'VAT_CN',
        approvalMode: payload.approvalMode || x.approvalMode || 'STRICT',
        channels: payload.channels || x.channels || DEFAULT_CHANNELS_BY_MARKET[payload.marketProfile] || DEFAULT_CHANNELS_BY_MARKET.CN,
        dataResidency: payload.dataResidency || x.dataResidency || 'CN',
        maskLevel: payload.maskLevel || x.maskLevel || 'STANDARD',
        currency: payload.currency || x.currency || 'CNY',
        timezone: payload.timezone || x.timezone || 'Asia/Shanghai',
      } : x)))
      if (row.id !== auth?.tenantId) {
        emitTenantConfigUpdated({ tenantId: row.id, source: 'update_tenant_row' })
      }
      setError('')
    } catch (err) {
      setError(withRequestIdMessage(err, t('loadFailed')))
      handleError(err)
    }
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
