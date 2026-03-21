import { memo, useCallback, useMemo } from 'react'
import VirtualListTable from '../../VirtualListTable'
import { DiffRow, NodeRow, TemplateRow, VersionRow } from './ApprovalTemplateRows'
import ApprovalNodeConfigPanel from './ApprovalNodeConfigPanel'
import ApprovalVersionHistoryPanel from './ApprovalVersionHistoryPanel'

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
              <ApprovalNodeConfigPanel
                t={t}
                editingNodes={editingNodes}
                selectedNodeIndex={selectedNodeIndex}
                updateNode={updateNode}
                previewCtx={previewCtx}
                setPreviewCtx={setPreviewCtx}
                previewMatched={previewMatched}
              />
            </div>
          )}
        </div>
      </div>

      <ApprovalVersionHistoryPanel
        t={t}
        approvals={approvals}
        approvalVersionTemplateId={approvalVersionTemplateId}
        versionRows={versionRows}
        renderVersionRow={renderVersionRow}
        expandedVersion={expandedVersion}
        diffRows={diffRows}
        renderDiffRow={renderDiffRow}
      />
    </>
  )
}

export default memo(ApprovalTemplateSection)
