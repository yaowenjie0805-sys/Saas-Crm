import { useEffect } from 'react'
import { useAppStore } from '../../store/appStore'
import { useCommerceMapper } from '../mainContentMappers'

export function useCommerceOrchestrator() {
  const setCommerceDomainState = useAppStore((state) => state.setCommerceDomainState)
  const setCommerceModels = (models) => setCommerceDomainState(models || {})
  return { setCommerceModels }
}

export function useSyncCommercePageModels(params) {
  const { setCommerceModels } = useCommerceOrchestrator()
  const { mainReportDesigner } = params
  const {
    mainCommerce,
    mainContracts,
    mainPayments,
  } = useCommerceMapper(params)
  useEffect(() => {
    setCommerceModels({
      commerce: mainCommerce,
      contracts: mainContracts,
      payments: mainPayments,
      reportDesigner: mainReportDesigner,
    })
  }, [setCommerceModels, mainCommerce, mainContracts, mainPayments, mainReportDesigner])
}

export function buildCommercePageModelInputs(ctx) {
  const cross = ctx?.cross || {}
  return {
    ...(ctx?.commerceModel || {}),
    mainReportDesigner: cross.mainReportDesigner,
  }
}
