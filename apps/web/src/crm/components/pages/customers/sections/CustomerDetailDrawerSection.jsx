import { formatMoney, translateStatus } from '../../../../shared'
import RowDetailDrawer from '../../../RowDetailDrawer'
import { Customer360View } from '../customer360'

export default function CustomerDetailDrawerSection({
  t,
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
}) {
  return (
    <RowDetailDrawer
      open={!!detail}
      title={t('customer360QuickView')}
      t={t}
      onClose={closeDrawer}
      rows={[
        { label: t('idLabel'), value: detail?.id },
        { label: t('companyName'), value: detail?.name },
        { label: t('owner'), value: detail?.owner },
        { label: t('status'), value: translateStatus(t, detail?.status) },
        { label: t('amount'), value: detail ? formatMoney(detail.value) : '-' },
      ]}
      actions={[
        { label: t('customer360OpenPage'), onClick: () => detail && openFullDetail(detail) },
        { label: t('quickCreateFollowUp'), onClick: () => detail && quickCreateFollowUp && quickCreateFollowUp(detail) },
        { label: t('quickCreateTask'), onClick: () => detail && quickCreateTask && quickCreateTask(detail) },
      ]}
      extra={
        detail ? (
          <Customer360View
            t={t}
            customer={detail}
            vm={customer360ViewModel}
            loading={timelineLoading || detailLoading}
            moduleMeta={detailModuleMeta}
            mode="drawer"
            onBackToList={() => null}
            onOpenFullDetail={() => openFullDetail(detail)}
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
              executeCustomer360Action('manualRefresh')
            }}
          />
        ) : null
      }
    />
  )
}
