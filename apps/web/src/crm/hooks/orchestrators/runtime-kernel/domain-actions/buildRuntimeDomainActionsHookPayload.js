import { buildRuntimeDomainActionsHookInput } from '../../runtime/buildRuntimeDomainActionsHookInput'
import { buildRuntimeDomainActionsInputPayload } from './buildRuntimeDomainActionsInputPayload'

export function buildRuntimeDomainActionsHookPayload(input) {
  return buildRuntimeDomainActionsHookInput(buildRuntimeDomainActionsInputPayload(input))
}
