import { useMemo } from 'react'

export function useAppMainContentInputs(params) {
  const {
    base,
    domains,
    actions,
    pageActions,
    crudActions,
    capabilities,
  } = params

  return useMemo(() => ({
    base,
    domains,
    actions,
    pageActions,
    crudActions,
    capabilities,
  }), [base, domains, actions, pageActions, crudActions, capabilities])
}
