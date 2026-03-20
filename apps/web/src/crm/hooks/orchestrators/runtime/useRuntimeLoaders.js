import { useEffect, useMemo } from 'react'
import { useActivePagePolling } from '../../useActivePagePolling'
import { useLoaderOrchestrator } from '../../useLoaderOrchestrator'
import { REFRESH_REASONS } from './routeConfig'

/**
 * Composes loader orchestration and polling side effects for runtime pages.
 */
export function useRuntimeLoaders({
  authToken,
  activePage,
  commonPageLoaders,
  keyPageLoaders,
  loadReasonRef,
  beginPageRequest,
  canSkipFetch,
  isInFlight,
  markInFlight,
  clearInFlight,
  markCacheDecision,
  markDuplicateFetchBlocked,
  markWorkbenchJumpDecision,
  markFetched,
  markFetchLatency,
  markAbort,
  markLoaderFallbackUsed,
  handleError,
  setLastRefreshReason,
  setCurrentLoaderKey,
  setCurrentPageSignature,
  setCurrentSignatureHit,
  markDomainLoadSource,
  markRefreshSourceAnomaly,
  setError,
  setLoginError,
  locationPathname,
  setNotificationPage,
  notificationStatusFilter,
  notificationSize,
  setLeadImportPage,
  leadImportStatusFilter,
  leadImportSize,
  setExportJobsPage,
  exportStatusFilter,
  exportJobsSize,
  setReportExportJobsPage,
  reportExportStatusFilter,
  reportExportJobsSize,
  setLeadImportExportPage,
  leadImportExportStatusFilter,
  leadImportExportSize,
  leadImportJobId,
  autoRefreshJobs,
  exportJobs,
  autoRefreshReportJobs,
  reportExportJobs,
  leadImportJobs,
  leadImportExportJobs,
  markPollingActiveInstances,
}) {
  const hasAuthToken = !!authToken
  const { refreshPage } = useLoaderOrchestrator({
    loaders: {
      authToken: hasAuthToken ? authToken : null,
      activePage,
      commonPageLoaders,
      keyPageLoaders,
      loadReasonRef,
      refreshReasons: REFRESH_REASONS,
    },
    runtime: {
      beginPageRequest,
      canSkipFetch,
      isInFlight,
      markInFlight,
      clearInFlight,
    },
    metrics: {
      markCacheDecision,
      markDuplicateFetchBlocked,
      markWorkbenchJumpDecision,
      markFetched,
      markFetchLatency,
      markAbort,
      markLoaderFallbackUsed,
      setLastRefreshReason,
      setCurrentLoaderKey,
      setCurrentPageSignature,
      setCurrentSignatureHit,
    },
    handlers: {
      handleError,
      onLoaderLifecycle: markDomainLoadSource,
      markRefreshSourceAnomaly,
    },
  })

  useEffect(() => {
    if (!authToken) setError('')
  }, [authToken, setError])

  useEffect(() => {
    if (authToken) setLoginError('')
  }, [authToken, setLoginError])

  useEffect(() => {
    if (locationPathname === '/login' || locationPathname === '/activate') {
      setError('')
      if (!authToken) setLoginError('')
    }
  }, [authToken, locationPathname, setError, setLoginError])

  useEffect(() => { setNotificationPage(1) }, [notificationStatusFilter, setNotificationPage])
  useEffect(() => { setNotificationPage(1) }, [notificationSize, setNotificationPage])
  useEffect(() => { setLeadImportPage(1) }, [leadImportStatusFilter, leadImportSize, setLeadImportPage])
  useEffect(() => { setExportJobsPage(1) }, [exportStatusFilter, exportJobsSize, setExportJobsPage])
  useEffect(() => { setReportExportJobsPage(1) }, [reportExportStatusFilter, reportExportJobsSize, setReportExportJobsPage])
  useEffect(() => { setLeadImportExportPage(1) }, [leadImportExportStatusFilter, leadImportExportSize, leadImportJobId, setLeadImportExportPage])

  const activePollers = useMemo(() => ([
    {
      id: 'audit-export-jobs',
      intervalMs: 1800,
      canRun: () => !!authToken && activePage === 'audit' && autoRefreshJobs && exportJobs.some((j) => ['PENDING', 'RUNNING'].includes(j.status)),
      run: async (signal) => {
        if (signal.aborted) return
        await refreshPage('audit', 'panel_action')
      },
    },
    {
      id: 'report-export-jobs',
      intervalMs: 1800,
      canRun: () => !!authToken && ['dashboard', 'reports'].includes(activePage) && autoRefreshReportJobs && reportExportJobs.some((j) => ['PENDING', 'RUNNING'].includes(j.status)),
      run: async (signal) => {
        if (signal.aborted) return
        await refreshPage(activePage === 'dashboard' ? 'dashboard' : 'reports', 'panel_action')
      },
    },
    {
      id: 'lead-import-jobs',
      intervalMs: 3000,
      canRun: () => !!authToken && activePage === 'leads' && (leadImportJobs || []).some((j) => ['PENDING', 'RUNNING'].includes(String(j.status || '').toUpperCase())),
      run: async (signal) => {
        if (signal.aborted) return
        await refreshPage('leads', 'panel_action')
      },
    },
    {
      id: 'lead-import-export-jobs',
      intervalMs: 2500,
      canRun: () => !!authToken && activePage === 'leads' && !!leadImportJobId && (leadImportExportJobs || []).some((j) => ['PENDING', 'RUNNING'].includes(String(j.status || '').toUpperCase())),
      run: async () => {
        await refreshPage('leads', 'panel_action')
      },
    },
  ]), [
    authToken,
    activePage,
    autoRefreshJobs,
    exportJobs,
    autoRefreshReportJobs,
    reportExportJobs,
    leadImportJobs,
    leadImportJobId,
    leadImportExportJobs,
    refreshPage,
  ])

  useEffect(() => {
    const activeCount = (activePollers || []).filter((poller) => {
      try {
        return typeof poller?.canRun === 'function' && !!poller.canRun()
      } catch {
        return false
      }
    }).length
    markPollingActiveInstances(activeCount)
  }, [activePollers, markPollingActiveInstances])

  useActivePagePolling({ enabled: hasAuthToken, pollers: activePollers })

  return {
    refreshPage,
    activePollers,
  }
}
