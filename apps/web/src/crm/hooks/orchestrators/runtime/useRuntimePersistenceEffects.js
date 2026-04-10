import { useRuntimeAuthPersistenceEffects } from './useRuntimeAuthPersistenceEffects'
import { useRuntimeFilterPersistenceEffects } from './useRuntimeFilterPersistenceEffects'

export function useRuntimePersistenceEffects(args) {
  useRuntimeAuthPersistenceEffects(args)
  useRuntimeFilterPersistenceEffects(args)
}
