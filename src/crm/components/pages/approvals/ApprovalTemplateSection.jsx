import { memo, useCallback, useMemo } from 'react'
import { translateStatus } from '../../../shared'
import VirtualListTable from '../../VirtualListTable'

const TemplateRow = memo(function TemplateRow({
  row,
  t,
  active,
  setEditingTemplateId,
  setSelectedNodeIndex,
  approvals,
}) {
  return (
    <div className={`table-row table-row-5 compact ${active ? 'row-active' : ''}`}>
      <span>{row.id}</span>
      <span>{row.bizType}</span>
      <span>
        <input
          className="tool-input"
          value={row.name || ''}
          onChange={(e) => approvals.setTemplates((prev) => prev.map((x) => (x.id === row.id ? { ...x, name: e.target.value } : x)))}
        />
      </span>
      <span>
        <div className="inline-tools">
          <label className="switch-inline">
            <input
              type="checkbox"
              checked={!!row.enabled}
              onChange={(e) => approvals.setTemplates((prev) => prev.map((x) => (x.id === row.id ? { ...x, enabled: e.target.checked } : x)))}
            />
            {t('enabled')}
          </label>
          <span>{translateStatus(t, row.status || '') || row.status || '-'}</span>
          <span>v{row.activeVersion || row.version || '-'}</span>
        </div>
      </span>
      <span>
        <div className="inline-tools">
          <button className="mini-btn" onClick={() => { setEditingTemplateId(row.id); setSelectedNodeIndex(0) }}>{t('approvalEditFlow')}</button>
          <button
            className="mini-btn"
            onClick={() => {
              approvals.setTemplates((prev) => prev.map((x) => (x.id === row.id ? { ...x, status: 'DRAFT', enabled: false } : x)))
              approvals.updateTemplate({ ...row, status: 'DRAFT', enabled: false })
            }}
          >
            {t('approvalDraft')}
          </button>
          <button className="mini-btn" onClick={() => approvals.publishTemplate(row.id)}>{t('approvalPublish')}</button>
          <button className="mini-btn" onClick={() => approvals.loadVersions(row.id)}>{t('approvalVersionHistory')}</button>
          <button className="mini-btn" onClick={() => approvals.updateTemplate(row)}>{t('save')}</button>
        </div>
      </span>
    </div>
  )
})

const VersionRow = memo(function VersionRow({
  row,
  t,
  isExpanded,
  setExpandedVersionId,
  summarizeVersionDiff,
  confirmRollback,
  editingTemplate,
  formatDateTime,
}) {
  const rowId = row.id || `${row.templateId}-${row.version}`
  return (
    <div className={`table-row table-row-6 compact ${isExpanded ? 'row-active' : ''}`}>
      <span>v{row.version}</span>
      <span>{translateStatus(t, row.status || '') || row.status || '-'}</span>
      <span>{formatDateTime(row.publishedAt)}</span>
      <span>{row.publishedBy || '-'}</span>
      <span>{summarizeVersionDiff(editingTemplate, row)}</span>
      <span>
        <div className="inline-tools">
          <button className="mini-btn" onClick={() => setExpandedVersionId(isExpanded ? '' : rowId)}>{t('detail')}</button>
          <button className="mini-btn" onClick={() => confirmRollback(row)}>{t('rollbackRole')}</button>
        </div>
      </span>
    </div>
  )
})

const DiffRow = memo(function DiffRow({ row, summarizeNodeChange }) {
  return (
    <div className="table-row table-row-3 compact">
      <span>{row.nodeKey || '-'}</span>
      <span>{row.type || '-'}</span>
      <span>{summarizeNodeChange(row)}</span>
    </div>
  )
})

const NodeRow = memo(function NodeRow({
  node,
  idx,
  t,
  selectedNodeIndex,
  setSelectedNodeIndex,
  moveNode,
  nodeTotal,
}) {
  return (
    <div className={`approval-node-item ${selectedNodeIndex === idx ? 'active' : ''}`}>
      <button className="mini-btn" onClick={() => moveNode(idx, idx - 1)} disabled={idx === 0}>{t('moveUp')}</button>
      <button className="mini-btn" onClick={() => moveNode(idx, idx + 1)} disabled={idx === nodeTotal - 1}>{t('moveDown')}</button>
      <button className="mini-btn" onClick={() => setSelectedNodeIndex(idx)}>#{node.seq || idx + 1}</button>
      <span>{(node.approverRoles || []).join(',') || t('na')}</span>
    </div>
  )
})

function ApprovalTemplateSection({
  t,
  approvals,
  refreshApprovals,
  editingTemplate,
  editingTemplateId,
  setEditingTemplateId,
  editingNodes,
  selectedNodeIndex,
  setSelectedNodeIndex,
  addNode,
  moveNode,
  updateNode,
  previewCtx,
  setPreviewCtx,
  previewMatched,
  approvalVersionTemplateId,
  expandedVersionId,
  setExpandedVersionId,
  summarizeVersionDiff,
  summarizeNodeChange,
  confirmRollback,
  formatDateTime,
}) {
  const templateRows = useMemo(() => approvals.templates || [], [approvals.templates])
  const versionRows = useMemo(() => approvals.versions || [], [approvals.versions])
  const expandedVersion = useMemo(() => {
    const key = String(expandedVersionId || '')
    if (!key) return null
    return versionRows.find((v) => (v.id || `${v.templateId}-${v.version}`) === key) || null
  }, [expandedVersionId, versionRows])
  const diffRows = expandedVersion?.diffSummary?.changes || []
  const nodeRows = useMemo(
    () => editingNodes.map((node, idx) => ({ ...node, __rowIdx: idx })),
    [editingNodes],
  )
  const renderTemplateRow = useCallback((row) => (
    <TemplateRow
      key={row.id}
      row={row}
      t={t}
      active={editingTemplateId === row.id}
      setEditingTemplateId={setEditingTemplateId}
      setSelectedNodeIndex={setSelectedNodeIndex}
      approvals={approvals}
    />
  ), [t, editingTemplateId, setEditingTemplateId, setSelectedNodeIndex, approvals])
  const renderVersionRow = useCallback((v) => {
    const rowId = v.id || `${v.templateId}-${v.version}`
    return (
      <VersionRow
        key={rowId}
        row={v}
        t={t}
        isExpanded={expandedVersionId === rowId}
        setExpandedVersionId={setExpandedVersionId}
        summarizeVersionDiff={summarizeVersionDiff}
        confirmRollback={confirmRollback}
        editingTemplate={editingTemplate}
        formatDateTime={formatDateTime}
      />
    )
  }, [t, expandedVersionId, setExpandedVersionId, summarizeVersionDiff, confirmRollback, editingTemplate, formatDateTime])
  const renderDiffRow = useCallback((row) => <DiffRow row={row} summarizeNodeChange={summarizeNodeChange} />, [summarizeNodeChange])
  const renderNodeRow = useCallback((row) => (
    <NodeRow
      key={`${row.id || 'node'}-${row.__rowIdx}`}
      node={row}
      idx={row.__rowIdx}
      t={t}
      selectedNodeIndex={selectedNodeIndex}
      setSelectedNodeIndex={setSelectedNodeIndex}
      moveNode={moveNode}
      nodeTotal={nodeRows.length}
    />
  ), [t, selectedNodeIndex, setSelectedNodeIndex, moveNode, nodeRows.length])

  return (
    <>
      <div className="inline-tools filter-row">
        <select className="tool-input" value={approvals.template.bizType} onChange={(e) => approvals.setTemplate((p) => ({ ...p, bizType: e.target.value }))}>
          <option value="CONTRACT">{t('bizTypeContract')}</option>
          <option value="PAYMENT">{t('bizTypePayment')}</option>
          <option value="QUOTE">{t('quotes')}</option>
        </select>
        <input className="tool-input" placeholder={t('title')} value={approvals.template.name} onChange={(e) => approvals.setTemplate((p) => ({ ...p, name: e.target.value }))} />
        <input className="tool-input" placeholder={t('approverRoles')} value={approvals.template.approverRoles} onChange={(e) => approvals.setTemplate((p) => ({ ...p, approverRoles: e.target.value }))} />
        <button className="mini-btn" onClick={approvals.createTemplate}>{t('createTemplate')}</button>
        <button className="mini-btn" onClick={refreshApprovals}>{t('refresh')}</button>
      </div>

      <div className="approval-editor-grid">
        <div className="panel" style={{ marginTop: 10, boxShadow: 'none' }}>
          <div className="panel-head"><h2>{t('approvalTemplateMatrix')}</h2></div>
          <div className="table-row table-head-row table-row-5 compact">
            <span>{t('idLabel')}</span><span>{t('bizType')}</span><span>{t('title')}</span><span>{t('status')}</span><span>{t('action')}</span>
          </div>
          <VirtualListTable
            rows={templateRows}
            rowHeight={62}
            viewportHeight={Math.min(420, Math.max(160, templateRows.length * 62))}
            getRowKey={(row) => row.id}
            renderRow={renderTemplateRow}
          />
          {templateRows.length === 0 && <div className="empty-tip">{t('noData')}</div>}
        </div>

        <div className="panel" style={{ marginTop: 10, boxShadow: 'none' }}>
          <div className="panel-head"><h2>{t('approvalFlowNodes')}</h2><button className="mini-btn" disabled={!editingTemplate} onClick={addNode}>{t('approvalAddNode')}</button></div>
          {!editingTemplate && <div className="empty-tip">{t('approvalSelectTemplate')}</div>}
          {!!editingTemplate && (
            <div>
              <VirtualListTable
                rows={nodeRows}
                rowHeight={48}
                viewportHeight={Math.min(260, Math.max(120, nodeRows.length * 48))}
                getRowKey={(row) => `${row.id || 'node'}-${row.__rowIdx}`}
                renderRow={renderNodeRow}
              />
              {nodeRows.length === 0 && <div className="empty-tip">{t('noData')}</div>}
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
                  <div className="info-banner">{t('approvalConditionPreview')}: {(editingNodes[selectedNodeIndex]?.conditions?.amountMin ?? '-')}/{(editingNodes[selectedNodeIndex]?.conditions?.amountMax ?? '-')} | {editingNodes[selectedNodeIndex]?.conditions?.role || t('approvalAny')} | {editingNodes[selectedNodeIndex]?.conditions?.department || t('approvalAny')}</div>
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

      {approvalVersionTemplateId && (
        <div className="panel" style={{ marginTop: 12, boxShadow: 'none' }}>
          <div className="panel-head">
            <h2>{t('approvalVersionHistory')}</h2>
            <button className="mini-btn" onClick={() => approvals.loadVersions(approvalVersionTemplateId)}>{t('refresh')}</button>
          </div>
          <div className="table-row table-head-row table-row-6 compact">
            <span>{t('version')}</span><span>{t('status')}</span><span>{t('createdAt')}</span><span>{t('user')}</span><span>{t('summary')}</span><span>{t('action')}</span>
          </div>
          <VirtualListTable
            rows={versionRows}
            rowHeight={56}
            viewportHeight={Math.min(360, Math.max(140, versionRows.length * 56))}
            getRowKey={(v) => v.id || `${v.templateId}-${v.version}`}
            renderRow={renderVersionRow}
          />
          {versionRows.length === 0 && <div className="empty-tip">{t('noData')}</div>}
          {!!expandedVersion && (
            <div className="panel" style={{ marginTop: 6 }}>
              <div className="panel-head"><h2>{t('approvalDiffDetail')}</h2></div>
              <div className="table-row table-head-row table-row-3 compact">
                <span>{t('idLabel')}</span><span>{t('type')}</span><span>{t('summary')}</span>
              </div>
              <VirtualListTable
                rows={diffRows}
                rowHeight={48}
                viewportHeight={Math.min(280, Math.max(120, diffRows.length * 48))}
                getRowKey={(row, idx) => `${expandedVersion.id || expandedVersion.version}-${row.nodeKey || 'node'}-${row.type || 'type'}-${idx}`}
                renderRow={renderDiffRow}
              />
              {!diffRows.length && <div className="empty-tip">{t('noData')}</div>}
            </div>
          )}
        </div>
      )}
    </>
  )
}

export default memo(ApprovalTemplateSection)
