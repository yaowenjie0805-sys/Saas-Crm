import { useEffect } from 'react'
import { useAppStore } from '../../store/appStore'
import { useGovernanceMapper } from '../mainContentMappers/useGovernanceMapper'

export function useGovernanceOrchestrator() {
  const setGovernanceDomainState = useAppStore((state) => state.setGovernanceDomainState)
  const setGovernanceModels = (models) => setGovernanceDomainState(models || {})
  return { setGovernanceModels }
}

export function useSyncGovernancePageModels(params) {
  const { setGovernanceModels } = useGovernanceOrchestrator()
  const {
    mainPermissions,
    mainUsers,
    mainSalesAutomation,
    mainTenants,
  } = useGovernanceMapper(params)
  useEffect(() => {
    setGovernanceModels({
      permissions: mainPermissions,
      users: mainUsers,
      salesAutomation: mainSalesAutomation,
      tenants: mainTenants,
    })
  }, [setGovernanceModels, mainPermissions, mainUsers, mainSalesAutomation, mainTenants])
}

export function buildGovernancePageModelInputs(ctx) {
  return ctx?.governanceModel || {}
}
