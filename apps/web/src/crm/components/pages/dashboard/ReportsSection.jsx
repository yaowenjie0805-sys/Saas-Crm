import { memo } from 'react'
import BarChartRow from '../../BarChartRow'
import {
  formatMoneyByCurrency,
  translateChannel,
  translateOwnerAlias,
  translateStage,
  translateStatus,
} from '../../../shared'

function toBarEntries(data) {
  if (Array.isArray(data)) return data
  if (data && typeof data === 'object') {
    return Object.entries(data).map(([label, value]) => ({ label, value }))
  }
  return []
}

/**
 * 报表图表部分
 */
function ReportsSection({
  reports,
  reportCurrency,
  t,
}) {
  if (!reports) {
    return <div className="empty-tip">{t('loadingReports')}</div>
  }

  const marketContext = reports?.marketContext || null
  const localizedMetrics = reports?.localizedMetrics || null
  const reportSummary = reports?.summary || {}

  const marketProfile = marketContext?.marketProfile || 'CN'
  const marketCurrency = marketContext?.currency || reportCurrency || 'CNY'
  const marketTimezone = marketContext?.timezone || 'Asia/Shanghai'
  const approvalMode = marketContext?.approvalMode || 'STRICT'
  const pipelineHealth = Number(localizedMetrics?.pipelineHealth || 0)
  const arrLike = Number(localizedMetrics?.arrLike || 0)
  const tenantConfigSynced = !!reports?.tenantConfigSynced
  const localizedFallback = !!reports?.localizedFallback

  const ownerBars = toBarEntries(reports?.customerByOwner)
    .map((item) => ({ ...item, label: translateOwnerAlias(t, item.label) }))
  const statusBars = toBarEntries(reports?.revenueByStatus)
    .map((item) => ({ ...item, label: translateStatus(t, item.label) }))
  const stageBars = toBarEntries(reports?.opportunityByStage)
    .map((item) => ({ ...item, label: translateStage(t, item.label) }))
  const channelBars = toBarEntries(reports?.followUpByChannel)
    .map((item) => ({ ...item, label: translateChannel(t, item.label) }))

  return (
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
        <div className="report-card">
          <h4>{t('customerByOwner')}</h4>
          {ownerBars.map((b) => <BarChartRow key={b.label} label={b.label} value={b.value} />)}
        </div>
        <div className="report-card">
          <h4>{t('revenueByStatus')}</h4>
          {statusBars.map((b) => <BarChartRow key={b.label} label={b.label} value={b.value} money />)}
        </div>
        <div className="report-card">
          <h4>{t('opportunityByStage')}</h4>
          {stageBars.map((b) => <BarChartRow key={b.label} label={b.label} value={b.value} />)}
        </div>
        <div className="report-card">
          <h4>{t('followByChannel')}</h4>
          {channelBars.map((b) => <BarChartRow key={b.label} label={b.label} value={b.value} />)}
        </div>
      </div>
    </>
  )
}

export default memo(ReportsSection)
