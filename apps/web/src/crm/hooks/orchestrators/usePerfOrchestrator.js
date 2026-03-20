import { useEffect, useMemo } from 'react'
import { useAppStore } from '../../store/appStore'

export function usePerfOrchestrator() {
  const setPerfDomainState = useAppStore((state) => state.setPerfDomainState)
  const setPerfModels = (models) => setPerfDomainState(models || {})
  return { setPerfModels }
}

export function useSyncPerfPageModels({
  perfMetrics,
  currentLoaderKey,
  lastRefreshReason,
  currentPageSignature,
  currentSignatureHit,
  recentWorkbenchJump,
  domainLoadSource,
  lastPerfSnapshotAt,
}) {
  const { setPerfModels } = usePerfOrchestrator()
  const perfModels = useMemo(() => ({
    perfMetrics,
    currentLoaderKey,
    lastRefreshReason,
    currentPageSignature,
    currentSignatureHit,
    recentWorkbenchJump,
    domainLoadSource,
    lastPerfSnapshotAt,
  }), [
    perfMetrics,
    currentLoaderKey,
    lastRefreshReason,
    currentPageSignature,
    currentSignatureHit,
    recentWorkbenchJump,
    domainLoadSource,
    lastPerfSnapshotAt,
  ])
  useEffect(() => {
    setPerfModels(perfModels)
  }, [setPerfModels, perfModels])
}

export function buildPerfPageModelInputs(ctx) {
  return ctx?.perfModel || {}
}
