import { buildAuthPageModelInputs, useSyncAuthPageModels } from '../useAuthOrchestrator'
import { buildCustomerPageModelInputs, useSyncCustomerPageModels } from '../useCustomerOrchestrator'
import { buildCommercePageModelInputs, useSyncCommercePageModels } from '../useCommerceOrchestrator'
import { buildGovernancePageModelInputs, useSyncGovernancePageModels } from '../useGovernanceOrchestrator'
import { buildApprovalPageModelInputs, useSyncApprovalPageModels } from '../useApprovalOrchestrator'
import { buildReportingPageModelInputs, useSyncReportingPageModels } from '../useReportingOrchestrator'
import { buildLeadImportPageModelInputs, useSyncLeadImportPageModels } from '../useLeadImportOrchestrator'
import { buildPerfPageModelInputs, useSyncPerfPageModels } from '../usePerfOrchestrator'

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
