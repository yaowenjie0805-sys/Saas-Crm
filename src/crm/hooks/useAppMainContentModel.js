import { useAppRootBindings } from './useAppRootBindings'
import { useMainContentDomains } from './useMainContentDomains'

export function useAppMainContentModel({ base, domains, actions, capabilities, pageActions, crudActions }) {
  const bindings = useAppRootBindings({ base, domains, actions, capabilities, pageActions, crudActions })
  return useMainContentDomains(bindings)
}
