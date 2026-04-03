import { CUSTOMER360_MODULE_KEYS, Customer360View } from './customer360'
import BatchResultModal from '../../BatchResultModal'
import {
  CustomerFormBar,
  CustomerFilterBar,
  CustomerBatchToolbar,
  CustomersTableSection,
  CustomerDetailDrawerSection,
} from './sections'

export function CustomersFullDetailSection({
  t,
  detail,
  customer360ViewModel,
  timelineLoading,
  detailLoading,
  detailModuleMeta,
  closeFullDetail,
  quickCreateFollowUp,
  quickCreateTask,
  executeCustomer360Action,
  navigateTo,
  openNeighbor,
  refreshCustomer360Modules,
  markCustomer360ActionResult,
}) {
  return (
    <section className="panel">
      <div className="panel-head"><h2>{t('customer360FullPage')}</h2></div>
      <Customer360View
        t={t}
        customer={detail}
        vm={customer360ViewModel}
        loading={timelineLoading || detailLoading}
        moduleMeta={detailModuleMeta}
        mode="page"
        onBackToList={closeFullDetail}
        onOpenFullDetail={() => null}
        onQuickCreateFollowUp={() => {
          if (!detail || !quickCreateFollowUp) return
          quickCreateFollowUp(detail)
          executeCustomer360Action('followup')
        }}
        onQuickCreateTask={() => {
          if (!detail || !quickCreateTask) return
          quickCreateTask(detail)
          executeCustomer360Action('task')
        }}
        onQuickCreateQuote={() => {
          if (!detail) return
          navigateTo('quotes', { customerId: detail.id, owner: detail.owner || '', page: 1 })
          executeCustomer360Action('quote')
        }}
        onQuickViewOrders={() => detail && navigateTo('orders', { customerId: detail.id, owner: detail.owner || '', page: 1 })}
        onQuickUrgeApproval={() => {
          if (!detail) return
          navigateTo('approvals', { status: 'PENDING', customerId: detail.id, page: 1 })
          executeCustomer360Action('urgeApproval')
        }}
        onNavigatePrev={() => openNeighbor(-1)}
        onNavigateNext={() => openNeighbor(1)}
        onNavigateTarget={navigateTo}
        onRefreshModules={refreshCustomer360Modules}
        onRefresh={() => {
          if (!detail) return
          refreshCustomer360Modules(CUSTOMER360_MODULE_KEYS)
          markCustomer360ActionResult?.(true)
        }}
      />
    </section>
  )
}

export function CustomersListSection(props) {
  const {
    t,
    canWrite,
    customerForm,
    setCustomerForm,
    saveCustomer,
    formError,
    fieldErrors,
    loading,
    customerQDraft,
    setCustomerQDraft,
    customerStatusDraft,
    setCustomerStatusDraft,
    applyFilters,
    resetFilters,
    onRefresh,
    selectedCount,
    selectPage,
    clearSelection,
    batchOwner,
    setBatchOwner,
    batchStatus,
    setBatchStatus,
    batchAssign,
    batchChangeStatus,
    canDeleteCustomer,
    batchDelete,
    batchSummary,
    setBatchModalOpen,
    batchMessage,
    allChecked,
    toggleAll,
    setSortBy,
    rows,
    selectedIds,
    toggleOne,
    openDetail,
    editCustomer,
    removeCustomer,
    page,
    totalPages,
    pagination,
    onPageChange,
    onSizeChange,
    detail,
    closeDrawer,
    openFullDetail,
    quickCreateFollowUp,
    quickCreateTask,
    customer360ViewModel,
    timelineLoading,
    detailLoading,
    detailModuleMeta,
    executeCustomer360Action,
    navigateTo,
    openNeighbor,
    refreshCustomer360Modules,
    clearSummary,
  } = props

  return (
    <section className="panel" data-testid="customers-page">
      <div className="panel-head"><h2 data-testid="customers-heading">{t('customers')}</h2></div>
      <CustomerFormBar
        t={t}
        canWrite={canWrite}
        customerForm={customerForm}
        setCustomerForm={setCustomerForm}
        saveCustomer={saveCustomer}
        fieldErrors={fieldErrors}
        formError={formError}
      />
      <CustomerFilterBar
        t={t}
        customerQDraft={customerQDraft}
        setCustomerQDraft={setCustomerQDraft}
        customerStatusDraft={customerStatusDraft}
        setCustomerStatusDraft={setCustomerStatusDraft}
        applyFilters={applyFilters}
        resetFilters={resetFilters}
        onRefresh={onRefresh}
      />
      <CustomerBatchToolbar
        t={t}
        selectedCount={selectedCount}
        selectPage={selectPage}
        clearSelection={clearSelection}
        batchOwner={batchOwner}
        setBatchOwner={setBatchOwner}
        batchStatus={batchStatus}
        setBatchStatus={setBatchStatus}
        batchAssign={batchAssign}
        batchChangeStatus={batchChangeStatus}
        canWrite={canWrite}
        canDeleteCustomer={canDeleteCustomer}
        batchDelete={batchDelete}
        batchSummary={batchSummary}
        setBatchModalOpen={setBatchModalOpen}
        batchMessage={batchMessage}
      />
      <CustomersTableSection
        t={t}
        allChecked={allChecked}
        toggleAll={toggleAll}
        setSortBy={setSortBy}
        loading={loading}
        rows={rows}
        selectedIds={selectedIds}
        toggleOne={toggleOne}
        openDetail={openDetail}
        editCustomer={editCustomer}
        canDeleteCustomer={canDeleteCustomer}
        removeCustomer={removeCustomer}
        page={page}
        totalPages={totalPages}
        pagination={pagination}
        onPageChange={onPageChange}
        onSizeChange={onSizeChange}
      />
      <CustomerDetailDrawerSection
        t={t}
        detail={detail}
        closeDrawer={closeDrawer}
        openFullDetail={openFullDetail}
        quickCreateFollowUp={quickCreateFollowUp}
        quickCreateTask={quickCreateTask}
        customer360ViewModel={customer360ViewModel}
        timelineLoading={timelineLoading}
        detailLoading={detailLoading}
        detailModuleMeta={detailModuleMeta}
        executeCustomer360Action={executeCustomer360Action}
        navigateTo={navigateTo}
        openNeighbor={openNeighbor}
        refreshCustomer360Modules={refreshCustomer360Modules}
      />
      <BatchResultModal t={t} open={props.batchModalOpen} summary={batchSummary} onClose={() => { props.setBatchModalOpen(false); clearSummary() }} />
    </section>
  )
}
