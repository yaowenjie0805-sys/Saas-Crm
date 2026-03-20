import { lazy, memo, Suspense, useCallback, useEffect, useMemo, useState } from 'react'
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
} from '../hooks/pageModels/useMainContentPageModels'
import { useGlobalSearchBridge } from '../hooks/useGlobalSearchBridge'

const DashboardPanel = lazy(() => import('./pages/DashboardPanel'))
const PermissionsPanel = lazy(() => import('./pages/PermissionsPanel'))
const AuditPanel = lazy(() => import('./pages/AuditPanel'))
const CustomersPanel = lazy(() => import('./pages/CustomersPanel'))
const PipelinePanel = lazy(() => import('./pages/PipelinePanel'))
const TasksPanel = lazy(() => import('./pages/TasksPanel'))
const FollowUpsPanel = lazy(() => import('./pages/FollowUpsPanel'))
const ContactsPanel = lazy(() => import('./pages/ContactsPanel'))
const ContractsPanel = lazy(() => import('./pages/ContractsPanel'))
const PaymentsPanel = lazy(() => import('./pages/PaymentsPanel'))
const LeadsPanel = lazy(() => import('./pages/LeadsPanel'))
const ReportDesignerPanel = lazy(() => import('./pages/ReportDesignerPanel'))
const ProductsPanel = lazy(() => import('./pages/ProductsPanel'))
const PriceBooksPanel = lazy(() => import('./pages/PriceBooksPanel'))
const QuotesPanel = lazy(() => import('./pages/QuotesPanel'))
const OrdersPanel = lazy(() => import('./pages/OrdersPanel'))
const ApprovalsPageContainer = lazy(() => import('./pages/ApprovalsPageContainer'))
const GovernancePageContainer = lazy(() => import('./pages/GovernancePageContainer'))

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
    stats,
    reports,
    workbenchToday,
    quoteOpportunityFilter,
    orderOpportunityFilter,
    quotePrefill,
    consumeQuotePrefill,
    auth,
    onLogout,
  } = base
  const t = typeof rawT === 'function' ? rawT : (key) => String(key || '')

  const {
    permissionRole,
    setPermissionRole,
    canManagePermissions,
    previewPermissionPack,
    pendingPack,
    commitPendingPack,
    rollbackPermissionRole,
    permissionPreview,
    permissionMatrix,
    changePermission,
    permissionConflicts,
  } = permissions

  const {
    canManageUsers,
  } = users

  const {
    canManageSalesAutomation,
  } = salesAutomation

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
      refreshPage('reports', 'panel_action')
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
        {(activePage === 'dashboard' || activePage === 'reports') && (
          <DashboardPanel
            activePage={activePage}
            refreshPage={refreshPage}
            stats={stats}
            reports={reports}
            workbenchToday={workbenchToday}
            onWorkbenchNavigate={base.onWorkbenchNavigate}
            quickCreateTask={base.quickCreateTask}
            trackWorkbenchEvent={base.trackWorkbenchEvent}
            t={t}
            canViewReports={base.canViewReports}
            auditFrom={base.auditFrom}
            auditTo={base.auditTo}
            auditRole={base.auditRole}
            reportOwner={base.reportOwner}
            setReportOwner={base.setReportOwner}
            reportDepartment={base.reportDepartment}
            setReportDepartment={base.setReportDepartment}
            reportTimezone={base.reportTimezone}
            setReportTimezone={base.setReportTimezone}
            reportCurrency={base.reportCurrency}
            setReportCurrency={base.setReportCurrency}
            exportReportCsv={base.exportReportCsv}
            reportExportJobs={base.reportExportJobs}
            reportExportStatusFilter={base.reportExportStatusFilter}
            setReportExportStatusFilter={base.setReportExportStatusFilter}
            reportExportJobsPage={base.reportExportJobsPage}
            setReportExportJobsPage={base.setReportExportJobsPage}
            reportExportJobsTotalPages={base.reportExportJobsTotalPages}
            reportExportJobsSize={base.reportExportJobsSize}
            setReportExportJobsSize={base.setReportExportJobsSize}
            autoRefreshReportJobs={base.autoRefreshReportJobs}
            setAutoRefreshReportJobs={base.setAutoRefreshReportJobs}
            loadReportExportJobs={base.loadReportExportJobs}
            retryReportExportJob={base.retryReportExportJob}
            downloadReportExportJob={base.downloadReportExportJob}
          />
        )}

        {activePage === 'permissions' && (
          <PermissionsPanel
            activePage={activePage}
            t={t}
            permissionRole={permissionRole}
            setPermissionRole={setPermissionRole}
            canManagePermissions={canManagePermissions}
            previewPermissionPack={previewPermissionPack}
            pendingPack={pendingPack}
            commitPendingPack={commitPendingPack}
            rollbackPermissionRole={rollbackPermissionRole}
            permissionPreview={permissionPreview}
            permissionMatrix={permissionMatrix}
            changePermission={changePermission}
            permissionConflicts={permissionConflicts}
          />
        )}

        {(activePage === 'usersAdmin' || activePage === 'salesAutomation' || activePage === 'adminTenants') && (
          <GovernancePageContainer
            activePage={activePage}
            t={t}
            onRefresh={refreshGovernance}
            canManageUsers={canManageUsers}
            users={users}
            canManageSalesAutomation={canManageSalesAutomation}
            salesAutomation={salesAutomation}
            tenants={tenants}
          />
        )}

        {activePage === 'customers' && <CustomersPanel activePage={activePage} t={t} canWrite={canWrite} loading={loading} apiContext={apiContext} {...customers} />}
        {activePage === 'leads' && <LeadsPanel activePage={activePage} t={t} canWrite={canWrite} loading={loading} {...leads} />}
        {activePage === 'products' && <ProductsPanel activePage={activePage} t={t} canWrite={canWrite} apiContext={apiContext} refreshPage={refreshPage} commerce={commerce} />}
        {activePage === 'priceBooks' && <PriceBooksPanel activePage={activePage} t={t} canWrite={canWrite} apiContext={apiContext} refreshPage={refreshPage} commerce={commerce} />}
        {activePage === 'quotes' && <QuotesPanel activePage={activePage} t={t} canWrite={canWrite} apiContext={apiContext} opportunityFilter={quoteOpportunityFilter} prefill={quotePrefill} onConsumePrefill={consumeQuotePrefill} refreshPage={refreshPage} commerce={commerce} />}
        {activePage === 'orders' && <OrdersPanel activePage={activePage} t={t} canWrite={canWrite} apiContext={apiContext} opportunityFilter={orderOpportunityFilter} refreshPage={refreshPage} commerce={commerce} />}
        {activePage === 'pipeline' && <PipelinePanel activePage={activePage} t={t} canWrite={canWrite} loading={loading} apiContext={apiContext} {...pipeline} />}
        {activePage === 'followUps' && <FollowUpsPanel activePage={activePage} t={t} canWrite={canWrite} {...followUps} />}
        {activePage === 'contacts' && <ContactsPanel activePage={activePage} t={t} canWrite={canWrite} loading={loading} {...contacts} />}
        {activePage === 'contracts' && <ContractsPanel activePage={activePage} t={t} canWrite={canWrite} loading={loading} apiContext={apiContext} canDeleteCustomer={customers.canDeleteCustomer} {...contracts} />}
        {activePage === 'payments' && <PaymentsPanel activePage={activePage} t={t} canWrite={canWrite} loading={loading} apiContext={apiContext} canDeleteCustomer={customers.canDeleteCustomer} {...payments} />}
        {activePage === 'reportDesigner' && <ReportDesignerPanel activePage={activePage} t={t} canDesign={['ADMIN', 'MANAGER', 'ANALYST'].includes(role)} {...reportDesigner} />}
        {activePage === 'tasks' && <TasksPanel activePage={activePage} t={t} canWrite={canWrite} {...tasks} />}
        {activePage === 'approvals' && <ApprovalsPageContainer activePage={activePage} t={t} approvals={approvals} refreshApprovals={refreshApprovals} />}
        {activePage === 'audit' && <AuditPanel activePage={activePage} t={t} refreshPage={refreshPage} {...audit} />}
        </Suspense>
        </div>
      </CrmErrorBoundary>
    </main>
  )
}

export default memo(MainContent)
