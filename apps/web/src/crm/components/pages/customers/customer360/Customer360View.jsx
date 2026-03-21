import { memo, useMemo } from 'react'
import VirtualListTable from '../../../VirtualListTable'
import { formatMoney, translateStatus } from '../../../../shared'
import { toArray } from './shared'
import { buildCustomer360SectionConfigs } from './customer360ViewSections'

const Customer360ItemActions = memo(function Customer360ItemActions({ t, onOpen, onCopyId }) {
  return (
    <div className="crm360-item-actions">
      {onOpen ? <button className="mini-btn" onClick={onOpen}>{t('openSource')}</button> : null}
      {onCopyId ? <button className="mini-btn" onClick={onCopyId}>{t('copyId')}</button> : null}
    </div>
  )
})

const Customer360SectionRow = memo(function Customer360SectionRow({
  item,
  t,
  mainText,
  statusText,
  timeText,
  onOpen,
  onCopyId,
  onQuickAction,
  quickLabel,
}) {
  return (
    <div className="table-row table-row-4 crm360-table-row">
      <span>{mainText || '-'}</span>
      <span>{statusText || '-'}</span>
      <span>{timeText || '-'}</span>
      <span>
        <Customer360ItemActions t={t} onOpen={onOpen ? () => onOpen(item) : null} onCopyId={onCopyId ? () => onCopyId(item) : null} />
        {onQuickAction && quickLabel ? <button className="mini-btn" onClick={() => onQuickAction(item)}>{quickLabel}</button> : null}
      </span>
    </div>
  )
})

const Customer360SectionTable = memo(function Customer360SectionTable({
  t,
  title,
  items,
  emptyText,
  buildMainText,
  buildStatusText,
  buildTimeText,
  onOpen,
  onCopyId,
  onQuickAction,
  quickLabel,
  loading,
  error,
  onRetry,
}) {
  const rows = useMemo(() => toArray(items), [items])
  const viewportHeight = useMemo(
    () => Math.min(240, Math.max(56, rows.length * 52)),
    [rows.length],
  )
  return (
    <section className="crm360-section">
      <div className="crm360-section-head"><h4>{title}</h4></div>
      {loading ? <div className="loading">{t('loading')}</div> : null}
      {!loading && error ? (
        <div className="empty-tip">
          {error}
          {onRetry ? <button className="mini-btn" onClick={onRetry}>{t('refresh')}</button> : null}
        </div>
      ) : null}
      {!loading && !error && !rows.length ? <div className="empty-tip">{emptyText}</div> : null}
      {!loading && !error && !!rows.length && (
        <>
          <div className="table-row table-head-row table-row-4 crm360-table-head">
            <span>{t('title')}</span>
            <span>{t('status')}</span>
            <span>{t('createdAt')}</span>
            <span>{t('action')}</span>
          </div>
          <VirtualListTable
            rows={rows}
            rowHeight={52}
            viewportHeight={viewportHeight}
            getRowKey={(row, index) => row?.id || row?.sourceId || `${title}-${index}`}
            renderRow={(item, _index, key) => (
              <Customer360SectionRow
                key={key}
                item={item}
                t={t}
                mainText={buildMainText(item)}
                statusText={buildStatusText(item)}
                timeText={buildTimeText(item)}
                onOpen={onOpen}
                onCopyId={onCopyId}
                onQuickAction={onQuickAction}
                quickLabel={quickLabel}
              />
            )}
          />
        </>
      )}
    </section>
  )
})

function Customer360View({
  t,
  customer,
  vm,
  loading,
  moduleMeta,
  mode,
  onBackToList,
  onOpenFullDetail,
  onQuickCreateFollowUp,
  onQuickCreateTask,
  onQuickCreateQuote,
  onQuickViewOrders,
  onQuickUrgeApproval,
  onNavigatePrev,
  onNavigateNext,
  onNavigateTarget,
  onRefreshModules,
  onRefresh,
}) {
  const headerClass = mode === 'page' ? 'crm360-header crm360-header-page' : 'crm360-header'
  const related = vm.related
  const sectionConfigs = useMemo(
    () =>
      buildCustomer360SectionConfigs({
        t,
        customer,
        related,
        moduleMeta,
        onNavigateTarget,
        onQuickCreateFollowUp,
        onQuickCreateQuote,
        onQuickViewOrders,
        onQuickUrgeApproval,
        onRefreshModules,
      }),
    [
      t,
      customer,
      related,
      moduleMeta,
      onNavigateTarget,
      onQuickCreateFollowUp,
      onQuickCreateQuote,
      onQuickViewOrders,
      onQuickUrgeApproval,
      onRefreshModules,
    ],
  )
  return (
    <div className={mode === 'page' ? 'crm360-page' : 'crm360-drawer'}>
      <div className={headerClass}>
        <div>
          <h3>{customer?.name || `${t('customers')} #${customer?.id || '-'}`}</h3>
          <div className="crm360-subtitle">
            {t('owner')}: {customer?.owner || '-'} | {t('status')}: {translateStatus(t, customer?.status)}
          </div>
        </div>
        <div className="crm360-head-actions">
          {mode === 'drawer' ? <button className="mini-btn" onClick={onNavigatePrev}>{t('pagePrev')}</button> : null}
          {mode === 'drawer' ? <button className="mini-btn" onClick={onNavigateNext}>{t('pageNext')}</button> : null}
          {mode === 'drawer' ? <button className="mini-btn" onClick={onOpenFullDetail}>{t('customer360OpenPage')}</button> : null}
          {mode === 'page' ? <button className="mini-btn" onClick={onBackToList}>{t('customer360BackToList')}</button> : null}
          <button className="mini-btn" onClick={onQuickCreateFollowUp}>{t('quickCreateFollowUp')}</button>
          <button className="mini-btn" onClick={onQuickCreateTask}>{t('quickCreateTask')}</button>
          <button className="mini-btn" onClick={onQuickCreateQuote}>{t('quickCreateQuote')}</button>
          <button className="mini-btn" onClick={onQuickViewOrders}>{t('quickViewOrders')}</button>
          <button className="mini-btn" onClick={onQuickUrgeApproval}>{t('quickUrgeApproval')}</button>
          <button className="mini-btn" onClick={onRefresh}>{t('refresh')}</button>
        </div>
      </div>
      {loading ? <div className="loading">{t('loading')}</div> : null}
      <div className="crm360-kpis">
        <div className="kpi"><strong>{t('recent30FollowUps')}</strong><span>{vm.metrics.recentFollowUps30d}</span></div>
        <div className="kpi"><strong>{t('inFlightOpportunityAmount')}</strong><span>{formatMoney(vm.metrics.inFlightAmount)}</span></div>
        <div className="kpi"><strong>{t('orderTarget')}</strong><span>{formatMoney(vm.metrics.orderAmount)}</span></div>
        <div className="kpi"><strong>{t('paymentReceived')}</strong><span>{formatMoney(vm.metrics.paymentReceived)}</span></div>
        <div className="kpi"><strong>{t('paymentOutstanding')}</strong><span>{formatMoney(vm.metrics.paymentOutstanding)}</span></div>
        <div className="kpi"><strong>{t('pendingApprovals')}</strong><span>{vm.metrics.pendingApprovals}</span></div>
      </div>
      <div className="crm360-risk-tags">
        <strong>{t('riskTags')}:</strong>
        {!vm.riskTags.length ? <span>{t('noRiskTags')}</span> : null}
        {vm.riskTags.map((tag) => <span className="crm360-risk-tag" key={tag}>{tag}</span>)}
      </div>
      <div className="crm360-grid">
        {sectionConfigs.map((section) => (
          <Customer360SectionTable
            key={section.key}
            t={t}
            title={section.title}
            items={section.items}
            emptyText={t('noData')}
            buildMainText={section.buildMainText}
            buildStatusText={section.buildStatusText}
            buildTimeText={section.buildTimeText}
            onOpen={section.onOpen}
            onCopyId={section.onCopyId}
            onQuickAction={section.onQuickAction}
            quickLabel={section.quickLabel}
            loading={section.loading}
            error={section.error}
            onRetry={section.onRetry}
          />
        ))}
      </div>
      <section className="crm360-section">
        <div className="crm360-section-head"><h4>{t('timeline')}</h4></div>
        {!vm.timeline.length ? <div className="empty-tip">{t('noData')}</div> : null}
        {vm.timeline.map((item, idx) => (
          <div className="drawer-timeline-item" key={`${item.sourceId || idx}-${idx}`}>
            <div>{item.title}</div>
            <small>{item.time ? String(item.time).replace('T', ' ').slice(0, 16) : '-'} | {translateStatus(t, item.status)}</small>
          </div>
        ))}
      </section>
    </div>
  )
}

export default memo(Customer360View)
