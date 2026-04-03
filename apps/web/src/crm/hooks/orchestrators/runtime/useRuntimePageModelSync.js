import {
  buildApprovalPageModelInputs,
  buildAuthPageModelInputs,
  buildCommercePageModelInputs,
  buildCustomerPageModelInputs,
  buildGovernancePageModelInputs,
  buildLeadImportPageModelInputs,
  buildPerfPageModelInputs,
  buildReportingPageModelInputs,
  useSyncApprovalPageModels,
  useSyncAuthPageModels,
  useSyncCommercePageModels,
  useSyncCustomerPageModels,
  useSyncGovernancePageModels,
  useSyncLeadImportPageModels,
  useSyncPerfPageModels,
  useSyncReportingPageModels,
} from '..'

export function useRuntimePageModelSync({ ctx }) {
  const authModel = buildAuthPageModelInputs(ctx)
  const reportingModel = buildReportingPageModelInputs(ctx)
  useSyncAuthPageModels(authModel)
  const { mainTasks, mainReportDesigner } = useSyncReportingPageModels(reportingModel)
  const customerModel = buildCustomerPageModelInputs({
    ...ctx,
    cross: { mainTasks },
  })
  const commerceModel = buildCommercePageModelInputs({
    ...ctx,
    cross: { mainReportDesigner },
  })
  const governanceModel = buildGovernancePageModelInputs(ctx)
  const approvalModel = buildApprovalPageModelInputs(ctx)
  const leadImportModel = buildLeadImportPageModelInputs(ctx)
  const perfModel = buildPerfPageModelInputs(ctx)
  useSyncCustomerPageModels(customerModel)
  useSyncCommercePageModels(commerceModel)
  useSyncGovernancePageModels(governanceModel)
  useSyncApprovalPageModels(approvalModel)
  useSyncLeadImportPageModels(leadImportModel)
  useSyncPerfPageModels(perfModel)
}
