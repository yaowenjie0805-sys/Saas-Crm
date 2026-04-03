import { useRuntimePageModelSync } from '../../runtime'
import { buildRuntimeViewModels } from '../runtimeEngineBuilders'
import { buildShellViewModelHookInput } from './helpers'

export function useBuildShellViewModel(ctx, data, actions, navModel) {
  const { pageModelCtx, shellProps } = buildRuntimeViewModels(
    buildShellViewModelHookInput(ctx, data, actions, navModel),
  )

  useRuntimePageModelSync({ ctx: pageModelCtx })
  return shellProps
}
