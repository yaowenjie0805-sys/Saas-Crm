import { memo, Suspense, useCallback, useEffect, useMemo, useState } from 'react'
import { translateOwnerAlias, translateRole } from '../shared'
import CrmErrorBoundary from './CrmErrorBoundary'
import TopBar from './layout/TopBar'
import {
  useApprovalsPageModel,
  useAuditPageModel,
  useBasePageModel,
  useCommercePageModel,
  useContactsPageModel,
  useContractsPageModel,
  useCustomersPageModel,
  useFollowUpsPageModel,
  useLeadsPageModel,
  usePaymentsPageModel,
  usePermissionsPageModel,
  usePipelinePageModel,
  useReportDesignerPageModel,
  useSalesAutomationPageModel,
  useTasksPageModel,
  useTenantsPageModel,
  useUsersPageModel,
} from '../hooks/pageModels'
import { useGlobalSearchBridge } from '../hooks/useGlobalSearchBridge'
import MainContentPanels from './MainContentPanels'

function MainContent() {
  const [searchNotice, setSearchNotice] = useState('')
  const base = useBasePageModel()
  const permissions = usePermissionsPageModel()
  const users = useUsersPageModel()
  const salesAutomation = useSalesAutomationPageModel()
  const commerce = useCommercePageModel()
  const customers = useCustomersPageModel()
  const pipeline = usePipelinePageModel()
  const followUps = useFollowUpsPageModel()
  const contacts = useContactsPageModel()
  const contracts = useContractsPageModel()
  const payments = usePaymentsPageModel()
  const leads = useLeadsPageModel()
  const reportDesigner = useReportDesignerPageModel()
  const tasks = useTasksPageModel()
  const audit = useAuditPageModel()
  const approvals = useApprovalsPageModel()
  const tenants = useTenantsPageModel()
  const {
    currentPageLabel,
    lang,
    setLang,
    refreshPage,
    t: rawT,
    canWrite,
    role,
    error,
    loading,
    activePage,
    quoteOpportunityFilter,
    orderOpportunityFilter,
    quotePrefill,
    consumeQuotePrefill,
    auth,
    onLogout,
  } = base
  const t = typeof rawT === 'function' ? rawT : (key) => String(key || '')

  const accountName = translateOwnerAlias(t, auth?.displayName || auth?.username || t('defaultLabel'))
  const accountRole = translateRole(t, auth?.role || role)
  const accountLabel = `${accountName} | ${accountRole}`

  useEffect(() => {
    const onTenantConfigUpdated = (event) => {
      const changedTenantId = String(event?.detail?.tenantId || '').trim()
      const currentTenantId = String(auth?.tenantId || '').trim()
      if (!changedTenantId || !currentTenantId || changedTenantId !== currentTenantId) return
      if (!['dashboard', 'reports', 'adminTenants'].includes(activePage)) return
      // Keep report/dashboard market context aligned with latest tenant governance edits.
      const targetPage = activePage === 'adminTenants' ? 'adminTenants' : 'reports'
      refreshPage(targetPage, 'panel_action')
    }
    window.addEventListener('crm:tenant-config-updated', onTenantConfigUpdated)
    return () => window.removeEventListener('crm:tenant-config-updated', onTenantConfigUpdated)
  }, [activePage, auth?.tenantId, refreshPage])

  const refreshGovernance = useCallback(() => refreshPage(activePage, 'panel_action'), [activePage, refreshPage])
  const refreshApprovals = useCallback(() => refreshPage('approvals', 'panel_action'), [refreshPage])

  const pageSkeleton = (
    <section className="panel">
      <div className="loading">{t('loading')}</div>
    </section>
  )
  const apiContext = useMemo(() => ({ token: auth?.token || '', lang, tenantId: auth?.tenantId || '' }), [auth?.tenantId, auth?.token, lang])
  const backToDashboard = useCallback(() => {
    refreshPage('dashboard', 'panel_action')
    base.onWorkbenchNavigate?.('dashboard')
  }, [base, refreshPage])
  const onSearchSubmit = useGlobalSearchBridge({
    activePage,
    customers,
    leads,
    commerce,
    refreshPage,
    setNotice: setSearchNotice,
    t,
  })

  useEffect(() => {
    if (!searchNotice) return undefined
    const timer = window.setTimeout(() => setSearchNotice(''), 1800)
    return () => window.clearTimeout(timer)
  }, [searchNotice])

  return (
    <main className="content">
      <TopBar
        t={t}
        currentPageLabel={currentPageLabel}
        lang={lang}
        setLang={setLang}
        accountLabel={accountLabel}
        tenantId={auth?.tenantId || ''}
        onLogout={onLogout}
        onRefreshCurrentPage={() => refreshPage(activePage, 'topbar_refresh')}
        onSearchSubmit={onSearchSubmit}
      />

      {!canWrite && <div className="info-banner">{t('readOnly')}</div>}
      {searchNotice && <div className="info-banner">{searchNotice}</div>}
      {error && <div className="error-banner">{error}</div>}
      {loading && <div className="loading">{t('loading')}</div>}

      <CrmErrorBoundary
        resetKey={activePage}
        t={t}
        onRetry={() => refreshPage(activePage, 'panel_action')}
        onBackToDashboard={backToDashboard}
      >
        <div data-testid={`page-${activePage}`}>
        <Suspense fallback={pageSkeleton}>
        <MainContentPanels
          activePage={activePage}
          t={t}
          canWrite={canWrite}
          loading={loading}
          apiContext={apiContext}
          refreshPage={refreshPage}
          base={base}
          permissions={permissions}
          users={users}
          salesAutomation={salesAutomation}
          tenants={tenants}
          customers={customers}
          pipeline={pipeline}
          followUps={followUps}
          contacts={contacts}
          contracts={contracts}
          payments={payments}
          leads={leads}
          reportDesigner={reportDesigner}
          tasks={tasks}
          approvals={approvals}
          audit={audit}
          commerce={commerce}
          quoteOpportunityFilter={quoteOpportunityFilter}
          orderOpportunityFilter={orderOpportunityFilter}
          quotePrefill={quotePrefill}
          consumeQuotePrefill={consumeQuotePrefill}
          refreshGovernance={refreshGovernance}
          refreshApprovals={refreshApprovals}
          role={role}
        />
        </Suspense>
        </div>
      </CrmErrorBoundary>
    </main>
  )
}

export default memo(MainContent)
