import { PAGE_CHUNK_PRELOADERS, PAGE_DOMAIN_PRELOADERS, PAGE_TO_PATH } from '../../../runtime'
import { ensureI18nNamespaces, getI18nNamespacesForPage } from '../../../../../i18n'
import {
  buildRuntimeCrudActionsInput,
  buildRuntimeAuthActionsInput,
  buildRuntimeDomainActionsHookPayload,
} from '../../runtimeEngineBuilders'

export function buildCrudActionsHookInput(ctx, data) {
  return buildRuntimeCrudActionsInput({
    runtime: ctx.runtime,
    auth: ctx.auth,
    lang: ctx.lang,
    t: ctx.t,
    canWrite: ctx.kernel.canWrite,
    canDeleteCustomer: ctx.kernel.canDeleteCustomer,
    canDeleteOpportunity: ctx.kernel.canDeleteOpportunity,
    handleError: data.handleError,
    validateCustomerForm: data.validateCustomerForm,
    validateContactForm: data.validateContactForm,
    validateContractForm: data.validateContractForm,
    validatePaymentForm: data.validatePaymentForm,
    refreshPage: data.refreshPage,
    activePage: ctx.runtime.activePage,
    markNavStart: ctx.perf.markNavStart,
    loadReasonRef: ctx.kernel.loadReasonRef,
    setActivePage: ctx.runtime.setActivePage,
    navigate: ctx.navigate,
    setCrudErrors: ctx.runtime.setCrudErrors,
    setCrudFieldErrors: ctx.runtime.setCrudFieldErrors,
  })
}

export function buildAuthActionsHookInput(ctx, data) {
  return buildRuntimeAuthActionsInput({
    runtime: ctx.runtime,
    lang: ctx.lang,
    t: ctx.t,
    validateLogin: data.validateLogin,
    validateRegister: data.validateRegister,
    validateSso: data.validateSso,
    saveAuth: data.saveAuth,
    handleLoginError: data.handleLoginError,
    logoutGuardRef: ctx.kernel.logoutGuardRef,
  })
}

export function buildDomainActionsHookInput(ctx, data) {
  return buildRuntimeDomainActionsHookPayload({
    runtime: ctx.runtime,
    auth: ctx.auth,
    lang: ctx.lang,
    role: ctx.kernel.role,
    t: ctx.t,
    canViewOpsMetrics: ctx.kernel.canViewOpsMetrics,
    canManagePermissions: ctx.kernel.canManagePermissions,
    canViewReports: ctx.kernel.canViewReports,
    handleError: data.handleError,
    refreshPage: data.refreshPage,
    loadLeadAssignmentRules: data.loadLeadAssignmentRules,
    loadAutomationRulesV1: data.loadAutomationRulesV1,
    loadPermissionConflicts: data.loadPermissionConflicts,
    loadAdminUsers: data.loadAdminUsers,
    loadApprovalTemplates: data.loadApprovalTemplates,
    loadApprovalStats: data.loadApprovalStats,
    loadApprovalInstances: data.loadApprovalInstances,
    loadApprovalTasks: data.loadApprovalTasks,
    loadApprovalDetail: data.loadApprovalDetail,
    loadNotificationJobs: data.loadNotificationJobs,
    loadTenants: data.loadTenants,
    loadApprovalTemplateVersions: data.loadApprovalTemplateVersions,
    hasInvalidAuditRange: data.hasInvalidAuditRange,
    pageSignature: data.pageSignature,
    canSkipFetch: ctx.pagePolicy.canSkipFetch,
    loadCustomers: data.loadCustomers,
    markFetched: ctx.pagePolicy.markFetched,
    loadLeads: data.loadLeads,
    loadOpportunities: data.loadOpportunities,
    loadTasks: data.loadTasks,
    loadWorkbenchToday: data.loadWorkbenchToday,
    loadReports: data.loadReports,
    markChunkPreloadHit: ctx.perf.markChunkPreloadHit,
    markNavStart: ctx.perf.markNavStart,
    activePage: ctx.runtime.activePage,
    loadReasonRef: ctx.kernel.loadReasonRef,
    setActivePage: ctx.runtime.setActivePage,
    navigate: ctx.navigate,
    pageToPath: PAGE_TO_PATH,
    pageChunkPreloaders: PAGE_CHUNK_PRELOADERS,
    pageDomainPreloaders: PAGE_DOMAIN_PRELOADERS,
    prefetchI18nNamespaces: (pageKey) =>
      ensureI18nNamespaces(ctx.lang, getI18nNamespacesForPage(pageKey)),
    markWorkbenchJumpDecision: ctx.perf.markWorkbenchJumpDecision,
    markWorkbenchActionResult: ctx.perf.markWorkbenchActionResult,
    workbenchJumpRef: ctx.kernel.workbenchJumpRef,
    markWorkbenchJumpMeta: ctx.kernel.markWorkbenchJumpMeta,
    commerceDomain: data.commerceDomain,
    markNavEnd: ctx.perf.markNavEnd,
    markRenderCommit: ctx.perf.markRenderCommit,
    markFirstInteractive: ctx.perf.markFirstInteractive,
    getMetrics: ctx.perf.getMetrics,
  })
}

export function buildSetupActionsResult(crudDomainActions, authActions, domainActions) {
  return {
    crudDomainActions,
    submitLogin: authActions.submitLogin,
    submitRegister: authActions.submitRegister,
    submitSsoLogin: authActions.submitSsoLogin,
    startOidcLogin: authActions.startOidcLogin,
    domainActions,
  }
}
