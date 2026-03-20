import { memo } from 'react'
import SidebarNav from '../SidebarNav'
import MainContent from '../MainContent'

function PerfLine({ label, value, onCopy }) {
  return (
    <div className="small-tip perf-truncate-row">
      <span className="perf-truncate-label">{label}</span>
      <button
        type="button"
        className="mini-btn perf-truncate-btn"
        title={String(value || '-')}
        onClick={() => onCopy(value)}
      >
        {value || '-'}
      </button>
    </div>
  )
}

function AppShell({
  auth,
  navGroups,
  activePage,
  onNavigate,
  onPrefetch,
  performLogout,
  t,
  dev,
  perfMetrics,
  currentLoaderKey,
  lastRefreshReason,
  currentPageSignature,
  currentSignatureHit,
  recentWorkbenchJump,
  domainLoadSource,
  copyPerfSnapshot,
  lastPerfSnapshotAt,
  }) {
  const customer360Warn = perfMetrics.customer360_regression_risk === 'WARN'
  const customer360WarnFlags = Array.isArray(perfMetrics.customer360_warning_flags)
    ? perfMetrics.customer360_warning_flags
    : []
  const customer360Lowest = Array.isArray(perfMetrics.customer360_module_cache_hit_lowest)
    ? perfMetrics.customer360_module_cache_hit_lowest
    : []
  const customer360Slowest = Array.isArray(perfMetrics.customer360_module_refresh_latency_highest)
    ? perfMetrics.customer360_module_refresh_latency_highest
    : []
  const customer360PrefetchModules = Array.isArray(perfMetrics.customer360_prefetch_modules)
    ? perfMetrics.customer360_prefetch_modules
    : []
  const customer360LowCacheWarn = customer360Lowest.filter((row) => Number(row?.value || 0) < 30)
  const customer360SlowWarn = customer360Slowest.filter((row) => Number(row?.value || 0) > 600)
  const copyInlineText = async (text) => {
    if (typeof navigator === 'undefined' || !navigator.clipboard?.writeText) return
    const value = String(text || '').trim()
    if (!value) return
    try {
      await navigator.clipboard.writeText(value)
    } catch {
      // ignore copy errors
    }
  }

  return (
    <div className="app-shell">
      <SidebarNav auth={auth} navGroups={navGroups} activePage={activePage} onNavigate={onNavigate} onPrefetch={onPrefetch} onLogout={performLogout} t={t} />
      <MainContent />
      {dev && (
        <aside className="perf-panel">
          <h4>Perf</h4>
          <div>FP: {perfMetrics.nav_click_to_first_paint}ms</div>
          <div>FI: {perfMetrics.page_first_interactive_ms || 0}ms</div>
          <div>Fetch: {perfMetrics.active_page_fetch_ms}ms</div>
          <div>RenderCommit: {perfMetrics.page_render_commit_count || 0}</div>
          <div>Cache: {perfMetrics.cache_hit_rate}%</div>
          <div>Abort: {perfMetrics.api_abort_count}</div>
          <div>Preload: {perfMetrics.chunk_preload_hit}</div>
          <div>WB Hit: {perfMetrics.workbench_jump_hit_rate}%</div>
          <div>WB Dedup: {perfMetrics.workbench_jump_duplicate_blocked_count || 0}</div>
          <div>WB Anomaly: {perfMetrics.workbench_jump_anomaly_count || 0}</div>
          <div>WB Action OK: {perfMetrics.workbench_action_success_rate}%</div>
          <div>C360 Action OK: {perfMetrics.customer360_action_success_rate || 0}%</div>
          <div>C360 Refresh: {perfMetrics.customer360_module_refresh_latency || 0}ms</div>
          <div>C360 Jump Hit: {perfMetrics.customer360_jump_hit_rate || 0}%</div>
          <div className={customer360Warn ? 'perf-warn' : ''}>C360 Cache Hit: {perfMetrics.customer360_module_cache_hit_rate || 0}%</div>
          <div className={customer360Warn ? 'perf-warn' : ''}>C360 Prefetch Hit: {perfMetrics.customer360_prefetch_hit_rate || 0}%</div>
          <div>C360 Prefetch Abort: {perfMetrics.customer360_prefetch_abort_count || 0}</div>
          <div className={customer360Warn ? 'perf-warn' : ''}>C360 Risk: {perfMetrics.customer360_regression_risk || 'OK'}</div>
          {customer360WarnFlags.length ? (
            <div className="small-tip perf-warn">C360 Warn: {customer360WarnFlags.join(', ')}</div>
          ) : null}
          {customer360PrefetchModules.length ? (
            <div className="small-tip">C360 Prefetch Modules: {customer360PrefetchModules.join(', ')}</div>
          ) : null}
          {customer360Lowest.length ? (
            <div className="small-tip">C360 Cache Low: {customer360Lowest.map((row) => `${row.module}(${row.value}%)`).join(', ')}</div>
          ) : null}
          {customer360LowCacheWarn.length ? (
            <div className="small-tip perf-warn">C360 Cache Warn: {customer360LowCacheWarn.map((row) => `${row.module}(${row.value}%)`).join(', ')}</div>
          ) : null}
          {customer360Slowest.length ? (
            <div className="small-tip">C360 Slow: {customer360Slowest.map((row) => `${row.module}(${row.value}ms)`).join(', ')}</div>
          ) : null}
          {customer360SlowWarn.length ? (
            <div className="small-tip perf-warn">C360 Slow Warn: {customer360SlowWarn.map((row) => `${row.module}(${row.value}ms)`).join(', ')}</div>
          ) : null}
          <div>Dedup: {perfMetrics.duplicate_fetch_blocked_count}</div>
          <div>Pollers: {perfMetrics.polling_active_instances}</div>
          <div>LoginLeakBlocked: {perfMetrics.login_error_leak_blocked_count}</div>
          <div>FallbackUsed: {perfMetrics.loader_fallback_used_count}</div>
          <div>AuthMisroute: {perfMetrics.auth_channel_misroute_count}</div>
          <div>RefreshAnomaly: {perfMetrics.refresh_source_anomaly_count}</div>
          <PerfLine label="Loader:" value={currentLoaderKey || '-'} onCopy={copyInlineText} />
          <PerfLine label="Reason:" value={lastRefreshReason || '-'} onCopy={copyInlineText} />
          <PerfLine label="Signature:" value={currentPageSignature || '-'} onCopy={copyInlineText} />
          <div className="small-tip">SignatureHit: {currentSignatureHit ? 'YES' : 'NO'}</div>
          <div className="small-tip">WB Jump: {recentWorkbenchJump?.targetPage || '-'} ({recentWorkbenchJump?.hit ? 'HIT' : 'MISS'})</div>
          <PerfLine label="WB Reason:" value={recentWorkbenchJump?.reason || '-'} onCopy={copyInlineText} />
          <PerfLine label="WB Signature:" value={recentWorkbenchJump?.signature || '-'} onCopy={copyInlineText} />
          <div className="small-tip">WB At: {recentWorkbenchJump?.at ? new Date(recentWorkbenchJump.at).toLocaleTimeString() : '-'}</div>
          <div className="small-tip">DomainLoad:</div>
          <PerfLine label="Customer:" value={`${domainLoadSource?.customer?.source || '-'}${domainLoadSource?.customer?.at ? ` @ ${new Date(domainLoadSource.customer.at).toLocaleTimeString()}` : ''}`} onCopy={copyInlineText} />
          <PerfLine label="Commerce:" value={`${domainLoadSource?.commerce?.source || '-'}${domainLoadSource?.commerce?.at ? ` @ ${new Date(domainLoadSource.commerce.at).toLocaleTimeString()}` : ''}`} onCopy={copyInlineText} />
          <PerfLine label="Governance:" value={`${domainLoadSource?.governance?.source || '-'}${domainLoadSource?.governance?.at ? ` @ ${new Date(domainLoadSource.governance.at).toLocaleTimeString()}` : ''}`} onCopy={copyInlineText} />
          <PerfLine label="Approval:" value={`${domainLoadSource?.approval?.source || '-'}${domainLoadSource?.approval?.at ? ` @ ${new Date(domainLoadSource.approval.at).toLocaleTimeString()}` : ''}`} onCopy={copyInlineText} />
          <PerfLine label="Reporting:" value={`${domainLoadSource?.reporting?.source || '-'}${domainLoadSource?.reporting?.at ? ` @ ${new Date(domainLoadSource.reporting.at).toLocaleTimeString()}` : ''}`} onCopy={copyInlineText} />
          <PerfLine label="Workbench:" value={`${domainLoadSource?.workbench?.source || '-'}${domainLoadSource?.workbench?.at ? ` @ ${new Date(domainLoadSource.workbench.at).toLocaleTimeString()}` : ''}`} onCopy={copyInlineText} />
          <div className="inline-tools" style={{ marginTop: 8 }}>
            <button className="mini-btn" onClick={copyPerfSnapshot}>Copy Perf JSON</button>
          </div>
          {lastPerfSnapshotAt && <div className="small-tip">Copied at: {new Date(lastPerfSnapshotAt).toLocaleTimeString()}</div>}
        </aside>
      )}
    </div>
  )
}

export default memo(AppShell)
