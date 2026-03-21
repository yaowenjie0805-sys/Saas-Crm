import { buildRuntimePageModelContext } from '../runtime/useRuntimePageModelContext'
import { buildRuntimeShellProps } from '../runtime/buildRuntimeShellProps'
import {
  buildRuntimePageModelContextPayload,
  buildRuntimeShellPropsPayload,
} from './view-model/buildRuntimeViewModelPayloads'

export function buildRuntimeViewModels(params) {
  const pageModelCtx = buildRuntimePageModelContext(buildRuntimePageModelContextPayload(params))
  const shellProps = buildRuntimeShellProps(buildRuntimeShellPropsPayload(params))
  return { pageModelCtx, shellProps }
}
