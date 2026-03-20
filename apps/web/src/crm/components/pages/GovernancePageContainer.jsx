import { lazy, memo, Suspense, useCallback } from 'react'

const UsersGovernanceSection = lazy(() => import('./governance/UsersGovernanceSection'))
const SalesAutomationSection = lazy(() => import('./governance/SalesAutomationSection'))
const TenantsGovernanceSection = lazy(() => import('./governance/TenantsGovernanceSection'))

function GovernancePageContainer({
  activePage,
  t,
  onRefresh,
  canManageUsers,
  users,
  canManageSalesAutomation,
  salesAutomation,
  tenants,
}) {
  const refreshSelf = useCallback(() => onRefresh?.(), [onRefresh])

  if (!((activePage === 'usersAdmin' && canManageUsers) || (activePage === 'salesAutomation' && canManageSalesAutomation) || activePage === 'adminTenants')) return null

  if (activePage === 'usersAdmin' && canManageUsers) {
    return (
      <section className="panel">
        <Suspense fallback={<div className="loading">{t('loading')}</div>}>
          <UsersGovernanceSection t={t} users={users} onRefresh={refreshSelf} />
        </Suspense>
      </section>
    )
  }

  if (activePage === 'salesAutomation' && canManageSalesAutomation) {
    return (
      <section className="panel">
        <Suspense fallback={<div className="loading">{t('loading')}</div>}>
          <SalesAutomationSection t={t} salesAutomation={salesAutomation} onRefresh={refreshSelf} />
        </Suspense>
      </section>
    )
  }

  if (activePage === 'adminTenants') {
    return (
      <Suspense fallback={<div className="loading">{t('loading')}</div>}>
        <TenantsGovernanceSection t={t} tenants={tenants} onRefresh={refreshSelf} />
      </Suspense>
    )
  }

  return null
}

export default memo(GovernancePageContainer)
