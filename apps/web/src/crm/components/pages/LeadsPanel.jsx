import { memo, useEffect, useMemo, useState } from 'react'
import { translateOwnerAlias, translateStatus } from '../../shared'
import ListState from '../ListState'
import ServerPager from '../ServerPager'
import VirtualListTable from '../VirtualListTable'
import { useSelectionSet } from '../../hooks/useSelectionSet'

const LEAD_FORM_STATUS_VALUES = ['NEW', 'QUALIFIED', 'NURTURING', 'DISQUALIFIED']
const LEAD_FILTER_STATUS_VALUES = [...LEAD_FORM_STATUS_VALUES, 'CONVERTED']
const PAGE_SIZE_VALUES = [8, 10, 20, 50]
const IMPORT_STATUS_VALUES = ['ALL', 'PENDING', 'RUNNING', 'PARTIAL_SUCCESS', 'SUCCESS', 'FAILED', 'CANCELED']
const IMPORT_EXPORT_STATUS_VALUES = ['ALL', 'PENDING', 'RUNNING', 'DONE', 'FAILED']
const IMPORT_PAGE_SIZE_VALUES = [10, 20, 50]

const safeSnippet = (value, max = 120) => {
  const text = String(value || '').trim()
  if (!text) return '-'
  return text.length > max ? `${text.slice(0, max)}...` : text
}

const LeadRow = memo(function LeadRow({ row, checked, onToggle, canWrite, t, editLead, convertLead }) {
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

const LeadImportJobRow = memo(function LeadImportJobRow({ row, t, selectImportJob, cancelImportJob, retryImportJob, isImportActionPending }) {
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

const LeadImportExportJobRow = memo(function LeadImportExportJobRow({ row, importJobId, t, downloadImportFailedRowsExportJob, isImportActionPending }) {
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

function LeadsPanel({
  activePage,
  t,
  canWrite,
  loading,
  leadForm,
  setLeadForm,
  saveLead,
  convertLead,
  formError,
  fieldErrors,
  leads,
  editLead,
  leadQ,
  setLeadQ,
  leadStatus,
  setLeadStatus,
  pagination,
  onPageChange,
  onSizeChange,
  onRefresh,
  bulkAssignByRule,
  bulkUpdateStatus,
  importCsv,
  importJob,
  importJobs,
  importStatus,
  setImportStatus,
  importPaging,
  onImportPageChange,
  onImportSizeChange,
  selectImportJob,
  cancelImportJob,
  retryImportJob,
  importFailedRows,
  downloadTemplate,
  importMetrics,
  canCreateImportExport,
  importExportJobs,
  importExportStatus,
  setImportExportStatus,
  importExportPaging,
  onImportExportPageChange,
  onImportExportSizeChange,
  createImportFailedRowsExportJob,
  downloadImportFailedRowsExportJob,
  isImportActionPending,
}) {
  const [bulkStatus, setBulkStatus] = useState('QUALIFIED')
  const [file, setFile] = useState(null)
  const [leadQDraft, setLeadQDraft] = useState(leadQ || '')
  const [leadStatusDraft, setLeadStatusDraft] = useState(leadStatus || '')

  const rows = useMemo(() => (leads || []).map((row) => ({
    ...row,
    statusLabel: translateStatus(t, row.status),
  })), [leads, t])
  const importJobsView = useMemo(() => (importJobs || []).map((row) => ({
    ...row,
    statusLabel: translateStatus(t, row.status),
  })), [importJobs, t])
  const importExportJobsView = useMemo(() => (importExportJobs || []).map((row) => ({
    ...row,
    statusLabel: translateStatus(t, row.status),
  })), [importExportJobs, t])
  const importFailedRowsView = useMemo(() => (importFailedRows || []).map((row, idx) => ({
    ...row,
    key: `${row.lineNo}-${idx}`,
    rawLineSnippet: safeSnippet(row.rawLine),
    errorSnippet: safeSnippet(row.errorMessage || row.errorCode, 80),
  })), [importFailedRows])
  const selection = useSelectionSet(rows, (row) => row.id)
  const { selectedIds, selectedCount, allChecked, clearSelection, toggleAll, toggleOne } = selection
  const selectedIdList = useMemo(() => [...selectedIds], [selectedIds])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLeadQDraft(leadQ || '')
  }, [leadQ])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLeadStatusDraft(leadStatus || '')
  }, [leadStatus])

  const applyLeadFilters = () => {
    if (leadQ !== leadQDraft) setLeadQ(leadQDraft)
    if (leadStatus !== leadStatusDraft) setLeadStatus(leadStatusDraft)
    onPageChange(1)
  }

  const resetLeadFilters = () => {
    setLeadQDraft('')
    setLeadStatusDraft('')
    if (leadQ !== '') setLeadQ('')
    if (leadStatus !== '') setLeadStatus('')
    onPageChange(1)
  }
  const refreshSelf = onRefresh

  if (activePage !== 'leads') return null

  return (
    <section className="panel">
      <div className="panel-head">
        <h2>{t('leads')}</h2>
        <div className="inline-tools">
          <button className="mini-btn" onClick={refreshSelf}>{t('refresh')}</button>
          <button className="mini-btn" onClick={downloadTemplate}>{t('leadDownloadTemplate')}</button>
        </div>
      </div>

      <div className="inline-tools filter-row">
        <input className="tool-input" placeholder={t('keyword')} value={leadQDraft} onChange={(e) => setLeadQDraft(e.target.value)} />
        <select className="tool-input" value={leadStatusDraft} onChange={(e) => setLeadStatusDraft(e.target.value)}>
          <option value="">{t('allStatuses')}</option>
          {LEAD_FILTER_STATUS_VALUES.map((status) => (
            <option key={status} value={status}>{translateStatus(t, status)}</option>
          ))}
        </select>
        <select className="tool-input" value={pagination.size} onChange={(e) => onSizeChange(Number(e.target.value || 10))}>
          {PAGE_SIZE_VALUES.map((n) => <option key={n} value={n}>{n}</option>)}
        </select>
        <button className="mini-btn" onClick={applyLeadFilters}>{t('search')}</button>
        <button className="mini-btn" onClick={resetLeadFilters}>{t('reset')}</button>
      </div>

      {canWrite && (
        <div className="inline-tools filter-row" style={{ marginTop: 12 }}>
          <input className={`tool-input ${fieldErrors?.name ? 'input-invalid' : ''}`} placeholder={t('leadName')} value={leadForm.name} onChange={(e) => setLeadForm((p) => ({ ...p, name: e.target.value }))} />
          <input className="tool-input" placeholder={t('companyName')} value={leadForm.company} onChange={(e) => setLeadForm((p) => ({ ...p, company: e.target.value }))} />
          <input className="tool-input" placeholder={t('phone')} value={leadForm.phone} onChange={(e) => setLeadForm((p) => ({ ...p, phone: e.target.value }))} />
          <input className="tool-input" placeholder={t('email')} value={leadForm.email} onChange={(e) => setLeadForm((p) => ({ ...p, email: e.target.value }))} />
          <input className="tool-input" placeholder={t('owner')} value={leadForm.owner} onChange={(e) => setLeadForm((p) => ({ ...p, owner: e.target.value }))} />
          <input className="tool-input" placeholder={t('leadSource')} value={leadForm.source} onChange={(e) => setLeadForm((p) => ({ ...p, source: e.target.value }))} />
          <select className="tool-input" value={leadForm.status} onChange={(e) => setLeadForm((p) => ({ ...p, status: e.target.value }))}>
            <option value="NEW">{t('leadStatusNew')}</option>
            <option value="QUALIFIED">{t('leadStatusQualified')}</option>
            <option value="NURTURING">{t('leadStatusNurturing')}</option>
            <option value="DISQUALIFIED">{t('leadStatusDisqualified')}</option>
          </select>
          <button className="mini-btn" onClick={saveLead} disabled={loading}>{leadForm.id ? t('save') : t('create')}</button>
        </div>
      )}

      {canWrite && (
        <div className="inline-tools filter-row" style={{ marginTop: 12 }}>
          <input type="file" accept=".csv,text/csv" className="tool-input" onChange={(e) => setFile(e.target.files?.[0] || null)} />
          <button className="mini-btn" disabled={!file || isImportActionPending?.('upload')} onClick={() => importCsv(file)}>{t('leadImportCsv')}</button>
          <button className="mini-btn" disabled={selectedCount === 0} onClick={async () => { await bulkAssignByRule(selectedIdList); clearSelection() }}>{t('leadBulkAssign')}</button>
          <select className="tool-input" value={bulkStatus} onChange={(e) => setBulkStatus(e.target.value)}>
            {LEAD_FORM_STATUS_VALUES.map((status) => (
              <option key={status} value={status}>{translateStatus(t, status)}</option>
            ))}
          </select>
          <button className="mini-btn" disabled={selectedCount === 0} onClick={async () => { await bulkUpdateStatus(bulkStatus, selectedIdList); clearSelection() }}>{t('leadBulkStatus')}</button>
        </div>
      )}

      {importJob && (
        <div className="info-banner" style={{ marginTop: 10 }}>
          {t('status')}: {translateStatus(t, importJob.status)} | {t('count')}: {importJob.processedRows || 0}/{importJob.totalRows || 0} | {t('success')}: {importJob.successCount || 0} | {t('failed')}: {importJob.failCount || 0} | {t('progress')}: {importJob.percent || 0}%
        </div>
      )}
      {!!importMetrics && (
        <section className="stats-grid" style={{ marginTop: 12 }}>
          <article className="stat-card"><p>{t('leadImportMetricTotal')}</p><h3>{importMetrics.importJobTotal || 0}</h3><span>{t('last24Hours')}</span></article>
          <article className="stat-card"><p>{t('leadImportMetricRunning')}</p><h3>{importMetrics.importRunning || 0}</h3><span>{translateStatus(t, 'RUNNING')}</span></article>
          <article className="stat-card"><p>{t('leadImportMetricSuccessRate')}</p><h3>{Math.round((Number(importMetrics.importSuccessRate || 0) * 100))}%</h3><span>{t('success')}</span></article>
          <article className="stat-card"><p>{t('leadImportMetricFailureRate')}</p><h3>{Math.round((Number(importMetrics.importFailureRate || 0) * 100))}%</h3><span>{t('failed')}</span></article>
          <article className="stat-card"><p>{t('leadImportMetricAvgDuration')}</p><h3>{Math.round(Number(importMetrics.importAvgDurationMs || 0) / 1000)}s</h3><span>{t('duration')}</span></article>
        </section>
      )}

      <div className="panel" style={{ marginTop: 12, boxShadow: 'none' }}>
        <div className="panel-head"><h2>{t('leadImportJobs')}</h2></div>
        <div className="inline-tools filter-row">
          <select className="tool-input" value={importStatus} onChange={(e) => setImportStatus(e.target.value)}>
            {IMPORT_STATUS_VALUES.map((s) => <option key={s} value={s}>{s === 'ALL' ? t('filterAll') : translateStatus(t, s)}</option>)}
          </select>
          <select className="tool-input" value={importPaging.size} onChange={(e) => onImportSizeChange(Number(e.target.value || 10))}>
            {IMPORT_PAGE_SIZE_VALUES.map((n) => <option key={n} value={n}>{n}</option>)}
          </select>
        </div>
        <div className="table-row table-head-row" style={{ marginTop: 8 }}>
          <span>{t('idLabel')}</span><span>{t('status')}</span><span>{t('progress')}</span><span>{t('failed')}</span><span>{t('action')}</span>
        </div>
        <ListState loading={loading} empty={!loading && importJobsView.length === 0} emptyText={t('noData')} />
        {!loading && importJobsView.length > 0 && (
          <VirtualListTable
            rows={importJobsView}
            viewportHeight={240}
            rowHeight={42}
            getRowKey={(row) => row.id}
            renderRow={(row) => (
              <LeadImportJobRow
                key={row.id}
                row={row}
                t={t}
                selectImportJob={selectImportJob}
                cancelImportJob={cancelImportJob}
                retryImportJob={retryImportJob}
                isImportActionPending={isImportActionPending}
              />
            )}
          />
        )}
        {!loading && importJobsView.length > 0 && (
          <ServerPager
            t={t}
            page={importPaging.page}
            totalPages={importPaging.totalPages}
            size={importPaging.size}
            onPageChange={onImportPageChange}
            onSizeChange={onImportSizeChange}
          />
        )}
        {!!importFailedRowsView.length && (
          <div style={{ marginTop: 8 }}>
            <div className="table-row table-head-row">
              <span>{t('lineNo')}</span><span>{t('summary')}</span><span>{t('errorReason')}</span>
            </div>
            {importFailedRowsView.map((row) => (
              <div key={row.key} className="table-row">
                <span>{row.lineNo}</span>
                <span title={row.rawLine || ''}>{row.rawLineSnippet}</span>
                <span title={row.errorMessage || row.errorCode || ''}>{row.errorSnippet}</span>
              </div>
            ))}
          </div>
        )}

        <div className="panel" style={{ marginTop: 12, boxShadow: 'none' }}>
          <div className="panel-head">
            <h2>{t('leadImportFailedRowsExport')}</h2>
          </div>
          <div className="inline-tools filter-row">
            <select className="tool-input" value={importExportStatus} onChange={(e) => setImportExportStatus(e.target.value)}>
              {IMPORT_EXPORT_STATUS_VALUES.map((s) => <option key={s} value={s}>{s === 'ALL' ? t('filterAll') : translateStatus(t, s)}</option>)}
            </select>
            <select className="tool-input" value={importExportPaging.size} onChange={(e) => onImportExportSizeChange(Number(e.target.value || 10))}>
              {IMPORT_PAGE_SIZE_VALUES.map((n) => <option key={n} value={n}>{n}</option>)}
            </select>
            <button className="mini-btn" disabled={!canCreateImportExport || !importJob?.id || isImportActionPending?.('export-create', importJob?.id)} onClick={() => createImportFailedRowsExportJob(importJob?.id)}>{t('leadImportExportCreate')}</button>
          </div>
          <div className="table-row table-head-row" style={{ marginTop: 8 }}>
            <span>{t('idLabel')}</span><span>{t('status')}</span><span>{t('progress')}</span><span>{t('count')}</span><span>{t('action')}</span>
          </div>
          <ListState loading={loading} empty={!loading && importExportJobsView.length === 0} emptyText={t('noData')} />
          {!loading && importExportJobsView.length > 0 && (
            <VirtualListTable
              rows={importExportJobsView}
              viewportHeight={220}
              rowHeight={42}
              getRowKey={(row) => row.jobId}
              renderRow={(row) => (
                <LeadImportExportJobRow
                  key={row.jobId}
                  row={row}
                  importJobId={importJob?.id}
                  t={t}
                  downloadImportFailedRowsExportJob={downloadImportFailedRowsExportJob}
                  isImportActionPending={isImportActionPending}
                />
              )}
            />
          )}
          {!loading && importExportJobsView.length > 0 && (
            <ServerPager
              t={t}
              page={importExportPaging.page}
              totalPages={importExportPaging.totalPages}
              size={importExportPaging.size}
              onPageChange={onImportExportPageChange}
              onSizeChange={onImportExportSizeChange}
            />
          )}
        </div>
      </div>

      {!!formError && <div className="error-banner" style={{ marginTop: 10 }}>{formError}</div>}

      <div className="table-row table-head-row table-row-6" style={{ marginTop: 12 }}>
        <span><input type="checkbox" checked={allChecked} onChange={(e) => toggleAll(e.target.checked)} /></span>
        <span>{t('leadName')}</span>
        <span>{t('companyName')}</span>
        <span>{t('status')}</span>
        <span>{t('owner')}</span>
        <span>{t('action')}</span>
      </div>
      <VirtualListTable
        rows={rows}
        viewportHeight={460}
        getRowKey={(row) => row.id}
        renderRow={(row) => (
          <LeadRow
            key={row.id}
            row={row}
            checked={selectedIds.has(row.id)}
            onToggle={(e) => toggleOne(row.id, e.target.checked)}
            canWrite={canWrite}
            t={t}
            editLead={editLead}
            convertLead={convertLead}
          />
        )}
      />
      {(leads || []).length === 0 && <div className="empty-tip">{t('noData')}</div>}

      {(rows || []).length > 0 && (
        <ServerPager
          t={t}
          page={pagination.page}
          totalPages={pagination.totalPages}
          size={pagination.size}
          onPageChange={onPageChange}
          onSizeChange={onSizeChange}
        />
      )}
    </section>
  )
}

export default memo(LeadsPanel)
