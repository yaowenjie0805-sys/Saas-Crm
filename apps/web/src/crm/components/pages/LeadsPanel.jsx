import { memo, useEffect, useMemo, useState } from 'react'
import { translateStatus } from '../../shared'
import { useSelectionSet } from '../../hooks/useSelectionSet'
import LeadImportSection from './leads/LeadImportSection'
import LeadListSection from './leads/LeadListSection'
import {
  LEAD_FILTER_STATUS_VALUES,
  LEAD_FORM_STATUS_VALUES,
  PAGE_SIZE_VALUES,
  safeSnippet,
} from './leads/leadPanelConstants'

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

      <LeadImportSection
        t={t}
        loading={loading}
        importJob={importJob}
        importMetrics={importMetrics}
        importJobsView={importJobsView}
        importStatus={importStatus}
        setImportStatus={setImportStatus}
        importPaging={importPaging}
        onImportPageChange={onImportPageChange}
        onImportSizeChange={onImportSizeChange}
        selectImportJob={selectImportJob}
        cancelImportJob={cancelImportJob}
        retryImportJob={retryImportJob}
        importFailedRowsView={importFailedRowsView}
        importExportStatus={importExportStatus}
        setImportExportStatus={setImportExportStatus}
        importExportPaging={importExportPaging}
        onImportExportPageChange={onImportExportPageChange}
        onImportExportSizeChange={onImportExportSizeChange}
        canCreateImportExport={canCreateImportExport}
        createImportFailedRowsExportJob={createImportFailedRowsExportJob}
        importExportJobsView={importExportJobsView}
        downloadImportFailedRowsExportJob={downloadImportFailedRowsExportJob}
        isImportActionPending={isImportActionPending}
      />

      {!!formError && <div className="error-banner" style={{ marginTop: 10 }}>{formError}</div>}

      <LeadListSection
        t={t}
        rows={rows}
        leads={leads}
        selectedIds={selectedIds}
        allChecked={allChecked}
        toggleAll={toggleAll}
        toggleOne={toggleOne}
        canWrite={canWrite}
        editLead={editLead}
        convertLead={convertLead}
        pagination={pagination}
        onPageChange={onPageChange}
        onSizeChange={onSizeChange}
      />
    </section>
  )
}

export default memo(LeadsPanel)
