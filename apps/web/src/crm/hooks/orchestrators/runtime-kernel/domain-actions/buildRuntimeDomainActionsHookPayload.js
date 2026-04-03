import { buildRuntimeDomainActionsHookInput } from '../../runtime'
import { buildRuntimeDomainActionsInputPayload } from './buildRuntimeDomainActionsInputPayload'

export function buildRuntimeDomainActionsHookPayload(input) {
  return buildRuntimeDomainActionsHookInput(buildRuntimeDomainActionsInputPayload(input))
}
