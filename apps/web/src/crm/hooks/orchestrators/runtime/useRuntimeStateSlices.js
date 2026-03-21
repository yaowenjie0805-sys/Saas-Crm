import { useAppRuntimeOrchestratorState } from '../useAppRuntimeOrchestratorState'
import { buildRuntimeStateContexts } from './buildRuntimeStateContexts'
import { buildRuntimeStateSnapshots } from './buildRuntimeStateSnapshots'

export function useRuntimeStateSlices({ persisted, readPageSize }) {
  const runtime = useAppRuntimeOrchestratorState({ persisted, readPageSize })

  return {
    runtime,
    ...buildRuntimeStateContexts(runtime),
    ...buildRuntimeStateSnapshots(runtime),
  }
}
