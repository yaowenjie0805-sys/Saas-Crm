import {
  buildRuntimeDomainLoadersInput,
  buildRuntimePersistenceInput,
  buildRuntimePageLoadersInput,
  buildRuntimeLoadersInput,
  buildAppAuthModelInput,
  buildRuntimeFormValidatorsInput,
} from '../../runtimeEngineBuilders'

export function buildAuthModelHookInput(ctx, formatErrorMessage) {
  return buildAppAuthModelInput({
    runtime: ctx.runtime,
    auth: ctx.auth,
    setAuth: ctx.setAuth,
    resetDomainSlices: ctx.resetDomainSlices,
    abortAll: ctx.pagePolicy.abortAll,
    loadReasonRef: ctx.kernel.loadReasonRef,
    workbenchJumpRef: ctx.kernel.workbenchJumpRef,
    navigate: ctx.navigate,
    pathname: ctx.location.pathname,
    logoutGuardRef: ctx.kernel.logoutGuardRef,
    suppressLoginErrorUntilRef: ctx.kernel.suppressLoginErrorUntilRef,
    markLoginErrorLeakBlocked: ctx.perf.markLoginErrorLeakBlocked,
    markAuthChannelMisroute: ctx.perf.markAuthChannelMisroute,
    t: ctx.t,
    formatErrorMessage,
  })
}

export function buildFormValidatorsHookInput(ctx) {
  return buildRuntimeFormValidatorsInput({
    runtime: ctx.runtime,
    t: ctx.t,
    auth: ctx.auth,
  })
}

export function buildDomainLoadersHookInput(ctx, handleError, hasInvalidAuditRange) {
  return buildRuntimeDomainLoadersInput({
    runtime: ctx.runtime,
    auth: ctx.auth,
    lang: ctx.lang,
    seqRef: ctx.kernel.seqRef,
    beginPageRequest: ctx.pagePolicy.beginPageRequest,
    handleError,
    canViewOpsMetrics: ctx.kernel.canViewOpsMetrics,
    canManageUsers: ctx.kernel.canManageUsers,
    canManageSalesAutomation: ctx.kernel.canManageSalesAutomation,
    canViewAudit: ctx.kernel.canViewAudit,
    canViewReports: ctx.kernel.canViewReports,
    hasInvalidAuditRange,
    t: ctx.t,
  })
}

export function buildPersistenceHookInput(ctx, saveAuth, loadSsoConfig, handleLoginError) {
  return buildRuntimePersistenceInput({
    runtime: ctx.runtime,
    lang: ctx.lang,
    abortAll: ctx.pagePolicy.abortAll,
    auth: ctx.auth,
    locationPathname: ctx.location.pathname,
    navigate: ctx.navigate,
    saveAuth,
    loadSsoConfig,
    t: ctx.t,
    loginFormTenantId: ctx.runtime.loginForm?.tenantId,
    handleLoginError,
  })
}

export function buildPageLoadersHookInput(ctx, runtimeDomainLoaders) {
  return buildRuntimePageLoadersInput({
    runtime: ctx.runtime,
    leadImportJobId: ctx.runtime.leadImportJob?.id,
    loadLeads: runtimeDomainLoaders.loadLeads,
    loadLeadImportJobs: runtimeDomainLoaders.loadLeadImportJobs,
    loadLeadImportFailedRows: runtimeDomainLoaders.loadLeadImportFailedRows,
    loadLeadImportExportJobs: runtimeDomainLoaders.loadLeadImportExportJobs,
    loadLeadImportMetrics: runtimeDomainLoaders.loadLeadImportMetrics,
    canViewOpsMetrics: ctx.kernel.canViewOpsMetrics,
    loadCustomers: runtimeDomainLoaders.loadCustomers,
    loadOpportunities: runtimeDomainLoaders.loadOpportunities,
    loadContacts: runtimeDomainLoaders.loadContacts,
    loadContracts: runtimeDomainLoaders.loadContracts,
    loadPayments: runtimeDomainLoaders.loadPayments,
    commerceDomain: runtimeDomainLoaders.commerceDomain,
    loadFollowUps: runtimeDomainLoaders.loadFollowUps,
    loadTasks: runtimeDomainLoaders.loadTasks,
    canViewReports: ctx.kernel.canViewReports,
    loadWorkbenchToday: runtimeDomainLoaders.loadWorkbenchToday,
    loadReports: runtimeDomainLoaders.loadReports,
    loadReportExportJobs: runtimeDomainLoaders.loadReportExportJobs,
    canViewAudit: ctx.kernel.canViewAudit,
    loadAudit: runtimeDomainLoaders.loadAudit,
    loadExportJobs: runtimeDomainLoaders.loadExportJobs,
    loadPermissionMatrix: runtimeDomainLoaders.loadPermissionMatrix,
    loadPermissionConflicts: runtimeDomainLoaders.loadPermissionConflicts,
    canManageUsers: ctx.kernel.canManageUsers,
    loadAdminUsers: runtimeDomainLoaders.loadAdminUsers,
    loadTenants: runtimeDomainLoaders.loadTenants,
    canManageSalesAutomation: ctx.kernel.canManageSalesAutomation,
    loadLeadAssignmentRules: runtimeDomainLoaders.loadLeadAssignmentRules,
    loadAutomationRulesV1: runtimeDomainLoaders.loadAutomationRulesV1,
    loadApprovalTemplates: runtimeDomainLoaders.loadApprovalTemplates,
    loadApprovalStats: runtimeDomainLoaders.loadApprovalStats,
    loadApprovalInstances: runtimeDomainLoaders.loadApprovalInstances,
    loadApprovalTasks: runtimeDomainLoaders.loadApprovalTasks,
    loadNotificationJobs: runtimeDomainLoaders.loadNotificationJobs,
    loadDesignerTemplates: runtimeDomainLoaders.loadDesignerTemplates,
  })
}

export function buildRuntimeLoadersHookInput(ctx, pageLoaders, handleError) {
  return buildRuntimeLoadersInput({
    runtime: ctx.runtime,
    authToken: ctx.auth?.token,
    activePage: ctx.runtime.activePage,
    commonPageLoaders: pageLoaders.commonPageLoaders,
    keyPageLoaders: pageLoaders.keyPageLoaders,
    loadReasonRef: ctx.kernel.loadReasonRef,
    beginPageRequest: ctx.pagePolicy.beginPageRequest,
    canSkipFetch: ctx.pagePolicy.canSkipFetch,
    isInFlight: ctx.pagePolicy.isInFlight,
    markInFlight: ctx.pagePolicy.markInFlight,
    clearInFlight: ctx.pagePolicy.clearInFlight,
    markCacheDecision: ctx.perf.markCacheDecision,
    markDuplicateFetchBlocked: ctx.perf.markDuplicateFetchBlocked,
    markWorkbenchJumpDecision: ctx.perf.markWorkbenchJumpDecision,
    markFetched: ctx.pagePolicy.markFetched,
    markFetchLatency: ctx.perf.markFetchLatency,
    markAbort: ctx.perf.markAbort,
    markLoaderFallbackUsed: ctx.perf.markLoaderFallbackUsed,
    handleError,
    setLastRefreshReason: ctx.runtime.setLastRefreshReason,
    setCurrentLoaderKey: ctx.runtime.setCurrentLoaderKey,
    setCurrentPageSignature: ctx.runtime.setCurrentPageSignature,
    setCurrentSignatureHit: ctx.runtime.setCurrentSignatureHit,
    markDomainLoadSource: ctx.kernel.markDomainLoadSource,
    markRefreshSourceAnomaly: ctx.perf.markRefreshSourceAnomaly,
    setError: ctx.runtime.setError,
    setLoginError: ctx.runtime.setLoginError,
    locationPathname: ctx.location.pathname,
    leadImportJobId: ctx.runtime.leadImportJob?.id,
    markPollingActiveInstances: ctx.perf.markPollingActiveInstances,
  })
}
