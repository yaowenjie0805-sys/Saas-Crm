import { useEffect } from 'react'
import { useAppStore } from '../../store/appStore'
import { useCustomerMappers } from '../mainContentMappers/useCustomerMappers'

export function useCustomerOrchestrator() {
  const setCustomerDomainState = useAppStore((state) => state.setCustomerDomainState)
  const setCustomerModels = (models) => setCustomerDomainState(models || {})
  return { setCustomerModels }
}

export function useSyncCustomerPageModels(params) {
  const { setCustomerModels } = useCustomerOrchestrator()
  const {
    mainLeads,
    mainCustomers,
    mainPipeline,
    mainFollowUps,
    mainContacts,
  } = useCustomerMappers(params)
  const mainTasks = params.mainTasks
  useEffect(() => {
    setCustomerModels({
      leads: mainLeads,
      customers: mainCustomers,
      pipeline: mainPipeline,
      followUps: mainFollowUps,
      contacts: mainContacts,
      tasks: mainTasks,
    })
  }, [setCustomerModels, mainLeads, mainCustomers, mainPipeline, mainFollowUps, mainContacts, mainTasks])
}

export function buildCustomerPageModelInputs(ctx) {
  const cross = ctx?.cross || {}
  return {
    ...(ctx?.customerModel || {}),
    mainTasks: cross.mainTasks,
  }
}
