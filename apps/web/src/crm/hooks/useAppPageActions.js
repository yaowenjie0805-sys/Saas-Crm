import {
  useApprovalPageActions,
  useGovernancePageActions,
  useReportingPageActions,
} from './pageActions'

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
