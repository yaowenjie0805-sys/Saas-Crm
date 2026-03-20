import { useEffect, useMemo } from 'react'
import { useAppStore } from '../../store/appStore'

export function useLeadImportOrchestrator() {
  const setLeadImportDomainState = useAppStore((state) => state.setLeadImportDomainState)
  const setLeadImportModels = (models) => setLeadImportDomainState(models || {})
  return { setLeadImportModels }
}

export function useSyncLeadImportPageModels({ mainLeads }) {
  const { setLeadImportModels } = useLeadImportOrchestrator()
  const leadImportModels = useMemo(() => ({
    leadImportJobs: mainLeads?.leadImportJobs || [],
    leadImportExportJobs: mainLeads?.leadImportExportJobs || [],
  }), [mainLeads])
  useEffect(() => {
    setLeadImportModels(leadImportModels)
  }, [setLeadImportModels, leadImportModels])
}

export function buildLeadImportPageModelInputs(ctx) {
  return ctx?.leadImportModel || {}
}
