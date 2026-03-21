import { useRuntimeDomainActions } from '../../runtime/useRuntimeDomainActions'
import { useRuntimeAuthActions } from '../../runtime/useRuntimeAuthActions'
import { useRuntimeCrudDomainActions } from '../../runtime/useRuntimeCrudDomainActions'
import {
  buildCrudActionsHookInput,
  buildAuthActionsHookInput,
  buildDomainActionsHookInput,
  buildSetupActionsResult,
} from './helpers/setupActionsBuilders'

export function useSetupActions(ctx, data) {
  const crudDomainActions = useRuntimeCrudDomainActions(buildCrudActionsHookInput(ctx, data))

  const authActions = useRuntimeAuthActions(buildAuthActionsHookInput(ctx, data))
  const domainActions = useRuntimeDomainActions(buildDomainActionsHookInput(ctx, data))
  return buildSetupActionsResult(crudDomainActions, authActions, domainActions)
}
