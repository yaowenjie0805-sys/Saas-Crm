import { memo } from 'react'
import { translateOwnerAlias } from '../../../shared'

export const LeadRow = memo(function LeadRow({ row, checked, onToggle, canWrite, t, editLead, convertLead }) {
  return (
    <div className="table-row table-row-6">
      <span><input type="checkbox" checked={checked} onChange={onToggle} /></span>
      <span>{row.name}</span>
      <span>{row.company || '-'}</span>
      <span>{row.statusLabel || '-'}</span>
      <span>{translateOwnerAlias(t, row.owner || '-')}</span>
      <span>
        <div className="inline-tools">
          <button className="mini-btn" onClick={() => editLead(row)}>{t('detail')}</button>
          {canWrite && (
            <button
              className="mini-btn"
              disabled={String(row.status || '').toUpperCase() === 'CONVERTED'}
              onClick={() => convertLead(row.id)}
            >
              {t('leadConvert')}
            </button>
          )}
        </div>
      </span>
    </div>
  )
})

export const LeadImportJobRow = memo(function LeadImportJobRow({
  row,
  t,
  selectImportJob,
  cancelImportJob,
  retryImportJob,
  isImportActionPending,
}) {
  const cancelPending = Boolean(isImportActionPending?.('cancel', row.id))
  const retryPending = Boolean(isImportActionPending?.('retry', row.id))
  return (
    <div className="table-row">
      <span>{row.id}</span>
      <span>{row.statusLabel || '-'}</span>
      <span>{row.percent || 0}% ({row.processedRows || 0}/{row.totalRows || 0})</span>
      <span>{row.failCount || 0}</span>
      <span>
        <div className="inline-tools">
          <button className="mini-btn" onClick={() => selectImportJob(row)}>{t('detail')}</button>
          <button className="mini-btn" disabled={cancelPending || !['PENDING', 'RUNNING'].includes(String(row.status || '').toUpperCase())} onClick={() => cancelImportJob(row.id)}>{t('close')}</button>
          <button className="mini-btn" disabled={retryPending || !['FAILED', 'CANCELED'].includes(String(row.status || '').toUpperCase())} onClick={() => retryImportJob(row.id)}>{t('retry')}</button>
        </div>
      </span>
    </div>
  )
})

export const LeadImportExportJobRow = memo(function LeadImportExportJobRow({
  row,
  importJobId,
  t,
  downloadImportFailedRowsExportJob,
  isImportActionPending,
}) {
  const downloadPending = Boolean(isImportActionPending?.('export-download', row.jobId))
  return (
    <div className="table-row">
      <span>{row.jobId}</span>
      <span>{row.statusLabel || '-'}</span>
      <span>{row.progress || 0}%</span>
      <span>{row.rowCount || 0}</span>
      <span>
        <button className="mini-btn" disabled={downloadPending || !row.downloadReady} onClick={() => downloadImportFailedRowsExportJob(importJobId, row.jobId)}>{t('exportDownload')}</button>
      </span>
    </div>
  )
})
