import { memo } from 'react'
import VirtualListTable from '../../VirtualListTable'

function ApprovalVersionHistoryPanel({
  t,
  approvals,
  approvalVersionTemplateId,
  versionRows,
  renderVersionRow,
  expandedVersion,
  diffRows,
  renderDiffRow,
}) {
  if (!approvalVersionTemplateId) return null

  return (
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
  )
}

export default memo(ApprovalVersionHistoryPanel)
