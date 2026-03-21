function buildRuntimePageModelBaseSection({
  currentPageLabel,
  lang,
  setLang,
  refreshPage,
  t,
  canWrite,
  role,
  canViewReports,
  canViewAudit,
  canManagePermissions,
  canManageUsers,
  canManageSalesAutomation,
  canDeleteCustomer,
  canDeleteOpportunity,
  navigate,
  performLogout,
  markCustomer360ActionResult,
  markCustomer360ModuleRefreshLatency,
  markCustomer360JumpHit,
  markCustomer360ModuleCacheHit,
  markCustomer360PrefetchHit,
  markCustomer360PrefetchAbort,
  markCustomer360PrefetchModules,
}) {
  return {
    currentPageLabel,
    lang,
    setLang,
    refreshPage,
    t,
    canWrite,
    role,
    canViewReports,
    canViewAudit,
    canManagePermissions,
    canManageUsers,
    canManageSalesAutomation,
    canDeleteCustomer,
    canDeleteOpportunity,
    navigate,
    performLogout,
    customer360Metrics: {
      markActionResult: markCustomer360ActionResult,
      markModuleRefreshLatency: markCustomer360ModuleRefreshLatency,
      markJumpHit: markCustomer360JumpHit,
      markModuleCacheHit: markCustomer360ModuleCacheHit,
      markPrefetchHit: markCustomer360PrefetchHit,
      markPrefetchAbort: markCustomer360PrefetchAbort,
      markPrefetchModules: markCustomer360PrefetchModules,
    },
  }
}

function buildRuntimePageModelReportingSection({
  hasInvalidAuditRange,
  loadAudit,
  loadExportJobs,
  loadDesignerTemplates,
  loadCustomerTimeline,
  loadOpportunityTimeline,
  loadReportExportJobs,
}) {
  return {
    hasInvalidAuditRange,
    loadAudit,
    loadExportJobs,
    loadDesignerTemplates,
    loadCustomerTimeline,
    loadOpportunityTimeline,
    loadReportExportJobs,
  }
}

function buildRuntimePageModelApprovalSection({
  loadApprovalDetail,
  loadApprovalTasks,
  loadApprovalTemplates,
  loadNotificationJobs,
}) {
  return {
    loadApprovalDetail,
    loadApprovalTasks,
    loadApprovalTemplates,
    loadNotificationJobs,
  }
}

function buildRuntimePageModelConfig({
  runtime,
  base,
  reporting,
  customer,
  commerceDomain,
  governance,
  approval,
}) {
  return {
    runtime,
    base: buildRuntimePageModelBaseSection(base),
    reporting: buildRuntimePageModelReportingSection(reporting),
    customer,
    commerce: { commerceDomain },
    governance,
    approval: buildRuntimePageModelApprovalSection(approval),
    leadImport: {},
    perf: {},
  }
}

export {
  buildRuntimePageModelConfig,
}
