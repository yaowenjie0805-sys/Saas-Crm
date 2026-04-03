import {
  useRuntimeAuthActions,
  useRuntimeCrudDomainActions,
  useRuntimeDomainActions,
} from '../../runtime'
import {
  buildCrudActionsHookInput,
  buildAuthActionsHookInput,
  buildDomainActionsHookInput,
  buildSetupActionsResult,
} from './helpers'

export function useSetupActions(ctx, data) {
  const crudDomainActions = useRuntimeCrudDomainActions(buildCrudActionsHookInput(ctx, data))

  const authActions = useRuntimeAuthActions(buildAuthActionsHookInput(ctx, data))
  const domainActions = useRuntimeDomainActions(buildDomainActionsHookInput(ctx, data))
  return buildSetupActionsResult(crudDomainActions, authActions, domainActions)
}
