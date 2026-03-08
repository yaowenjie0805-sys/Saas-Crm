
import { useMemo, useState } from 'react'
import DashboardPanel from './pages/DashboardPanel'
import PermissionsPanel from './pages/PermissionsPanel'
import AuditPanel from './pages/AuditPanel'
import CustomersPanel from './pages/CustomersPanel'
import PipelinePanel from './pages/PipelinePanel'
import TasksPanel from './pages/TasksPanel'
import FollowUpsPanel from './pages/FollowUpsPanel'
import ContactsPanel from './pages/ContactsPanel'
import ContractsPanel from './pages/ContractsPanel'
import PaymentsPanel from './pages/PaymentsPanel'
import { ROLES, translateRole } from '../shared'

function MainContent({ base, permissions, users, customers, pipeline, followUps, contacts, contracts, payments, tasks, audit, approvals, tenants }) {
  const { currentPageLabel, lang, setLang, loadAll, t, canWrite, error, loading, activePage, stats, reports } = base
  const { permissionRole, setPermissionRole, canManagePermissions, previewPermissionPack, pendingPack, commitPendingPack, rollbackPermissionRole, permissionPreview, permissionMatrix, changePermission, permissionConflicts } = permissions
  const { canManageUsers, adminUsers, loadAdminUsers, setAdminUsers, getAdminUserError, saveAdminUser, unlockAdminUser, inviteForm, setInviteForm, inviteUser, inviteResult } = users
  const [editingTemplateId, setEditingTemplateId] = useState('')
  const [selectedNodeIndex, setSelectedNodeIndex] = useState(0)
  const [expandedVersionId, setExpandedVersionId] = useState('')
  const [previewCtx, setPreviewCtx] = useState({ amount: '0', role: 'SALES', department: 'DEFAULT' })

  const editingTemplate = useMemo(() => (approvals.templates || []).find((x) => x.id === editingTemplateId) || null, [approvals.templates, editingTemplateId])
  const editingNodes = useMemo(() => Array.isArray(editingTemplate?.flowDefinition?.nodes) ? editingTemplate.flowDefinition.nodes : [], [editingTemplate])
  const previewMatched = useMemo(() => {
    const amount = Number(previewCtx.amount || 0)
    return editingNodes.filter((node) => {
      const c = node.conditions || {}
      if (c.amountMin !== null && c.amountMin !== undefined && c.amountMin !== '' && amount < Number(c.amountMin)) return false
      if (c.amountMax !== null && c.amountMax !== undefined && c.amountMax !== '' && amount > Number(c.amountMax)) return false
      if (c.role && String(c.role).toUpperCase() !== String(previewCtx.role || '').toUpperCase()) return false
      if (c.department && String(c.department).toUpperCase() !== String(previewCtx.department || '').toUpperCase()) return false
      return true
    }).map((x) => x.id)
  }, [editingNodes, previewCtx])

  const applyTemplatePatch = (templateId, patcher) => approvals.setTemplates((prev) => prev.map((x) => x.id === templateId ? patcher(x) : x))
  const updateNode = (index, patch) => { if (!editingTemplate) return; const nextNodes = editingNodes.map((node, idx) => idx === index ? { ...node, ...patch } : node); applyTemplatePatch(editingTemplate.id, (x) => ({ ...x, flowDefinition: { ...(x.flowDefinition || {}), nodes: nextNodes } })) }
  const moveNode = (from, to) => { if (!editingTemplate || from === to || from < 0 || to < 0 || from >= editingNodes.length || to >= editingNodes.length) return; const arr = editingNodes.map((n) => ({ ...n })); const [moved] = arr.splice(from, 1); arr.splice(to, 0, moved); const normalized = arr.map((n, idx) => ({ ...n, seq: idx + 1 })); applyTemplatePatch(editingTemplate.id, (x) => ({ ...x, flowDefinition: { ...(x.flowDefinition || {}), nodes: normalized } })); setSelectedNodeIndex(to) }
  const addNode = () => { if (!editingTemplate) return; const nextSeq = editingNodes.length + 1; const node = { id: `node_${nextSeq}`, seq: nextSeq, approverRoles: ['MANAGER'], conditions: {}, slaMinutes: 240, escalateToRoles: ['ADMIN'] }; applyTemplatePatch(editingTemplate.id, (x) => ({ ...x, flowDefinition: { ...(x.flowDefinition || {}), nodes: [...editingNodes, node] } })); setSelectedNodeIndex(editingNodes.length) }
  const formatDateTime = (value) => { const text = String(value || ''); return text ? text.replace('T', ' ').slice(0, 19) : '-' }
  const summarizeVersionDiff = (currentTemplate, versionRow) => {
    const backendDiff = versionRow?.diffSummary
    if (backendDiff && Array.isArray(backendDiff.changes)) {
      return `${t('approvalDiffNode')}: ${backendDiff.nodeDelta >= 0 ? '+' : ''}${backendDiff.nodeDelta} | ${t('approvalDiffChangeCount')}: ${backendDiff.changes.length}`
    }
    if (!currentTemplate || !versionRow) return t('noData')
    const currentNodes = Array.isArray(currentTemplate.flowDefinition?.nodes) ? currentTemplate.flowDefinition.nodes : []
    const versionNodes = Array.isArray(versionRow.flowDefinition?.nodes) ? versionRow.flowDefinition.nodes : []
    const nodeDelta = currentNodes.length - versionNodes.length
    const currentSla = currentNodes.reduce((acc, row) => acc + Number(row?.slaMinutes || 0), 0)
    const versionSla = versionNodes.reduce((acc, row) => acc + Number(row?.slaMinutes || 0), 0)
    return `${t('approvalDiffNode')}: ${nodeDelta >= 0 ? '+' : ''}${nodeDelta} | ${t('approvalDiffSla')}: ${currentSla - versionSla >= 0 ? '+' : ''}${currentSla - versionSla}`
  }
  const summarizeNodeChange = (row) => {
    if (!row?.delta || typeof row.delta !== 'object' || Object.keys(row.delta).length === 0) return row?.type || '-'
    return Object.entries(row.delta).map(([key, val]) => `${key}: ${val?.from || '-'} -> ${val?.to || '-'}`).join(' | ')
  }
  const confirmRollback = (row) => {
    const hint = summarizeVersionDiff(editingTemplate, row)
    const ok = window.confirm(`${t('approvalRollbackConfirm')} v${row.version}\n${hint}`)
    if (!ok) return
    approvals.rollbackTemplate(row.templateId, row.version)
  }

  return (
    <main className="content">
      <header className="topbar">
        <div><h1>{currentPageLabel}</h1></div>
        <div className="top-actions">
          <input className="tool-input" style={{ minWidth: 180 }} placeholder={t('globalSearch')} />
          <button className="mini-btn">{t('notifications')}</button>
          <div className="language-switch">
            <button className={lang === 'zh' ? 'active' : ''} onClick={() => setLang('zh')}>ZH</button>
            <button className={lang === 'en' ? 'active' : ''} onClick={() => setLang('en')}>EN</button>
          </div>
          <button className="primary-btn" onClick={loadAll}>{t('refresh')}</button>
        </div>
      </header>

      {!canWrite && <div className="info-banner">{t('readOnly')}</div>}
      {error && <div className="error-banner">{error}</div>}
      {loading && <div className="loading">{t('loading')}</div>}

      <DashboardPanel activePage={activePage} stats={stats} reports={reports} t={t} canViewReports={base.canViewReports} auditFrom={base.auditFrom} auditTo={base.auditTo} auditRole={base.auditRole} reportOwner={base.reportOwner} setReportOwner={base.setReportOwner} reportDepartment={base.reportDepartment} setReportDepartment={base.setReportDepartment} reportTimezone={base.reportTimezone} setReportTimezone={base.setReportTimezone} reportCurrency={base.reportCurrency} setReportCurrency={base.setReportCurrency} exportReportCsv={base.exportReportCsv} reportExportJobs={base.reportExportJobs} reportExportStatusFilter={base.reportExportStatusFilter} setReportExportStatusFilter={base.setReportExportStatusFilter} autoRefreshReportJobs={base.autoRefreshReportJobs} setAutoRefreshReportJobs={base.setAutoRefreshReportJobs} loadReportExportJobs={base.loadReportExportJobs} retryReportExportJob={base.retryReportExportJob} downloadReportExportJob={base.downloadReportExportJob} />

      <PermissionsPanel activePage={activePage} t={t} permissionRole={permissionRole} setPermissionRole={setPermissionRole} canManagePermissions={canManagePermissions} previewPermissionPack={previewPermissionPack} pendingPack={pendingPack} commitPendingPack={commitPendingPack} rollbackPermissionRole={rollbackPermissionRole} permissionPreview={permissionPreview} permissionMatrix={permissionMatrix} changePermission={changePermission} permissionConflicts={permissionConflicts} />

      {activePage === 'usersAdmin' && canManageUsers && (
        <section className="panel">
          <div className="panel-head"><h2>{t('usersAdmin')}</h2><button className="mini-btn" onClick={loadAdminUsers}>{t('refresh')}</button></div>
          <div className="inline-tools" style={{ marginBottom: 10 }}>
            <input className="tool-input" placeholder={t('username')} value={inviteForm.username} onChange={(e) => setInviteForm((p) => ({ ...p, username: e.target.value }))} />
            <select className="tool-input" value={inviteForm.role} onChange={(e) => setInviteForm((p) => ({ ...p, role: e.target.value }))}>
              {ROLES.map((r) => <option key={r} value={r}>{translateRole(t, r)}</option>)}
            </select>
            <input className="tool-input" placeholder={t('ownerScope')} value={inviteForm.ownerScope} onChange={(e) => setInviteForm((p) => ({ ...p, ownerScope: e.target.value }))} />
            <button className="mini-btn" onClick={inviteUser}>{t('inviteActivateBtn')}</button>
          </div>
          {inviteResult?.inviteLink && <div className="info-banner" style={{ marginBottom: 10 }}>{t('inviteLink')}: {inviteResult.inviteLink}</div>}
          {adminUsers.map((u) => (
            <div key={u.id || u.username} className="table-row admin-user-row">
              <span>{u.username}</span>
              <select className="tool-input" value={u.role} onChange={(e) => setAdminUsers((prev) => prev.map((x) => x.id === u.id ? { ...x, role: e.target.value } : x))}>
                {ROLES.map((r) => <option key={r} value={r}>{translateRole(t, r)}</option>)}
              </select>
              <div className="admin-scope-cell">
                <input className={getAdminUserError(u) ? 'tool-input input-invalid' : 'tool-input'} value={u.ownerScope || ''} placeholder={t('ownerScope')} onChange={(e) => setAdminUsers((prev) => prev.map((x) => x.id === u.id ? { ...x, ownerScope: e.target.value } : x))} />
                {getAdminUserError(u) && <span className="field-error">{getAdminUserError(u)}</span>}
              </div>
              <label className="switch-inline"><input type="checkbox" checked={!!u.enabled} onChange={(e) => setAdminUsers((prev) => prev.map((x) => x.id === u.id ? { ...x, enabled: e.target.checked } : x))} />{t('enabled')}</label>
              <span>{u.locked ? t('locked') : '-'}</span>
              <span>{u.lockRemainingSeconds || 0}s</span>
              <div className="admin-action-cell"><button className="mini-btn" disabled={!!getAdminUserError(u)} onClick={() => saveAdminUser(u)}>{t('save')}</button><button className="mini-btn" onClick={() => unlockAdminUser(u.id)}>{t('unlock')}</button></div>
            </div>
          ))}
          {adminUsers.length === 0 && <div className="empty-tip">{t('noData')}</div>}
        </section>
      )}

      <CustomersPanel activePage={activePage} t={t} canWrite={canWrite} loading={loading} {...customers} />
      <PipelinePanel activePage={activePage} t={t} canWrite={canWrite} loading={loading} {...pipeline} />
      <FollowUpsPanel activePage={activePage} t={t} canWrite={canWrite} {...followUps} />
      <ContactsPanel activePage={activePage} t={t} canWrite={canWrite} loading={loading} {...contacts} />
      <ContractsPanel activePage={activePage} t={t} canWrite={canWrite} loading={loading} canDeleteCustomer={customers.canDeleteCustomer} {...contracts} />
      <PaymentsPanel activePage={activePage} t={t} canWrite={canWrite} loading={loading} canDeleteCustomer={customers.canDeleteCustomer} {...payments} />
      <TasksPanel activePage={activePage} t={t} canWrite={canWrite} {...tasks} />

      {activePage === 'approvals' && (
        <section className="panel">
          <div className="panel-head"><h2>{t('approvals')}</h2></div>
          <section className="stats-grid" style={{ marginBottom: 12 }}>
            <article className="stat-card"><p>{t('approvalTemplatesCount')}</p><h3>{approvals.stats?.summary?.templates || 0}</h3><span>Templates</span></article>
            <article className="stat-card"><p>{t('approvalInstancesCount')}</p><h3>{approvals.stats?.summary?.instances || 0}</h3><span>Instances</span></article>
            <article className="stat-card"><p>{t('approvalTasksCount')}</p><h3>{approvals.stats?.summary?.tasks || 0}</h3><span>Tasks</span></article>
            <article className="stat-card"><p>{t('approvalPendingCount')}</p><h3>{approvals.stats?.summary?.pendingTasks || 0}</h3><span>Pending</span></article>
            <article className="stat-card"><p>SLA Overdue</p><h3>{approvals.stats?.sla?.overdueCount || 0}</h3><span>Overdue</span></article>
            <article className="stat-card"><p>SLA Escalated</p><h3>{approvals.stats?.sla?.escalatedCount || 0}</h3><span>Escalated</span></article>
          </section>

          <div className="inline-tools">
            <select className="tool-input" value={approvals.template.bizType} onChange={(e) => approvals.setTemplate((p) => ({ ...p, bizType: e.target.value }))}>
              <option value="CONTRACT">CONTRACT</option>
              <option value="PAYMENT">PAYMENT</option>
            </select>
            <input className="tool-input" placeholder={t('title')} value={approvals.template.name} onChange={(e) => approvals.setTemplate((p) => ({ ...p, name: e.target.value }))} />
            <input className="tool-input" placeholder={t('approverRoles')} value={approvals.template.approverRoles} onChange={(e) => approvals.setTemplate((p) => ({ ...p, approverRoles: e.target.value }))} />
            <button className="mini-btn" onClick={approvals.createTemplate}>{t('createTemplate')}</button>
            <button className="mini-btn" onClick={approvals.reloadTemplates}>{t('refresh')}</button>
          </div>

          <div className="approval-editor-grid">
            <div className="panel" style={{ marginTop: 10, boxShadow: 'none' }}>
              <div className="panel-head"><h2>{t('approvalTemplateMatrix')}</h2></div>
              <div className="table-row table-head-row">
                <span>ID</span><span>BizType</span><span>{t('title')}</span><span>{t('status')}</span><span>{t('action')}</span>
              </div>
              {(approvals.templates || []).map((row) => (
                <div key={row.id} className={`table-row ${editingTemplateId === row.id ? 'row-active' : ''}`}>
                  <span>{row.id}</span>
                  <span>{row.bizType}</span>
                  <span><input className="tool-input" value={row.name || ''} onChange={(e) => approvals.setTemplates((prev) => prev.map((x) => x.id === row.id ? { ...x, name: e.target.value } : x))} /></span>
                  <span>
                    <div className="inline-tools">
                      <label className="switch-inline"><input type="checkbox" checked={!!row.enabled} onChange={(e) => approvals.setTemplates((prev) => prev.map((x) => x.id === row.id ? { ...x, enabled: e.target.checked } : x))} />{t('enabled')}</label>
                      <span>{row.status || '-'}</span>
                      <span>v{row.activeVersion || row.version || '-'}</span>
                    </div>
                  </span>
                  <span>
                    <div className="inline-tools">
                      <button className="mini-btn" onClick={() => { setEditingTemplateId(row.id); setSelectedNodeIndex(0) }}>{t('approvalEditFlow')}</button>
                      <button className="mini-btn" onClick={() => { approvals.setTemplates((prev) => prev.map((x) => x.id === row.id ? { ...x, status: 'DRAFT', enabled: false } : x)); approvals.updateTemplate({ ...row, status: 'DRAFT', enabled: false }) }}>{t('approvalDraft')}</button>
                      <button className="mini-btn" onClick={() => approvals.publishTemplate(row.id)}>{t('approvalPublish')}</button>
                      <button className="mini-btn" onClick={() => approvals.loadVersions(row.id)}>{t('approvalVersionHistory')}</button>
                      <button className="mini-btn" onClick={() => approvals.updateTemplate(row)}>{t('save')}</button>
                    </div>
                  </span>
                </div>
              ))}
            </div>

            <div className="panel" style={{ marginTop: 10, boxShadow: 'none' }}>
              <div className="panel-head"><h2>{t('approvalFlowNodes')}</h2><button className="mini-btn" disabled={!editingTemplate} onClick={addNode}>{t('approvalAddNode')}</button></div>
              {!editingTemplate && <div className="empty-tip">{t('approvalSelectTemplate')}</div>}
              {!!editingTemplate && (
                <div>
                  {editingNodes.map((node, idx) => (
                    <div key={`${node.id}-${idx}`} className={`approval-node-item ${selectedNodeIndex === idx ? 'active' : ''}`}>
                      <button className="mini-btn" onClick={() => moveNode(idx, idx - 1)} disabled={idx === 0}>{t('moveUp')}</button>
                      <button className="mini-btn" onClick={() => moveNode(idx, idx + 1)} disabled={idx === editingNodes.length - 1}>{t('moveDown')}</button>
                      <button className="mini-btn" onClick={() => setSelectedNodeIndex(idx)}>#{node.seq || idx + 1}</button>
                      <span>{(node.approverRoles || []).join(',') || 'N/A'}</span>
                    </div>
                  ))}
                  {editingNodes[selectedNodeIndex] && (
                    <div className="panel" style={{ marginTop: 10 }}>
                      <div className="panel-head"><h2>{t('approvalNodeConfig')}</h2></div>
                      <div className="inline-tools">
                        <input className="tool-input" value={editingNodes[selectedNodeIndex]?.id || ''} onChange={(e) => updateNode(selectedNodeIndex, { id: e.target.value })} placeholder={t('approvalNodeId')} />
                        <input className="tool-input" value={(editingNodes[selectedNodeIndex]?.approverRoles || []).join(',')} onChange={(e) => updateNode(selectedNodeIndex, { approverRoles: e.target.value.split(',').map((x) => x.trim().toUpperCase()).filter(Boolean) })} placeholder={t('approverRoles')} />
                        <input className="tool-input" value={String(editingNodes[selectedNodeIndex]?.slaMinutes || 240)} onChange={(e) => updateNode(selectedNodeIndex, { slaMinutes: Number(e.target.value || 0) })} placeholder={t('approvalSlaMinutes')} />
                        <input className="tool-input" value={(editingNodes[selectedNodeIndex]?.escalateToRoles || []).join(',')} onChange={(e) => updateNode(selectedNodeIndex, { escalateToRoles: e.target.value.split(',').map((x) => x.trim().toUpperCase()).filter(Boolean) })} placeholder={t('approvalEscalateRoles')} />
                      </div>
                      <div className="inline-tools">
                        <input className="tool-input" value={String(editingNodes[selectedNodeIndex]?.conditions?.amountMin ?? '')} onChange={(e) => updateNode(selectedNodeIndex, { conditions: { ...(editingNodes[selectedNodeIndex]?.conditions || {}), amountMin: e.target.value === '' ? null : Number(e.target.value) } })} placeholder={t('approvalAmountMin')} />
                        <input className="tool-input" value={String(editingNodes[selectedNodeIndex]?.conditions?.amountMax ?? '')} onChange={(e) => updateNode(selectedNodeIndex, { conditions: { ...(editingNodes[selectedNodeIndex]?.conditions || {}), amountMax: e.target.value === '' ? null : Number(e.target.value) } })} placeholder={t('approvalAmountMax')} />
                        <input className="tool-input" value={editingNodes[selectedNodeIndex]?.conditions?.role || ''} onChange={(e) => updateNode(selectedNodeIndex, { conditions: { ...(editingNodes[selectedNodeIndex]?.conditions || {}), role: e.target.value.toUpperCase() } })} placeholder={t('approvalRoleCondition')} />
                        <input className="tool-input" value={editingNodes[selectedNodeIndex]?.conditions?.department || ''} onChange={(e) => updateNode(selectedNodeIndex, { conditions: { ...(editingNodes[selectedNodeIndex]?.conditions || {}), department: e.target.value } })} placeholder={t('approvalDepartmentCondition')} />
                      </div>
                      <div className="info-banner">{t('approvalConditionPreview')}: {(editingNodes[selectedNodeIndex]?.conditions?.amountMin ?? '-')}/{(editingNodes[selectedNodeIndex]?.conditions?.amountMax ?? '-')} | {editingNodes[selectedNodeIndex]?.conditions?.role || 'ANY'} | {editingNodes[selectedNodeIndex]?.conditions?.department || 'ANY'}</div>
                      <div className="inline-tools" style={{ marginTop: 8 }}>
                        <input className="tool-input" placeholder={t('approvalPreviewAmount')} value={previewCtx.amount} onChange={(e) => setPreviewCtx((p) => ({ ...p, amount: e.target.value }))} />
                        <input className="tool-input" placeholder={t('approvalPreviewRole')} value={previewCtx.role} onChange={(e) => setPreviewCtx((p) => ({ ...p, role: e.target.value.toUpperCase() }))} />
                        <input className="tool-input" placeholder={t('approvalPreviewDepartment')} value={previewCtx.department} onChange={(e) => setPreviewCtx((p) => ({ ...p, department: e.target.value }))} />
                      </div>
                      <div className="info-banner" style={{ marginTop: 8 }}>{t('approvalMatchedNodes')}: {previewMatched.length ? previewMatched.join(', ') : t('noData')}</div>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>

          {approvals.versionTemplateId && (
            <div className="panel" style={{ marginTop: 12, boxShadow: 'none' }}>
              <div className="panel-head">
                <h2>{t('approvalVersionHistory')}</h2>
                <button className="mini-btn" onClick={() => approvals.loadVersions(approvals.versionTemplateId)}>{t('refresh')}</button>
              </div>
              <div className="table-row table-head-row">
                <span>{t('version')}</span><span>{t('status')}</span><span>{t('createdAt')}</span><span>{t('user')}</span><span>{t('summary')}</span><span>{t('action')}</span>
              </div>
              {(approvals.versions || []).map((v) => (
                <div key={v.id || `${v.templateId}-${v.version}`}>
                  <div className="table-row">
                    <span>v{v.version}</span>
                    <span>{v.status || '-'}</span>
                    <span>{formatDateTime(v.publishedAt)}</span>
                    <span>{v.publishedBy || '-'}</span>
                    <span>{summarizeVersionDiff(editingTemplate, v)}</span>
                    <span>
                      <div className="inline-tools">
                        <button className="mini-btn" onClick={() => setExpandedVersionId(expandedVersionId === v.id ? '' : (v.id || `${v.templateId}-${v.version}`))}>{t('detail')}</button>
                        <button className="mini-btn" onClick={() => confirmRollback(v)}>{t('rollbackRole')}</button>
                      </div>
                    </span>
                  </div>
                  {expandedVersionId === (v.id || `${v.templateId}-${v.version}`) && (
                    <div className="panel" style={{ marginTop: 6 }}>
                      <div className="panel-head"><h2>{t('approvalDiffDetail')}</h2></div>
                      <div className="table-row table-head-row">
                        <span>{t('idLabel')}</span><span>{t('type')}</span><span>{t('summary')}</span>
                      </div>
                      {(v.diffSummary?.changes || []).map((c, idx) => (
                        <div key={`${v.id || idx}-${idx}`} className="table-row">
                          <span>{c.nodeKey || '-'}</span>
                          <span>{c.type || '-'}</span>
                          <span>{summarizeNodeChange(c)}</span>
                        </div>
                      ))}
                      {!(v.diffSummary?.changes || []).length && <div className="empty-tip">{t('noData')}</div>}
                    </div>
                  )}
                </div>
              ))}
              {(approvals.versions || []).length === 0 && <div className="empty-tip">{t('noData')}</div>}
            </div>
          )}

          <div className="inline-tools" style={{ marginTop: 10 }}>
            <select className="tool-input" value={approvals.instance.bizType} onChange={(e) => approvals.setInstance((p) => ({ ...p, bizType: e.target.value }))}>
              <option value="CONTRACT">CONTRACT</option>
              <option value="PAYMENT">PAYMENT</option>
            </select>
            <input className="tool-input" placeholder={t('bizId')} value={approvals.instance.bizId} onChange={(e) => approvals.setInstance((p) => ({ ...p, bizId: e.target.value }))} />
            <input className="tool-input" placeholder={t('amount')} value={approvals.instance.amount} onChange={(e) => approvals.setInstance((p) => ({ ...p, amount: e.target.value }))} />
            <button className="mini-btn" onClick={approvals.submitInstance}>{t('submit')}</button>
          </div>

          <div className="inline-tools" style={{ marginTop: 10 }}>
            <select className="tool-input" value={approvals.taskStatus} onChange={(e) => approvals.setTaskStatus(e.target.value)}>
              <option value="PENDING">PENDING</option>
              <option value="WAITING">WAITING</option>
              <option value="APPROVED">APPROVED</option>
              <option value="REJECTED">REJECTED</option>
              <option value="ESCALATED">ESCALATED</option>
              <option value="">ALL</option>
            </select>
            <label className="switch-inline"><input type="checkbox" checked={!!approvals.overdueOnly} onChange={(e) => approvals.setOverdueOnly(e.target.checked)} />{t('approvalOverdue')}</label>
            <label className="switch-inline"><input type="checkbox" checked={!!approvals.escalatedOnly} onChange={(e) => approvals.setEscalatedOnly(e.target.checked)} />{t('approvalEscalated')}</label>
            <input className="tool-input" placeholder={t('remark')} value={approvals.actionComment} onChange={(e) => approvals.setActionComment(e.target.value)} />
            <input className="tool-input" placeholder={t('transferTo')} value={approvals.transferTo} onChange={(e) => approvals.setTransferTo(e.target.value)} />
            <button className="mini-btn" onClick={approvals.reloadTasks}>{t('refresh')}</button>
          </div>

          <div className="table-row table-head-row" style={{ marginTop: 10 }}>
            <span>ID</span><span>{t('status')}</span><span>{t('role')}</span><span>{t('action')}</span><span>{t('createdAt')}</span>
          </div>
          {(approvals.tasks || []).map((task) => (
            <div key={task.id} className="table-row">
              <span>{task.id}</span>
              <span>{task.status}</span>
              <span>{task.approverRole}{task.overdue ? ` / ${t('approvalOverdue').toUpperCase()}` : ''}</span>
              <span>
                <button className="mini-btn" onClick={() => approvals.actTask(task.id, 'approve')}>{t('approvalApprove')}</button>
                <button className="mini-btn" onClick={() => approvals.actTask(task.id, 'reject')}>{t('approvalReject')}</button>
                <button className="mini-btn" onClick={() => approvals.actTask(task.id, 'transfer')}>{t('approvalTransfer')}</button>
                <button className="mini-btn" onClick={() => approvals.urgeTask(task.id)}>{t('approvalUrge')}</button>
              </span>
              <span>{formatDateTime(task.createdAt)}</span>
            </div>
          ))}

          <div className="panel" style={{ marginTop: 12, boxShadow: 'none' }}>
            <div className="panel-head">
              <h2>{t('notifications')}</h2>
              <button className="mini-btn" onClick={approvals.reloadNotifications}>{t('refresh')}</button>
            </div>
            <div className="inline-tools">
              <select className="tool-input" value={approvals.notificationStatus} onChange={(e) => approvals.setNotificationStatus(e.target.value)}>
                <option value="ALL">ALL</option>
                <option value="PENDING">PENDING</option>
                <option value="RUNNING">RUNNING</option>
                <option value="RETRY">RETRY</option>
                <option value="SUCCESS">SUCCESS</option>
                <option value="FAILED">FAILED</option>
              </select>
              <select className="tool-input" value={approvals.notificationSize} onChange={(e) => approvals.setNotificationSize(Number(e.target.value || 10))}>
                {[10, 20, 50].map((n) => <option key={n} value={n}>{n}</option>)}
              </select>
              <button className="mini-btn" disabled={!approvals.canRetryNotifications || (approvals.selectedNotificationJobs || []).length === 0} onClick={approvals.retryNotificationByIds}>{t('retrySelected')}</button>
              <button className="mini-btn" disabled={!approvals.canRetryNotifications} onClick={approvals.retryNotificationByFilter}>{t('retryFailedInFilter')}</button>
            </div>
            <div className="table-row table-head-row">
              <span><input type="checkbox" checked={(approvals.notifications || []).length > 0 && (approvals.notifications || []).every((j) => (approvals.selectedNotificationJobs || []).includes(j.jobId))} onChange={(e) => approvals.toggleAllNotificationJobs(e.target.checked)} /></span>
              <span>ID</span><span>{t('status')}</span><span>Target</span><span>{t('retry')}</span><span>{t('errorReason')}</span><span>{t('createdAt')}</span><span>{t('action')}</span>
            </div>
            {(approvals.notifications || []).map((job) => (
              <div key={job.jobId} className="table-row">
                <span><input type="checkbox" checked={(approvals.selectedNotificationJobs || []).includes(job.jobId)} onChange={(e) => approvals.toggleNotificationJob(job.jobId, e.target.checked)} /></span>
                <span>{job.jobId}</span>
                <span>{job.status}</span>
                <span>{job.target}</span>
                <span>{job.retryCount || 0}/{job.maxRetries || 0}</span>
                <span>{job.lastError || '-'}</span>
                <span>{formatDateTime(job.updatedAt || job.createdAt)}</span>
                <span><button className="mini-btn" disabled={job.status !== 'FAILED'} onClick={() => approvals.retryNotification(job.jobId)}>{t('retry')}</button></span>
              </div>
            ))}
            {(approvals.notifications || []).length === 0 && <div className="empty-tip">{t('noData')}</div>}
            <div className="inline-tools" style={{ marginTop: 8 }}>
              <button className="mini-btn" disabled={approvals.notificationPage <= 1} onClick={() => approvals.setNotificationPage(Math.max(1, approvals.notificationPage - 1))}>{t('pagePrev')}</button>
              <span>{approvals.notificationPage}/{approvals.notificationTotalPages}</span>
              <button className="mini-btn" disabled={approvals.notificationPage >= approvals.notificationTotalPages} onClick={() => approvals.setNotificationPage(Math.min(approvals.notificationTotalPages, approvals.notificationPage + 1))}>{t('pageNext')}</button>
            </div>
          </div>

          <div className="panel" style={{ marginTop: 12, boxShadow: 'none' }}>
            <div className="panel-head"><h2>{t('approvalInstances')}</h2></div>
            <div className="table-row table-head-row">
              <span>ID</span><span>{t('status')}</span><span>Biz</span><span>{t('version')}</span><span>{t('user')}</span><span>{t('action')}</span>
            </div>
            {(approvals.instances || []).map((ins) => (
              <div key={ins.id} className="table-row">
                <span>{ins.id}</span>
                <span>{ins.status}</span>
                <span>{ins.bizType}:{ins.bizId}</span>
                <span>v{ins.templateVersion || '-'}</span>
                <span>{ins.submitter}</span>
                <span><button className="mini-btn" onClick={() => approvals.loadDetail(ins.id)}>{t('detail')}</button></span>
              </div>
            ))}
            {approvals.detail && (
              <div className="panel" style={{ marginTop: 10 }}>
                <div className="panel-head"><h2>{t('approvalDetail')}</h2></div>
                <div className="audit-row">
                  <strong>{approvals.detail.id}</strong>
                  <span>{approvals.detail.status}</span>
                  <span>{approvals.detail.bizType}:{approvals.detail.bizId}</span>
                  <span>v{approvals.detail.templateVersion || '-'}</span>
                  <small>{formatDateTime(approvals.detail.createdAt)}</small>
                </div>
                {(approvals.detail.tasks || []).map((row) => (
                  <div key={row.id} className="audit-row">
                    <strong>#{row.seq}</strong>
                    <span>{row.approverRole}</span>
                    <span>{row.status}</span>
                    <small>{row.approverUser || '-'}</small>
                  </div>
                ))}
                {(approvals.detail.timeline || []).map((ev) => (
                  <div key={`timeline-${ev.id}`} className="audit-row">
                    <strong>{ev.eventType}</strong>
                    <span>{ev.taskId || '-'}</span>
                    <span>{ev.operatorUser || '-'}</span>
                    <small>{formatDateTime(ev.createdAt)}</small>
                  </div>
                ))}
              </div>
            )}
          </div>
        </section>
      )}

      {activePage === 'adminTenants' && (
        <section className="panel">
          <div className="panel-head"><h2>{t('tenantsAdmin')}</h2><button className="mini-btn" onClick={tenants.reload}>{t('refresh')}</button></div>
          <div className="inline-tools">
            <input className="tool-input" placeholder={t('tenantName')} value={tenants.form.name} onChange={(e) => tenants.setForm((p) => ({ ...p, name: e.target.value }))} />
            <input className="tool-input" placeholder={t('tenantQuota')} value={tenants.form.quotaUsers} onChange={(e) => tenants.setForm((p) => ({ ...p, quotaUsers: e.target.value }))} />
            <input className="tool-input" placeholder="Asia/Shanghai" value={tenants.form.timezone} onChange={(e) => tenants.setForm((p) => ({ ...p, timezone: e.target.value }))} />
            <input className="tool-input" placeholder="CNY" value={tenants.form.currency} onChange={(e) => tenants.setForm((p) => ({ ...p, currency: e.target.value }))} />
            <button className="mini-btn" onClick={tenants.createTenant}>{t('create')}</button>
          </div>
          {tenants.lastCreated && <div className="info-banner" style={{ marginTop: 10 }}>{t('tenantCreated')}: {tenants.lastCreated.id}</div>}
          <div className="table-row table-head-row" style={{ marginTop: 10 }}>
            <span>ID</span><span>{t('tenantName')}</span><span>{t('status')}</span><span>{t('tenantQuota')}</span><span>{t('tenantConfig')}</span>
          </div>
          {(tenants.rows || []).map((row) => (
            <div key={row.id} className="table-row">
              <span>{row.id}</span>
              <span><input className="tool-input" value={row.name || ''} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, name: e.target.value } : x))} /></span>
              <span>
                <select className="tool-input" value={row.status || 'ACTIVE'} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, status: e.target.value } : x))}>
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="INACTIVE">INACTIVE</option>
                </select>
              </span>
              <span><input className="tool-input" value={row.quotaUsers || ''} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, quotaUsers: e.target.value } : x))} /></span>
              <span>
                <div className="inline-tools">
                  <input className="tool-input" placeholder={t('reportTimezone')} value={row.timezone || ''} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, timezone: e.target.value } : x))} />
                  <input className="tool-input" placeholder={t('reportCurrency')} value={row.currency || ''} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, currency: e.target.value.toUpperCase() } : x))} />
                  <input className="tool-input" placeholder="YYYY-MM-DD" value={row.dateFormat || ''} onChange={(e) => tenants.setRows((prev) => prev.map((x) => x.id === row.id ? { ...x, dateFormat: e.target.value } : x))} />
                  <button className="mini-btn" onClick={() => tenants.updateTenant(row)}>{t('save')}</button>
                </div>
              </span>
            </div>
          ))}
        </section>
      )}

      <AuditPanel activePage={activePage} t={t} {...audit} />
    </main>
  )
}

export default MainContent
