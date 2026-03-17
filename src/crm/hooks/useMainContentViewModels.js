import { useCallback } from 'react'
import { useMainBaseMapper } from './mainContentMappers/useMainBaseMapper'
import { useCustomerMappers } from './mainContentMappers/useCustomerMappers'
import { useCommerceMapper } from './mainContentMappers/useCommerceMapper'
import { useGovernanceMapper } from './mainContentMappers/useGovernanceMapper'
import { useApprovalMapper } from './mainContentMappers/useApprovalMapper'
import { useReportingMapper } from './mainContentMappers/useReportingMapper'

function normalizeParams(params) {
  if (params?.base) {
    return {
      ...(params.base || {}),
      ...(params.actions || {}),
      ...(params.pageActions || {}),
      ...(params.crudActions || {}),
      ...(params.capabilities || {}),
      ...(params.domains?.customer || {}),
      ...(params.domains?.commerce || {}),
      ...(params.domains?.governance || {}),
      ...(params.domains?.approval || {}),
      ...(params.domains?.reporting || {}),
      ...(params.domains?.workbench || {}),
    }
  }
  return params || {}
}

export function useMainContentViewModels(params) {
  const normalized = normalizeParams(params)
  const { refreshPage } = normalized

  const refreshLeads = useCallback(() => refreshPage('leads', 'panel_action'), [refreshPage])
  const refreshCustomers = useCallback(() => refreshPage('customers', 'panel_action'), [refreshPage])
  const refreshPipeline = useCallback(() => refreshPage('pipeline', 'panel_action'), [refreshPage])
  const refreshFollowUps = useCallback(() => refreshPage('followUps', 'panel_action'), [refreshPage])
  const refreshContacts = useCallback(() => refreshPage('contacts', 'panel_action'), [refreshPage])
  const refreshContracts = useCallback(() => refreshPage('contracts', 'panel_action'), [refreshPage])
  const refreshPayments = useCallback(() => refreshPage('payments', 'panel_action'), [refreshPage])

  const mainBaseMapper = useMainBaseMapper(normalized)
  const customerMappers = useCustomerMappers({
    ...normalized,
    refreshLeads,
    refreshCustomers,
    refreshPipeline,
    refreshFollowUps,
    refreshContacts,
  })
  const commerceMapper = useCommerceMapper({
    ...normalized,
    refreshContracts,
    refreshPayments,
  })
  const governanceMapper = useGovernanceMapper(normalized)
  const approvalMapper = useApprovalMapper(normalized)
  const reportingMapper = useReportingMapper(normalized)

  return {
    ...mainBaseMapper,
    ...governanceMapper,
    ...customerMappers,
    ...commerceMapper,
    ...reportingMapper,
    ...approvalMapper,
  }
}
