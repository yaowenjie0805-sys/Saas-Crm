import { buildRuntimePageModelContext, buildRuntimeShellProps } from '../runtime'
import {
  buildRuntimePageModelContextPayload,
  buildRuntimeShellPropsPayload,
} from './view-model'

export function buildRuntimeViewModels(params) {
  const pageModelCtx = buildRuntimePageModelContext(buildRuntimePageModelContextPayload(params))
  const shellProps = buildRuntimeShellProps(buildRuntimeShellPropsPayload(params))
  return { pageModelCtx, shellProps }
}
