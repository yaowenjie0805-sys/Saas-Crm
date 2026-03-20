import { useMemo } from 'react'
import { useRuntimeSectionFields } from './useRuntimeSectionFields'

export function useLeadImportRuntimeState({ readPageSize }) {
  const defaults = useMemo(() => ({
    leadImportJob: null,
    leadImportJobs: [],
    leadImportStatusFilter: 'ALL',
    leadImportPage: 1,
    leadImportTotalPages: 1,
    leadImportSize: () => readPageSize('crm_page_size_lead_import_jobs', 10),
    leadImportFailedRows: [],
    leadImportMetrics: null,
    leadImportExportJobs: [],
    leadImportExportStatusFilter: 'ALL',
    leadImportExportPage: 1,
    leadImportExportTotalPages: 1,
    leadImportExportSize: () => readPageSize('crm_page_size_lead_import_export_jobs', 10),
  }), [readPageSize])

  return useRuntimeSectionFields('leadImportDomain', 'ui', defaults)
}
