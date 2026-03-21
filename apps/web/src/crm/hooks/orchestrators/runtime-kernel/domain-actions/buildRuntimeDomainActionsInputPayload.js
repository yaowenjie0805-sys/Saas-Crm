import { mergeRuntimeInput } from '../shared/mergeRuntimeInput'

export function buildRuntimeDomainActionsInputPayload(input) {
  return mergeRuntimeInput(input)
}
