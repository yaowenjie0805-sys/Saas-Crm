import { memo, useMemo } from 'react'
import VirtualListTable from '../../../VirtualListTable'
import { formatMoney, formatDateTime, translateStatus, translateStage } from '../../../../shared'
import { copyIdToClipboard, toArray } from './shared'

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
        <Customer360SectionTable t={t} title={t('contacts')} items={related.contacts} emptyText={t('noData')} buildMainText={(item) => `${item.name || '-'} (${item.title || '-'})`} buildStatusText={() => '-'} buildTimeText={(item) => formatDateTime(item.updatedAt || item.createdAt)} onOpen={(item) => onNavigateTarget && onNavigateTarget('contacts', { q: item.name || '', page: 1 })} onCopyId={(item) => copyIdToClipboard(item.id)} onQuickAction={(item) => onQuickCreateFollowUp && onQuickCreateFollowUp({ customerId: customer?.id, id: customer?.id, owner: customer?.owner, q: item.name || '' })} quickLabel={t('quickCreateFollowUp')} loading={moduleMeta?.contacts?.loading} error={moduleMeta?.contacts?.error} onRetry={() => onRefreshModules?.(['contacts'])} />
        <Customer360SectionTable t={t} title={t('pipeline')} items={related.opportunities} emptyText={t('noData')} buildMainText={(item) => item.title || `${item.owner || '-'} / ${translateStage(t, item.stage)}`} buildStatusText={(item) => translateStage(t, item.stage)} buildTimeText={(item) => formatDateTime(item.updatedAt || item.createdAt)} onOpen={(item) => onNavigateTarget && onNavigateTarget('pipeline', { q: item.owner || '', page: 1 })} onCopyId={(item) => copyIdToClipboard(item.id)} onQuickAction={() => onQuickCreateQuote && onQuickCreateQuote()} quickLabel={t('quickCreateQuote')} loading={moduleMeta?.opportunities?.loading} error={moduleMeta?.opportunities?.error} onRetry={() => onRefreshModules?.(['opportunities'])} />
        <Customer360SectionTable t={t} title={t('quotes')} items={related.quotes} emptyText={t('noData')} buildMainText={(item) => item.title || item.quoteNo || item.id} buildStatusText={(item) => translateStatus(t, item.status)} buildTimeText={(item) => formatDateTime(item.updatedAt || item.createdAt)} onOpen={(item) => onNavigateTarget && onNavigateTarget('quotes', { owner: item.owner || '', status: item.status || '', opportunityId: item.opportunityId || '', customerId: item.customerId || customer?.id || '', page: 1 })} onCopyId={(item) => copyIdToClipboard(item.id)} onQuickAction={() => onQuickViewOrders && onQuickViewOrders()} quickLabel={t('quickViewOrders')} loading={moduleMeta?.quotes?.loading} error={moduleMeta?.quotes?.error} onRetry={() => onRefreshModules?.(['quotes'])} />
        <Customer360SectionTable t={t} title={t('orders')} items={related.orders} emptyText={t('noData')} buildMainText={(item) => item.orderNo || item.id} buildStatusText={(item) => translateStatus(t, item.status)} buildTimeText={(item) => formatDateTime(item.updatedAt || item.createdAt)} onOpen={(item) => onNavigateTarget && onNavigateTarget('orders', { owner: item.owner || '', status: item.status || '', opportunityId: item.opportunityId || '', customerId: item.customerId || customer?.id || '', page: 1 })} onCopyId={(item) => copyIdToClipboard(item.id)} onQuickAction={() => onQuickViewOrders && onQuickViewOrders()} quickLabel={t('quickViewOrders')} loading={moduleMeta?.orders?.loading} error={moduleMeta?.orders?.error} onRetry={() => onRefreshModules?.(['orders'])} />
        <Customer360SectionTable t={t} title={t('contracts')} items={related.contracts} emptyText={t('noData')} buildMainText={(item) => item.contractNo || item.title || item.id} buildStatusText={(item) => translateStatus(t, item.status)} buildTimeText={(item) => formatDateTime(item.signDate || item.updatedAt || item.createdAt)} onOpen={(item) => onNavigateTarget && onNavigateTarget('contracts', { status: item.status || '', q: item.contractNo || '', page: 1 })} onCopyId={(item) => copyIdToClipboard(item.id)} onQuickAction={() => onQuickUrgeApproval && onQuickUrgeApproval()} quickLabel={t('quickUrgeApproval')} loading={moduleMeta?.contracts?.loading} error={moduleMeta?.contracts?.error} onRetry={() => onRefreshModules?.(['contracts'])} />
        <Customer360SectionTable t={t} title={t('payments')} items={related.payments} emptyText={t('noData')} buildMainText={(item) => formatMoney(item.amount)} buildStatusText={(item) => translateStatus(t, item.status)} buildTimeText={(item) => formatDateTime(item.receivedDate || item.updatedAt || item.createdAt)} onOpen={(item) => onNavigateTarget && onNavigateTarget('payments', { status: item.status || '', page: 1 })} onCopyId={(item) => copyIdToClipboard(item.id)} loading={moduleMeta?.payments?.loading} error={moduleMeta?.payments?.error} onRetry={() => onRefreshModules?.(['payments'])} />
        <Customer360SectionTable t={t} title={t('approvals')} items={related.approvals} emptyText={t('noData')} buildMainText={(item) => `${item.bizType || '-'} #${item.bizId || '-'} `} buildStatusText={(item) => translateStatus(t, item.status)} buildTimeText={(item) => formatDateTime(item.createdAt)} onOpen={(item) => onNavigateTarget && onNavigateTarget('approvals', { status: item.status || 'PENDING', page: 1 })} onCopyId={(item) => copyIdToClipboard(item.id)} onQuickAction={() => onQuickUrgeApproval && onQuickUrgeApproval()} quickLabel={t('quickUrgeApproval')} loading={moduleMeta?.approvals?.loading} error={moduleMeta?.approvals?.error} onRetry={() => onRefreshModules?.(['approvals'])} />
        <Customer360SectionTable t={t} title={t('audit')} items={related.audits} emptyText={t('noData')} buildMainText={(item) => item.action || '-'} buildStatusText={() => '-'} buildTimeText={(item) => formatDateTime(item.createdAt)} onOpen={(item) => onNavigateTarget && onNavigateTarget('audit', { q: String(item.entityId || customer?.id || ''), page: 1 })} onCopyId={(item) => copyIdToClipboard(item.id)} loading={moduleMeta?.audits?.loading} error={moduleMeta?.audits?.error} onRetry={() => onRefreshModules?.(['audits'])} />
        <Customer360SectionTable t={t} title={t('notifications')} items={related.notifications} emptyText={t('noData')} buildMainText={(item) => item.eventType || item.targetType || '-'} buildStatusText={(item) => translateStatus(t, item.status)} buildTimeText={(item) => formatDateTime(item.createdAt)} onOpen={() => onNavigateTarget && onNavigateTarget('approvals', { status: 'PENDING', page: 1 })} onCopyId={(item) => copyIdToClipboard(item.id)} loading={moduleMeta?.notifications?.loading} error={moduleMeta?.notifications?.error} onRetry={() => onRefreshModules?.(['notifications'])} />
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
