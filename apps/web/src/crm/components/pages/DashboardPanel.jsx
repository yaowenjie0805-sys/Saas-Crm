import { memo } from 'react'
import BarChartRow from '../BarChartRow'
import { formatDateTime, formatMoneyByCurrency, formatStatValue, mapToBars, translateChannel, translateOwnerAlias, translateRole, translateStage, translateStatLabel, translateStatus } from '../../shared'
import ServerPager from '../ServerPager'
import VirtualListTable from '../VirtualListTable'
import { buildCardMeta, buildStatusLabel, createOwnerAliasMap, mapWorkbenchItems } from './dashboard/dashboardPanelHelpers'

function DashboardPanel({
  activePage,
  refreshPage,
  stats,
  reports,
  workbenchToday,
  onWorkbenchNavigate,
  quickCreateTask,
  trackWorkbenchEvent,
  t,
  canViewReports,
  auditFrom,
  auditTo,
  auditRole,
  reportOwner,
  setReportOwner,
  reportDepartment,
  setReportDepartment,
  reportTimezone,
  setReportTimezone,
  reportCurrency,
  setReportCurrency,
  exportReportCsv,
  reportExportJobs,
  reportExportStatusFilter,
  setReportExportStatusFilter,
  reportExportJobsPage,
  setReportExportJobsPage,
  reportExportJobsTotalPages,
  reportExportJobsSize,
  setReportExportJobsSize,
  autoRefreshReportJobs,
  setAutoRefreshReportJobs,
  retryReportExportJob,
  downloadReportExportJob,
}) {
  if (!['dashboard', 'reports'].includes(activePage)) return null

  const reportSummary = reports?.summary || {}
  const marketContext = reports?.marketContext || null
  const localizedMetrics = reports?.localizedMetrics || null
  const marketProfile = marketContext?.marketProfile || 'CN'
  const marketCurrency = marketContext?.currency || reportCurrency || 'CNY'
  const marketTimezone = marketContext?.timezone || reportTimezone || 'Asia/Shanghai'
  const approvalMode = marketContext?.approvalMode || 'STRICT'
  const pipelineHealth = Number(localizedMetrics?.pipelineHealth || 0)
  const arrLike = Number(localizedMetrics?.arrLike || 0)
  const localizedFallback = !!reports?.localizedFallback
  const tenantConfigSynced = !!reports?.tenantConfigSynced
  const ownerAlias = createOwnerAliasMap(t)
  const ownerBars = mapToBars(reports?.customerByOwner).map((item) => ({ ...item, label: ownerAlias[item.label] || translateOwnerAlias(t, item.label) }))
  const statusBars = mapToBars(reports?.revenueByStatus).map((item) => ({ ...item, label: translateStatus(t, item.label) }))
  const stageBars = mapToBars(reports?.opportunityByStage).map((item) => ({ ...item, label: translateStage(t, item.label) }))
  const channelBars = mapToBars(reports?.followUpByChannel).map((item) => ({ ...item, label: translateChannel(t, item.label) }))
  const normalizedStats = (stats || []).map((item) => ({
    ...item,
    label: translateStatLabel(t, item.label),
    value: formatStatValue(t, item.label, item.value),
  }))
  const workbenchCards = workbenchToday?.cards || []
  const todoItems = mapWorkbenchItems(workbenchToday?.todoItems, t)
  const warningItems = mapWorkbenchItems(workbenchToday?.warningItems, t)
  const cardLabel = (row) => t(row?.labelKey || row?.key || '')
  const cardMeta = buildCardMeta()

  const jobs = (reportExportJobs || []).filter((job) => {
    if (reportExportStatusFilter === 'ALL') return true
    return String(job.status || '') === reportExportStatusFilter
  })
  const refreshReportJobs = () => refreshPage('reports', 'panel_action')

  return (
    <>
      {activePage === 'dashboard' && (
        <>
          <section className="stats-grid">
            {normalizedStats.map((s) => (
              <article key={s.label} className="stat-card">
                <p>{s.label}</p>
                <h3>{s.value}</h3>
                <span>{s.trend}</span>
              </article>
            ))}
          </section>

          <section className="panel">
            <div className="panel-head">
              <h2>{t('workbenchToday')}</h2>
              <div className="inline-tools">
                <button className="mini-btn" onClick={() => quickCreateTask?.({})}>{t('quickCreateTask')}</button>
                <button className="mini-btn" onClick={() => onWorkbenchNavigate?.('quotes')}>{t('quickCreateQuote')}</button>
              </div>
            </div>
            <div className="stats-grid">
              {workbenchCards.map((card) => {
                const meta = cardMeta[card.key] || { targetPage: 'dashboard', payload: {}, actionKey: 'workbenchView' }
                const targetPage = meta.targetPage
                const payload = meta.payload
                return (
                  <article key={card.key} className={`stat-card workbench-card level-${String(card.level || 'info').toLowerCase()}`}>
                    <p>{cardLabel(card)}</p>
                    <h3>{card.value || 0}</h3>
                    <div className="inline-tools filter-bar" style={{ marginBottom: 0 }}>
                      <button className="mini-btn" onClick={() => {
                        trackWorkbenchEvent?.('card_view_click', { key: card.key, targetPage, payload })
                        onWorkbenchNavigate?.(targetPage, payload)
                      }}>{t('workbenchView')}</button>
                      <button className="mini-btn" onClick={() => {
                        trackWorkbenchEvent?.('card_action_click', { key: card.key, targetPage, payload })
                        onWorkbenchNavigate?.(targetPage, payload)
                      }}>{t(meta.actionKey)}</button>
                    </div>
                  </article>
                )
              })}
            </div>
            <div className="inline-tools filter-bar" style={{ marginTop: 10 }}>
              <span className="muted-filter">{t('workbenchQuickActions')}</span>
              <button className="mini-btn" onClick={() => onWorkbenchNavigate?.('followUps')}>{t('quickCreateFollowUp')}</button>
              <button className="mini-btn" onClick={() => quickCreateTask?.({})}>{t('quickCreateTask')}</button>
              <button className="mini-btn" onClick={() => onWorkbenchNavigate?.('quotes')}>{t('quickCreateQuote')}</button>
              <button className="mini-btn" onClick={() => onWorkbenchNavigate?.('approvals', { status: 'PENDING' })}>{t('quickUrgeApproval')}</button>
            </div>
            <div className="workbench-grid">
              <div className="workbench-col">
                <h4>{t('todayTodo')}</h4>
                {todoItems.map((item) => (
                  <button key={`${item.type}-${item.id}`} className="workbench-item" onClick={() => {
                    trackWorkbenchEvent?.('todo_click', { id: item.id, type: item.type, targetPage: item.targetPage, payload: item.payload || {} })
                    onWorkbenchNavigate?.(item.targetPage || 'dashboard', item.payload || {})
                  }}>
                    <span>{item.title}</span>
                    <small>{item.statusText} · {item.ownerText}</small>
                  </button>
                ))}
                {todoItems.length === 0 && <div className="empty-tip">{t('noData')}</div>}
              </div>
              <div className="workbench-col">
                <h4>{t('warningList')}</h4>
                {warningItems.map((item) => (
                  <button key={`${item.type}-${item.id}`} className="workbench-item warning" onClick={() => {
                    trackWorkbenchEvent?.('warning_click', { id: item.id, type: item.type, targetPage: item.targetPage, payload: item.payload || {} })
                    onWorkbenchNavigate?.(item.targetPage || 'dashboard', item.payload || {})
                  }}>
                    <span>{item.title}</span>
                    <small>{item.statusText} · {item.ownerText}</small>
                  </button>
                ))}
                {warningItems.length === 0 && <div className="empty-tip">{t('noData')}</div>}
              </div>
            </div>
          </section>
        </>
      )}

      <section className="panel">
        <div className="panel-head">
          <h2>{t('reports')}</h2>
        </div>
        {canViewReports && (
          <div className="inline-tools filter-row" style={{ marginBottom: 8 }}>
            <span className="muted-filter">{t('filters')}: {auditFrom || '-'} ~ {auditTo || '-'} | {t('role')}: {auditRole ? translateRole(t, auditRole) : t('allRoles')}</span>
            <input className="tool-input" placeholder={t('reportOwner')} value={reportOwner} onChange={(e) => setReportOwner(e.target.value)} />
            <input className="tool-input" placeholder={t('reportDepartment')} value={reportDepartment} onChange={(e) => setReportDepartment(e.target.value)} />
            <input className="tool-input" placeholder={t('reportTimezone')} value={reportTimezone} onChange={(e) => setReportTimezone(e.target.value)} />
            <input className="tool-input" placeholder={t('reportCurrency')} value={reportCurrency} onChange={(e) => setReportCurrency(e.target.value.toUpperCase())} />
          </div>
        )}
        {canViewReports && (
          <div className="inline-tools filter-bar" style={{ marginBottom: 8 }}>
            <button className="mini-btn" onClick={exportReportCsv}>{t('exportReportCsv')}</button>
          </div>
        )}
        {!reports ? (
          <div className="empty-tip">{t('loadingReports')}</div>
        ) : (
          <>
            <div className="report-summary">
              <div><b>{t('reportsSummary')}</b></div>
              <div>{t('marketProfile')}: {marketProfile === 'GLOBAL' ? t('marketGlobal') : t('marketCN')}</div>
              <div>{t('approvalModeLabel')}: {approvalMode === 'STAGE_GATE' ? t('approvalModeStageGate') : t('approvalModeStrict')}</div>
              <div>{t('reportTimezone')}: {marketTimezone}</div>
              <div>{t('reportCurrency')}: {marketCurrency}</div>
              <div>{t('customers')}: {reportSummary.customers || 0}</div>
              <div>{t('amount')}: {formatMoneyByCurrency(reportSummary.revenue || 0, marketCurrency)}</div>
              <div>{t('pipeline')}: {reportSummary.opportunities || 0}</div>
              <div>{t('taskDoneRate')}: {reportSummary.taskDoneRate || 0}%</div>
              <div>{t('winRate')}: {reportSummary.winRate || 0}%</div>
              <div>{t('pipelineHealth')}: {pipelineHealth}%</div>
              <div>{t('arrLike')}: {formatMoneyByCurrency(arrLike, marketCurrency)}</div>
            </div>
            {!tenantConfigSynced && <div className="info-banner">{t('tenantConfigNotSyncedHint')}</div>}
            {localizedFallback && <div className="info-banner">{t('reportsLocalizedFallbackHint')}</div>}
            <div className="report-grid">
              <div className="report-card"><h4>{t('customerByOwner')}</h4>{ownerBars.map((b) => <BarChartRow key={b.label} label={b.label} value={b.value} />)}</div>
              <div className="report-card"><h4>{t('revenueByStatus')}</h4>{statusBars.map((b) => <BarChartRow key={b.label} label={b.label} value={b.value} money />)}</div>
              <div className="report-card"><h4>{t('opportunityByStage')}</h4>{stageBars.map((b) => <BarChartRow key={b.label} label={b.label} value={b.value} />)}</div>
              <div className="report-card"><h4>{t('followByChannel')}</h4>{channelBars.map((b) => <BarChartRow key={b.label} label={b.label} value={b.value} />)}</div>
            </div>
          </>
        )}
      </section>

      {canViewReports && (
        <section className="panel">
          <div className="panel-head">
            <h2>{t('reportExportJobs')}</h2>
            <div className="inline-tools filter-bar">
              <button className={`mini-btn ${reportExportStatusFilter === 'ALL' ? 'active' : ''}`} onClick={() => { setReportExportStatusFilter('ALL'); setReportExportJobsPage(1) }}>{t('filterAll')}</button>
              <button className={`mini-btn ${reportExportStatusFilter === 'RUNNING' ? 'active' : ''}`} onClick={() => { setReportExportStatusFilter('RUNNING'); setReportExportJobsPage(1) }}>{t('filterRunning')}</button>
              <button className={`mini-btn ${reportExportStatusFilter === 'FAILED' ? 'active' : ''}`} onClick={() => { setReportExportStatusFilter('FAILED'); setReportExportJobsPage(1) }}>{t('filterFailed')}</button>
              <button className={`mini-btn ${reportExportStatusFilter === 'DONE' ? 'active' : ''}`} onClick={() => { setReportExportStatusFilter('DONE'); setReportExportJobsPage(1) }}>{t('filterDone')}</button>
              <button className="mini-btn" onClick={refreshReportJobs}>{t('refresh')}</button>
              <label className="switch-inline"><input type="checkbox" checked={autoRefreshReportJobs} onChange={(e) => setAutoRefreshReportJobs(e.target.checked)} />{t('autoRefresh')}</label>
            </div>
          </div>
          <div className="table-row table-head-row table-row-6 compact">
            <span>{t('idLabel')}</span>
            <span>{t('status')}</span>
            <span>{t('progress')}</span>
            <span>{t('createdAt')}</span>
            <span>{t('filtersSummary')}</span>
            <span>{t('action')}</span>
          </div>
                    <VirtualListTable
            rows={jobs}
            rowHeight={42}
            viewportHeight={336}
            getRowKey={(job) => job.jobId}
            renderRow={(job) => (
              <div key={job.jobId} className="table-row table-row-6 compact">
                <span>{job.jobId}</span>
                <span>{buildStatusLabel(t, job.status)}</span>
                <span>{job.progress || 0}%</span>
                <span>{formatDateTime(job.createdAt)}</span>
                <span>{job.filters ? `${job.filters.role ? translateRole(t, job.filters.role) : '-'} | ${job.filters.from || '-'}~${job.filters.to || '-'}` : '-'}</span>
                <span>
                  {job.downloadReady
                    ? <button className="mini-btn" onClick={() => downloadReportExportJob(job.jobId)}>{t('exportDownload')}</button>
                    : <button className="mini-btn" onClick={() => retryReportExportJob(job.jobId)}>{t('retry')}</button>}
                </span>
              </div>
            )}
          />
          {jobs.length === 0 && <div className="empty-tip">{t('noData')}</div>}
          {jobs.length > 0 && (
            <ServerPager
              t={t}
              page={reportExportJobsPage}
              totalPages={reportExportJobsTotalPages}
              size={reportExportJobsSize}
              onPageChange={setReportExportJobsPage}
              onSizeChange={setReportExportJobsSize}
            />
          )}
        </section>
      )}
    </>
  )
}

export default memo(DashboardPanel)
