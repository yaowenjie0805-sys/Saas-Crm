import { useAuthRuntimeState } from './useAuthRuntimeState'
import { useCustomerRuntimeState } from './useCustomerRuntimeState'
import { useCommerceRuntimeState } from './useCommerceRuntimeState'
import { useGovernanceRuntimeState } from './useGovernanceRuntimeState'
import { useApprovalRuntimeState } from './useApprovalRuntimeState'
import { useReportingRuntimeState } from './useReportingRuntimeState'
import { useLeadImportRuntimeState } from './useLeadImportRuntimeState'
import { usePerfRuntimeState } from './usePerfRuntimeState'

export function useAppRuntimeOrchestratorState({ persisted, readPageSize, getMetrics }) {
  const auth = useAuthRuntimeState()
  const customer = useCustomerRuntimeState({ persisted, readPageSize })
  const commerce = useCommerceRuntimeState({ persisted, readPageSize })
  const governance = useGovernanceRuntimeState()
  const approval = useApprovalRuntimeState({ readPageSize })
  const reporting = useReportingRuntimeState({ persisted, readPageSize })
  const leadImport = useLeadImportRuntimeState({ readPageSize })
  const perf = usePerfRuntimeState({ getMetrics })

  return {
    ...auth,
    ...customer,
    ...commerce,
    ...governance,
    ...approval,
    ...reporting,
    ...leadImport,
    ...perf,
  }
}
