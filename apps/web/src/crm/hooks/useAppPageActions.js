import { useGovernancePageActions } from './pageActions/useGovernancePageActions'
import { useApprovalPageActions } from './pageActions/useApprovalPageActions'
import { useReportingPageActions } from './pageActions/useReportingPageActions'

export function useAppPageActions(params) {
  const governanceActions = useGovernancePageActions(params)
  const approvalActions = useApprovalPageActions(params)
  const reportingActions = useReportingPageActions(params)

  return {
    ...governanceActions,
    ...approvalActions,
    ...reportingActions,
  }
}
