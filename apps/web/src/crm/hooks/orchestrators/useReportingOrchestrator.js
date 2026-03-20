import { useEffect } from 'react'
import { useAppStore } from '../../store/appStore'
import { useReportingMapper } from '../mainContentMappers/useReportingMapper'

export function useReportingOrchestrator() {
  const setReportingDomainState = useAppStore((state) => state.setReportingDomainState)
  const setReportingModels = (models) => setReportingDomainState(models || {})
  return { setReportingModels }
}

export function useSyncReportingPageModels(params) {
  const { setReportingModels } = useReportingOrchestrator()
  const {
    mainTasks,
    mainReportDesigner,
    mainAudit,
  } = useReportingMapper(params)
  useEffect(() => {
    setReportingModels({ audit: mainAudit })
  }, [setReportingModels, mainAudit])
  return {
    mainTasks,
    mainReportDesigner,
  }
}

export function buildReportingPageModelInputs(ctx) {
  return ctx?.reportingModel || {}
}
