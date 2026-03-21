import ListState from '../../ListState'
import ServerPager from '../../ServerPager'
import VirtualListTable from '../../VirtualListTable'
import { LeadImportExportJobRow, LeadImportJobRow } from './LeadPanelRows'
import { IMPORT_EXPORT_STATUS_VALUES, IMPORT_PAGE_SIZE_VALUES, IMPORT_STATUS_VALUES } from './leadPanelConstants'
import { translateStatus } from '../../../shared'

export default function LeadImportSection({
  t,
  loading,
  importJob,
  importMetrics,
  importJobsView,
  importStatus,
  setImportStatus,
  importPaging,
  onImportPageChange,
  onImportSizeChange,
  selectImportJob,
  cancelImportJob,
  retryImportJob,
  importFailedRowsView,
  importExportStatus,
  setImportExportStatus,
  importExportPaging,
  onImportExportPageChange,
  onImportExportSizeChange,
  canCreateImportExport,
  createImportFailedRowsExportJob,
  importExportJobsView,
  downloadImportFailedRowsExportJob,
  isImportActionPending,
}) {
  return (
    <div className="panel" style={{ marginTop: 12, boxShadow: 'none' }}>
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
  )
}
