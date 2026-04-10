import { describe, expect, it } from 'vitest'
import * as runtime from '../hooks/orchestrators/runtime'

describe('runtime barrel exports', () => {
  it('exposes runtime auth persistence modules through named exports only', () => {
    expect(runtime.default).toBeUndefined()
    expect(runtime.useRuntimeAuthPersistenceEffects).toBeDefined()
    expect(runtime.useRuntimeOidcExchangeEffect).toBeDefined()
    expect(runtime.useRuntimeAnonymousSsoConfigEffect).toBeDefined()
    expect(runtime.useRuntimeAuthCleanupEffect).toBeDefined()
    expect(runtime.useRuntimeRouteGuardEffect).toBeDefined()
    expect(runtime.useRuntimeSessionRestoreEffect).toBeDefined()
    expect(runtime.resolveOidcTenantId).toBeDefined()
    expect(runtime.isValidOidcState).toBeDefined()
  })
})
