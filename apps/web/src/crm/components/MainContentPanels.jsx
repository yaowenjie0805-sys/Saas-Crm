import { lazy, memo } from 'react'

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

function MainContentPanels({
  activePage,
  t,
  canWrite,
  loading,
  apiContext,
  refreshPage,
  base,
  permissions,
  users,
  salesAutomation,
  tenants,
  customers,
  pipeline,
  followUps,
  contacts,
  contracts,
  payments,
  leads,
  reportDesigner,
  tasks,
  approvals,
  audit,
  commerce,
  quoteOpportunityFilter,
  orderOpportunityFilter,
  quotePrefill,
  consumeQuotePrefill,
  refreshGovernance,
  refreshApprovals,
  role,
}) {
  return (
    <>
      {(activePage === 'dashboard' || activePage === 'reports') && (
        <DashboardPanel
          activePage={activePage}
          refreshPage={refreshPage}
          stats={base.stats}
          reports={base.reports}
          workbenchToday={base.workbenchToday}
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
          permissionRole={permissions.permissionRole}
          setPermissionRole={permissions.setPermissionRole}
          canManagePermissions={permissions.canManagePermissions}
          previewPermissionPack={permissions.previewPermissionPack}
          pendingPack={permissions.pendingPack}
          commitPendingPack={permissions.commitPendingPack}
          rollbackPermissionRole={permissions.rollbackPermissionRole}
          permissionPreview={permissions.permissionPreview}
          permissionMatrix={permissions.permissionMatrix}
          changePermission={permissions.changePermission}
          permissionConflicts={permissions.permissionConflicts}
        />
      )}

      {(activePage === 'usersAdmin' || activePage === 'salesAutomation' || activePage === 'adminTenants') && (
        <GovernancePageContainer
          activePage={activePage}
          t={t}
          onRefresh={refreshGovernance}
          canManageUsers={users.canManageUsers}
          users={users}
          canManageSalesAutomation={salesAutomation.canManageSalesAutomation}
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
    </>
  )
}

export default memo(MainContentPanels)
