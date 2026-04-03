import { useAppNavigationModel } from '../../../useAppNavigationModel'
import { PAGE_TO_PATH, PATH_TO_PAGE } from '../../runtime'
import { buildAppNavigationModelInput } from '../runtimeEngineBuilders'

export function useSetupNavigationModel(ctx) {
  return useAppNavigationModel(
    buildAppNavigationModelInput({
      t: ctx.t,
      canViewAudit: ctx.kernel.canViewAudit,
      canManagePermissions: ctx.kernel.canManagePermissions,
      canManageUsers: ctx.kernel.canManageUsers,
      canManageSalesAutomation: ctx.kernel.canManageSalesAutomation,
      activePage: ctx.runtime.activePage,
      setActivePage: ctx.runtime.setActivePage,
      locationPathname: ctx.location.pathname,
      authToken: ctx.auth?.token,
      navigate: ctx.navigate,
      pathToPage: PATH_TO_PAGE,
      pageToPath: PAGE_TO_PATH,
    }),
  )
}
