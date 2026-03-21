import { memo } from 'react'
import { translateStatus } from '../../../shared'

export const TemplateRow = memo(function TemplateRow({
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

export const VersionRow = memo(function VersionRow({
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

export const DiffRow = memo(function DiffRow({ row, summarizeNodeChange }) {
  return (
    <div className="table-row table-row-3 compact">
      <span>{row.nodeKey || '-'}</span>
      <span>{row.type || '-'}</span>
      <span>{summarizeNodeChange(row)}</span>
    </div>
  )
})

export const NodeRow = memo(function NodeRow({
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
