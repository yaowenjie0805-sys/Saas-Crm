import { buildBatchFailureText } from './useBatchActions'

function BatchResultModal({ t, open, summary, onClose }) {
  if (!open || !summary) return null
  const canCopy = Array.isArray(summary.failures) && summary.failures.length > 0

  const copyFailures = async () => {
    if (!canCopy || !navigator?.clipboard?.writeText) return
    const text = buildBatchFailureText(summary)
    if (!text) return
    await navigator.clipboard.writeText(text)
  }

  return (
    <div className="modal-mask">
      <div className="modal-card modal-card-wide">
        <div className="panel-head">
          <h2>{t('batchResultTitle')}</h2>
          <button className="mini-btn" onClick={onClose}>{t('closeModal')}</button>
        </div>
        <div className="batch-summary-strip">
          <span>{t('batchRequested')}: {summary.requested || 0}</span>
          <span>{t('success')}: {summary.success || 0}</span>
          <span>{t('failed')}: {summary.failed || 0}</span>
        </div>
        {canCopy && (
          <div className="inline-tools" style={{ marginBottom: 8 }}>
            <button className="mini-btn" onClick={copyFailures}>{t('copyFailedSummary')}</button>
          </div>
        )}
        {!canCopy ? (
          <div className="empty-tip">{t('batchNoFailedItems')}</div>
        ) : (
          <div className="batch-fail-table-wrap">
            <div className="table-row table-head-row table-row-3">
              <span>{t('idLabel')}</span>
              <span>{t('errorReason')}</span>
              <span>{t('requestIdLabel')}</span>
            </div>
            {summary.failures.map((row, idx) => (
              <div key={`${row.id}-${idx}`} className="table-row table-row-3 compact">
                <span>{row.id}</span>
                <span>{row.message}</span>
                <span>{row.requestId || '-'}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default BatchResultModal
