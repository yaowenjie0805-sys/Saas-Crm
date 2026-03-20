import { useCallback } from 'react'
import { useLeadImportActions } from '../../useLeadImportActions'
import { useAppPageActions } from '../../useAppPageActions'
import { useAppShellBindings } from '../../useAppShellBindings'

export function useRuntimeDomainActions({
  leadImport,
  pageActions,
  shellBindings,
  setQuotePrefill,
  refreshPage,
}) {
  const leadImportActions = useLeadImportActions(leadImport)
  const appPageActions = useAppPageActions(pageActions)
  const shellActions = useAppShellBindings(shellBindings)

  const consumeQuotePrefill = useCallback(() => setQuotePrefill(null), [setQuotePrefill])
  const refreshLeads = useCallback(() => refreshPage('leads', 'panel_action'), [refreshPage])
  const refreshCustomers = useCallback(() => refreshPage('customers', 'panel_action'), [refreshPage])
  const refreshPipeline = useCallback(() => refreshPage('pipeline', 'panel_action'), [refreshPage])
  const refreshFollowUps = useCallback(() => refreshPage('followUps', 'panel_action'), [refreshPage])
  const refreshContacts = useCallback(() => refreshPage('contacts', 'panel_action'), [refreshPage])
  const refreshContracts = useCallback(() => refreshPage('contracts', 'panel_action'), [refreshPage])
  const refreshPayments = useCallback(() => refreshPage('payments', 'panel_action'), [refreshPage])

  return {
    ...leadImportActions,
    ...appPageActions,
    ...shellActions,
    consumeQuotePrefill,
    refreshLeads,
    refreshCustomers,
    refreshPipeline,
    refreshFollowUps,
    refreshContacts,
    refreshContracts,
    refreshPayments,
  }
}
