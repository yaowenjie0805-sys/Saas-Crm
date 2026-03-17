import { useEffect, useMemo } from 'react'

const NAV_BLUEPRINT = [
  { key: 'dashboard', labelKey: 'dashboard', groupKey: 'businessGroup' },
  { key: 'leads', labelKey: 'leads', groupKey: 'businessGroup' },
  { key: 'products', labelKey: 'products', groupKey: 'businessGroup' },
  { key: 'priceBooks', labelKey: 'priceBooks', groupKey: 'businessGroup' },
  { key: 'quotes', labelKey: 'quotes', groupKey: 'businessGroup' },
  { key: 'orders', labelKey: 'orders', groupKey: 'businessGroup' },
  { key: 'customers', labelKey: 'customers', groupKey: 'businessGroup' },
  { key: 'contacts', labelKey: 'contacts', groupKey: 'businessGroup' },
  { key: 'pipeline', labelKey: 'pipeline', groupKey: 'businessGroup' },
  { key: 'contracts', labelKey: 'contracts', groupKey: 'businessGroup' },
  { key: 'payments', labelKey: 'payments', groupKey: 'businessGroup' },
  { key: 'followUps', labelKey: 'followUps', groupKey: 'businessGroup' },
  { key: 'tasks', labelKey: 'tasks', groupKey: 'businessGroup' },
  { key: 'approvals', labelKey: 'approvals', groupKey: 'businessGroup' },
  { key: 'reports', labelKey: 'reports', groupKey: 'businessGroup' },
  { key: 'reportDesigner', labelKey: 'reportDesigner', groupKey: 'businessGroup' },
]

export function useAppNavigationModel({
  t,
  permissions,
  activePage,
  setActivePage,
  locationPathname,
  authToken,
  navigate,
  pathToPage,
  pageToPath,
}) {
  const navItems = useMemo(() => {
    const items = NAV_BLUEPRINT.map((item) => ({
      key: item.key,
      label: t(item.labelKey),
      group: t(item.groupKey),
    }))
    const governanceGroup = t('governanceGroup')
    if (permissions.canViewAudit) items.push({ key: 'audit', label: t('audit'), group: governanceGroup })
    if (permissions.canManagePermissions) items.push({ key: 'permissions', label: t('permissions'), group: governanceGroup })
    if (permissions.canManageUsers) items.push({ key: 'usersAdmin', label: t('usersAdmin'), group: governanceGroup })
    if (permissions.canManageSalesAutomation) items.push({ key: 'salesAutomation', label: t('automationRules'), group: governanceGroup })
    if (permissions.canManageUsers) items.push({ key: 'adminTenants', label: t('tenantsAdmin'), group: governanceGroup })
    return items
  }, [permissions.canManagePermissions, permissions.canManageSalesAutomation, permissions.canManageUsers, permissions.canViewAudit, t])

  useEffect(() => {
    if (!navItems.some((it) => it.key === activePage)) {
      setActivePage(navItems[0]?.key || 'dashboard')
    }
  }, [activePage, navItems, setActivePage])

  useEffect(() => {
    const routePage = pathToPage[locationPathname]
    if (routePage && navItems.some((it) => it.key === routePage)) {
      setActivePage(routePage)
      return
    }
    if (!routePage && authToken && locationPathname !== '/activate') {
      navigate(pageToPath.dashboard, { replace: true })
    }
  }, [authToken, locationPathname, navItems, navigate, pageToPath.dashboard, pathToPage, setActivePage])

  const currentPageLabel = useMemo(
    () => navItems.find((item) => item.key === activePage)?.label || t('salesOverview'),
    [activePage, navItems, t],
  )

  const navGroups = useMemo(() => navItems.reduce((acc, item) => {
    const key = item.group || 'Default'
    if (!acc[key]) acc[key] = []
    acc[key].push(item)
    return acc
  }, {}), [navItems])

  return {
    navItems,
    navGroups,
    currentPageLabel,
  }
}
