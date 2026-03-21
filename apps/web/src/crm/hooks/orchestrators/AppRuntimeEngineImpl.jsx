import RuntimeShellBranch from './runtime/RuntimeShellBranch'
import {
  useSetupRuntimeContext,
  useSetupNavigationModel,
  useSetupDataLoaders,
  useSetupActions,
  useBuildShellViewModel,
} from './runtime-kernel/useAppRuntimeEnginePhases'
import '../../../App.css'

function AppRuntimeEngine() {
  // Phase 1: setupRuntimeContext (state/routing/kernel)
  const ctx = useSetupRuntimeContext()

  // Phase 2: setupNavigationModel
  const navModel = useSetupNavigationModel(ctx)

  // Phase 3: setupDataLoaders (domain/page/loaders)
  const data = useSetupDataLoaders(ctx)

  // Phase 4: setupActions (auth/crud/domain actions)
  const actions = useSetupActions(ctx, data)

  // Phase 5: buildShellViewModel (view model + shell props)
  const shellProps = useBuildShellViewModel(ctx, data, actions, navModel)

  return (
    <RuntimeShellBranch
      hasAuthToken={ctx.kernel.hasAuthToken}
      isAuthRoute={ctx.isAuthRoute}
      sessionBootstrapping={ctx.runtime.sessionBootstrapping}
      apiContext={ctx.apiContext}
      authShellProps={shellProps.authShellProps}
      appShellProps={shellProps.appShellProps}
    />
  )
}

export default AppRuntimeEngine