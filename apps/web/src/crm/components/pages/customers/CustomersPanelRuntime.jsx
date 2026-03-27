import { memo, useCallback, useEffect, useRef } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import useCustomer360Data from './customer360/useCustomer360Data'
import { CustomersFullDetailSection, CustomersListSection } from './CustomersPanelSections'
import { useCustomerBatchOperations, useCustomersDetailNavigation, useCustomersListModel } from './hooks'

function CustomersPanel({
  activePage,
  t,
  canWrite,
  customerForm,
  setCustomerForm,
  saveCustomer,
  formError,
  fieldErrors,
  customers,
  editCustomer,
  canDeleteCustomer,
  removeCustomer,
  loading,
  customerQ,
  setCustomerQ,
  customerStatus,
  setCustomerStatus,
  pagination,
  onPageChange,
  onSizeChange,
  onRefresh,
  loadTimeline,
  timeline,
  quickCreateFollowUp,
  quickCreateTask,
  onWorkbenchNavigate,
  buildWorkbenchFilterSignature,
  customer360Metrics,
  apiContext,
}) {
  const location = useLocation()
  const navigate = useNavigate()
  const token = apiContext?.token
  const lang = apiContext?.lang || 'en'
  const markCustomer360ActionResult = customer360Metrics?.markActionResult

  const listModel = useCustomersListModel({
    customers,
    t,
    pagination,
    customerQ,
    setCustomerQ,
    customerStatus,
    setCustomerStatus,
    onPageChange,
  })
  const {
    rows,
    rowMap,
    byId,
    rowIndexById,
    page,
    totalPages,
    selection,
    setSortBy,
    customerQDraft,
    setCustomerQDraft,
    customerStatusDraft,
    setCustomerStatusDraft,
    applyFilters,
    resetFilters,
  } = listModel
  const { selectedIds, selectedCount, allChecked, clearSelection, selectPage, toggleAll, toggleOne } = selection

  const detailNav = useCustomersDetailNavigation({
    activePage,
    t,
    location,
    navigate,
    rowMap,
    rowIndexById,
    rows,
  })
  const {
    detail,
    detailMode,
    detailSource,
    openDetailState,
    openFullDetailUrl,
    resolveNeighbor,
    closeFullDetail: closeFullDetailNav,
    closeDrawer: closeDrawerNav,
  } = detailNav

  const {
    timelineLoading,
    setTimelineLoading,
    detailLoading,
    detailModuleMeta,
    loadCustomer360,
    refreshCustomer360Modules,
    executeCustomer360Action,
    scheduleNeighborPrefetch,
    abortPrefetchRequests,
    customer360ViewModel,
    navigateTo,
  } = useCustomer360Data({
    t,
    token,
    lang,
    activePage,
    detailMode,
    detail,
    timeline,
    rows,
    rowIndexById,
    loadTimeline,
    onWorkbenchNavigate,
    buildWorkbenchFilterSignature,
    customer360Metrics,
  })

  const {
    batchOwner,
    setBatchOwner,
    batchStatus,
    setBatchStatus,
    batchModalOpen,
    setBatchModalOpen,
    batchSummary,
    batchMessage,
    batchDelete,
    batchAssign,
    batchChangeStatus,
    clearSummary,
  } = useCustomerBatchOperations({
    t,
    canDeleteCustomer,
    selectedIds,
    clearSelection,
    onRefresh,
    token,
    lang,
    byId,
  })

  const urlDetailLoadKeyRef = useRef('')

  const loadDetailData = useCallback(async (row, options = {}) => {
    if (!row?.id) return
    setTimelineLoading(true)
    try {
      await Promise.all([
        loadTimeline ? loadTimeline(row.id) : Promise.resolve(),
        loadCustomer360(row, {
          resetModules: options.resetModules ?? true,
          force: options.force ?? false,
          source: options.source || 'open',
        }),
      ])
    } catch {
      // global error banner handles message
    } finally {
      setTimelineLoading(false)
    }
    if (options.prefetchNeighbor) scheduleNeighborPrefetch(row)
  }, [loadCustomer360, loadTimeline, scheduleNeighborPrefetch, setTimelineLoading])

  const openDetail = useCallback(async (row, mode = 'drawer') => {
    openDetailState(row, mode)
    await loadDetailData(row, { resetModules: true, force: false, source: 'open', prefetchNeighbor: mode === 'drawer' })
  }, [loadDetailData, openDetailState])

  const openFullDetail = useCallback(async (row) => {
    if (!row?.id) return
    openFullDetailUrl(row)
    await openDetail(row, 'page')
  }, [openDetail, openFullDetailUrl])

  const openNeighbor = useCallback(async (step) => {
    const next = resolveNeighbor(step)
    if (!next) return
    await openDetail(next, detailMode)
  }, [detailMode, openDetail, resolveNeighbor])

  const closeFullDetail = useCallback(() => {
    closeFullDetailNav()
    abortPrefetchRequests()
  }, [abortPrefetchRequests, closeFullDetailNav])

  const closeDrawer = useCallback(() => {
    closeDrawerNav()
    abortPrefetchRequests()
  }, [abortPrefetchRequests, closeDrawerNav])

  useEffect(() => {
    if (activePage !== 'customers' || detailSource !== 'url' || !detail?.id) {
      urlDetailLoadKeyRef.current = ''
      return
    }
    const nextKey = `${detailSource}:${detail.id}`
    if (urlDetailLoadKeyRef.current === nextKey) return
    urlDetailLoadKeyRef.current = nextKey
    loadDetailData(detail, { resetModules: true, force: false, source: 'open', prefetchNeighbor: false })
  }, [activePage, detail, detailSource, loadDetailData])

  const isFullDetail = activePage === 'customers' && detailMode === 'page' && !!detail
  if (activePage !== 'customers') return null
  if (isFullDetail) {
    return (
      <CustomersFullDetailSection
        t={t}
        detail={detail}
        customer360ViewModel={customer360ViewModel}
        timelineLoading={timelineLoading}
        detailLoading={detailLoading}
        detailModuleMeta={detailModuleMeta}
        closeFullDetail={closeFullDetail}
        quickCreateFollowUp={quickCreateFollowUp}
        quickCreateTask={quickCreateTask}
        executeCustomer360Action={executeCustomer360Action}
        navigateTo={navigateTo}
        openNeighbor={openNeighbor}
        refreshCustomer360Modules={refreshCustomer360Modules}
        markCustomer360ActionResult={markCustomer360ActionResult}
      />
    )
  }

  return (
    <CustomersListSection
      t={t}
      canWrite={canWrite}
      customerForm={customerForm}
      setCustomerForm={setCustomerForm}
      saveCustomer={saveCustomer}
      formError={formError}
      fieldErrors={fieldErrors}
      loading={loading}
      customerQDraft={customerQDraft}
      setCustomerQDraft={setCustomerQDraft}
      customerStatusDraft={customerStatusDraft}
      setCustomerStatusDraft={setCustomerStatusDraft}
      applyFilters={applyFilters}
      resetFilters={resetFilters}
      onRefresh={onRefresh}
      selectedCount={selectedCount}
      selectPage={selectPage}
      clearSelection={clearSelection}
      batchOwner={batchOwner}
      setBatchOwner={setBatchOwner}
      batchStatus={batchStatus}
      setBatchStatus={setBatchStatus}
      batchAssign={batchAssign}
      batchChangeStatus={batchChangeStatus}
      canDeleteCustomer={canDeleteCustomer}
      batchDelete={batchDelete}
      batchSummary={batchSummary}
      batchModalOpen={batchModalOpen}
      setBatchModalOpen={setBatchModalOpen}
      batchMessage={batchMessage}
      allChecked={allChecked}
      toggleAll={toggleAll}
      setSortBy={setSortBy}
      rows={rows}
      selectedIds={selectedIds}
      toggleOne={toggleOne}
      openDetail={openDetail}
      editCustomer={editCustomer}
      removeCustomer={removeCustomer}
      page={page}
      totalPages={totalPages}
      pagination={pagination}
      onPageChange={onPageChange}
      onSizeChange={onSizeChange}
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
      clearSummary={clearSummary}
    />
  )
}

export default memo(CustomersPanel)
