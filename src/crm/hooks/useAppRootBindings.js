import { useMemo } from 'react'
import { useAppDomainBindings } from './useAppDomainBindings'

export function useAppRootBindings({ base, domains, actions, capabilities, pageActions, crudActions }) {
  const normalizedInput = useMemo(() => ({
    base,
    domains,
    actions,
    capabilities,
    pageActions,
    crudActions,
  }), [base, domains, actions, capabilities, pageActions, crudActions])

  return useAppDomainBindings(normalizedInput)
}
