export function buildRuntimeLeadImportModel(runtime) {
  return {
    mainLeads: {
      leadImportJobs: runtime.leadImportJobs,
      leadImportExportJobs: runtime.leadImportExportJobs,
    },
  }
}

export function buildRuntimePerfModel(runtime) {
  return {
    perfMetrics: runtime.perfMetrics,
    currentLoaderKey: runtime.currentLoaderKey,
    lastRefreshReason: runtime.lastRefreshReason,
    currentPageSignature: runtime.currentPageSignature,
    currentSignatureHit: runtime.currentSignatureHit,
    recentWorkbenchJump: runtime.recentWorkbenchJump,
    domainLoadSource: runtime.domainLoadSource,
    lastPerfSnapshotAt: runtime.lastPerfSnapshotAt,
  }
}
