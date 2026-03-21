import { memo } from 'react'

function ApprovalNodeConfigPanel({
  t,
  editingNodes,
  selectedNodeIndex,
  updateNode,
  previewCtx,
  setPreviewCtx,
  previewMatched,
}) {
  const selectedNode = editingNodes[selectedNodeIndex]
  if (!selectedNode) return null

  return (
    <div className="panel" style={{ marginTop: 10 }}>
      <div className="panel-head"><h2>{t('approvalNodeConfig')}</h2></div>
      <div className="inline-tools">
        <input className="tool-input" value={selectedNode?.id || ''} onChange={(e) => updateNode(selectedNodeIndex, { id: e.target.value })} placeholder={t('approvalNodeId')} />
        <input className="tool-input" value={(selectedNode?.approverRoles || []).join(',')} onChange={(e) => updateNode(selectedNodeIndex, { approverRoles: e.target.value.split(',').map((x) => x.trim().toUpperCase()).filter(Boolean) })} placeholder={t('approverRoles')} />
        <input className="tool-input" value={String(selectedNode?.slaMinutes || 240)} onChange={(e) => updateNode(selectedNodeIndex, { slaMinutes: Number(e.target.value || 0) })} placeholder={t('approvalSlaMinutes')} />
        <input className="tool-input" value={(selectedNode?.escalateToRoles || []).join(',')} onChange={(e) => updateNode(selectedNodeIndex, { escalateToRoles: e.target.value.split(',').map((x) => x.trim().toUpperCase()).filter(Boolean) })} placeholder={t('approvalEscalateRoles')} />
      </div>
      <div className="inline-tools">
        <input className="tool-input" value={String(selectedNode?.conditions?.amountMin ?? '')} onChange={(e) => updateNode(selectedNodeIndex, { conditions: { ...(selectedNode?.conditions || {}), amountMin: e.target.value === '' ? null : Number(e.target.value) } })} placeholder={t('approvalAmountMin')} />
        <input className="tool-input" value={String(selectedNode?.conditions?.amountMax ?? '')} onChange={(e) => updateNode(selectedNodeIndex, { conditions: { ...(selectedNode?.conditions || {}), amountMax: e.target.value === '' ? null : Number(e.target.value) } })} placeholder={t('approvalAmountMax')} />
        <input className="tool-input" value={selectedNode?.conditions?.role || ''} onChange={(e) => updateNode(selectedNodeIndex, { conditions: { ...(selectedNode?.conditions || {}), role: e.target.value.toUpperCase() } })} placeholder={t('approvalRoleCondition')} />
        <input className="tool-input" value={selectedNode?.conditions?.department || ''} onChange={(e) => updateNode(selectedNodeIndex, { conditions: { ...(selectedNode?.conditions || {}), department: e.target.value } })} placeholder={t('approvalDepartmentCondition')} />
      </div>
      <div className="info-banner">{t('approvalConditionPreview')}: {(selectedNode?.conditions?.amountMin ?? '-')}/{(selectedNode?.conditions?.amountMax ?? '-')} | {selectedNode?.conditions?.role || t('approvalAny')} | {selectedNode?.conditions?.department || t('approvalAny')}</div>
      <div className="inline-tools" style={{ marginTop: 8 }}>
        <input className="tool-input" placeholder={t('approvalPreviewAmount')} value={previewCtx.amount} onChange={(e) => setPreviewCtx((p) => ({ ...p, amount: e.target.value }))} />
        <input className="tool-input" placeholder={t('approvalPreviewRole')} value={previewCtx.role} onChange={(e) => setPreviewCtx((p) => ({ ...p, role: e.target.value.toUpperCase() }))} />
        <input className="tool-input" placeholder={t('approvalPreviewDepartment')} value={previewCtx.department} onChange={(e) => setPreviewCtx((p) => ({ ...p, department: e.target.value }))} />
      </div>
      <div className="info-banner" style={{ marginTop: 8 }}>{t('approvalMatchedNodes')}: {previewMatched.length ? previewMatched.join(', ') : t('noData')}</div>
    </div>
  )
}

export default memo(ApprovalNodeConfigPanel)
