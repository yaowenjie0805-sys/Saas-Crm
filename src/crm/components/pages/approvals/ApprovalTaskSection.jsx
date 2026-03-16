import { memo } from 'react'
import { translateStatus } from '../../../shared'
import VirtualListTable from '../../VirtualListTable'

const ApprovalTaskRow = memo(function ApprovalTaskRow({ task, t, approvals, formatDateTime }) {
  return (
    <div className="table-row table-row-5 compact">
      <span>{task.id}</span>
      <span>{translateStatus(t, task.status)}</span>
      <span>{task.approverRole}{task.overdue ? ` / ${t('approvalOverdue').toUpperCase()}` : ''}</span>
      <span>
        <button className="mini-btn" onClick={() => approvals.actTask(task.id, 'approve')}>{t('approvalApprove')}</button>
        <button className="mini-btn" onClick={() => approvals.actTask(task.id, 'reject')}>{t('approvalReject')}</button>
        <button className="mini-btn" onClick={() => approvals.actTask(task.id, 'transfer')}>{t('approvalTransfer')}</button>
        <button className="mini-btn" onClick={() => approvals.urgeTask(task.id)}>{t('approvalUrge')}</button>
      </span>
      <span>{formatDateTime(task.createdAt)}</span>
    </div>
  )
})

function ApprovalTaskSection({ t, approvals, refreshApprovals, formatDateTime }) {
  return (
    <>
      {approvals.actionResult && (
        <div className="info-banner" style={{ marginTop: 8 }}>
          {`${t('status')}: ${approvals.actionResult.action || '-'} | ${t('idLabel')}: ${approvals.actionResult.taskId || '-'} | requestId: ${approvals.actionResult.requestId || '-'}`}
          {approvals.actionResult.bizType && ` | ${approvals.actionResult.bizType}:${approvals.actionResult.bizId} -> ${approvals.actionResult.bizStatus || '-'}`}
        </div>
      )}
      <div className="inline-tools filter-row" style={{ marginTop: 10 }}>
        <select className="tool-input" value={approvals.instance.bizType} onChange={(e) => approvals.setInstance((p) => ({ ...p, bizType: e.target.value }))}>
          <option value="CONTRACT">{t('bizTypeContract')}</option>
          <option value="PAYMENT">{t('bizTypePayment')}</option>
          <option value="QUOTE">{t('quotes')}</option>
        </select>
        <input className="tool-input" placeholder={t('bizId')} value={approvals.instance.bizId} onChange={(e) => approvals.setInstance((p) => ({ ...p, bizId: e.target.value }))} />
        <input className="tool-input" placeholder={t('amount')} value={approvals.instance.amount} onChange={(e) => approvals.setInstance((p) => ({ ...p, amount: e.target.value }))} />
        <button className="mini-btn" onClick={approvals.submitInstance}>{t('submit')}</button>
      </div>

      <div className="inline-tools filter-row" style={{ marginTop: 10 }}>
        <select className="tool-input" value={approvals.taskStatus} onChange={(e) => approvals.setTaskStatus(e.target.value)}>
          <option value="PENDING">{translateStatus(t, 'PENDING')}</option>
          <option value="WAITING">{translateStatus(t, 'WAITING')}</option>
          <option value="APPROVED">{translateStatus(t, 'APPROVED')}</option>
          <option value="REJECTED">{translateStatus(t, 'REJECTED')}</option>
          <option value="ESCALATED">{translateStatus(t, 'ESCALATED')}</option>
          <option value="">{t('filterAll')}</option>
        </select>
        <label className="switch-inline"><input type="checkbox" checked={!!approvals.overdueOnly} onChange={(e) => approvals.setOverdueOnly(e.target.checked)} />{t('approvalOverdue')}</label>
        <label className="switch-inline"><input type="checkbox" checked={!!approvals.escalatedOnly} onChange={(e) => approvals.setEscalatedOnly(e.target.checked)} />{t('approvalEscalated')}</label>
        <input className="tool-input" placeholder={t('remark')} value={approvals.actionComment} onChange={(e) => approvals.setActionComment(e.target.value)} />
        <input className="tool-input" placeholder={t('transferTo')} value={approvals.transferTo} onChange={(e) => approvals.setTransferTo(e.target.value)} />
      </div>
      <div className="inline-tools filter-bar" style={{ marginTop: 8 }}>
        <button className="mini-btn" onClick={() => approvals.setActionResult?.(null)}>{t('clearFilters')}</button>
        <button className="mini-btn" onClick={refreshApprovals}>{t('refresh')}</button>
      </div>

      <div className="table-row table-head-row table-row-5 compact" style={{ marginTop: 10 }}>
        <span>{t('idLabel')}</span><span>{t('status')}</span><span>{t('role')}</span><span>{t('action')}</span><span>{t('createdAt')}</span>
      </div>
      <VirtualListTable
        rows={approvals.tasks || []}
        rowHeight={52}
        viewportHeight={Math.min(420, Math.max(160, (approvals.tasks || []).length * 52))}
        getRowKey={(task) => task.id}
        renderRow={(task) => (
          <ApprovalTaskRow
            key={task.id}
            task={task}
            t={t}
            approvals={approvals}
            formatDateTime={formatDateTime}
          />
        )}
      />
    </>
  )
}

export default memo(ApprovalTaskSection)
