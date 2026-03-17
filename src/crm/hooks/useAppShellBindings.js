import { useAppPrefetchNavigation } from './useAppPrefetchNavigation'
import { useWorkbenchNavigation } from './useWorkbenchNavigation'
import { usePerfPanel } from './usePerfPanel'

export function useAppShellBindings({
  prefetch,
  workbench,
  perf,
}) {
  const { onPrefetch, onNavigate } = useAppPrefetchNavigation(prefetch)
  const workbenchBindings = useWorkbenchNavigation(workbench)
  const { copyPerfSnapshot } = usePerfPanel(perf)

  return {
    onPrefetch,
    onNavigate,
    copyPerfSnapshot,
    ...workbenchBindings,
  }
}
