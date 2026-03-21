import {
  buildReportingCtx,
  buildLeadImportCtx,
  buildPerfCtx,
  buildPaginationCtx,
  buildFormsCtx,
} from './runtime-state/buildRuntimeStateContextSections'

export function buildRuntimeStateContextsReportingMisc(runtime) {
  return {
    reportingCtx: buildReportingCtx(runtime),
    leadImportCtx: buildLeadImportCtx(runtime),
    perfCtx: buildPerfCtx(runtime),
    paginationCtx: buildPaginationCtx(runtime),
    formsCtx: buildFormsCtx(runtime),
  }
}
