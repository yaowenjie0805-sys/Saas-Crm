export function buildAppNavigationModelInput(input) {
  return {
    t: input.t,
    permissions: {
      canViewAudit: input.canViewAudit,
      canManagePermissions: input.canManagePermissions,
      canManageUsers: input.canManageUsers,
      canManageSalesAutomation: input.canManageSalesAutomation,
    },
    activePage: input.activePage,
    setActivePage: input.setActivePage,
    locationPathname: input.locationPathname,
    authToken: input.authToken,
    navigate: input.navigate,
    pathToPage: input.pathToPage,
    pageToPath: input.pageToPath,
  }
}
