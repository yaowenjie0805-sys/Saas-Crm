import { useEffect } from 'react'
import { useAppStore } from '../../store/appStore'
import { useApprovalMapper } from '../mainContentMappers'

export function useApprovalOrchestrator() {
  const setApprovalDomainState = useAppStore((state) => state.setApprovalDomainState)
  const setApprovalModels = (models) => setApprovalDomainState(models || {})
  return { setApprovalModels }
}

export function useSyncApprovalPageModels(params) {
  const { setApprovalModels } = useApprovalOrchestrator()
  const { mainApprovals } = useApprovalMapper(params)
  useEffect(() => {
    setApprovalModels({ approvals: mainApprovals })
  }, [setApprovalModels, mainApprovals])
}

export function buildApprovalPageModelInputs(ctx) {
  return ctx?.approvalModel || {}
}
