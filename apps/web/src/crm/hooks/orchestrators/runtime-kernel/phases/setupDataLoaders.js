import { useCallback } from 'react'
import { useAppAuthModel } from '../../../useAppAuthModel'
import { useRuntimeLoaders } from '../../runtime/useRuntimeLoaders'
import { useRuntimePersistenceEffects } from '../../runtime/useRuntimePersistenceEffects'
import { useRuntimePageLoaders } from '../../runtime/useRuntimePageLoaders'
import { useRuntimeFormValidators } from '../../runtime/useRuntimeFormValidators'
import { useRuntimeDomainLoaders } from '../useRuntimeDomainLoaders'
import {
  formatRuntimeErrorMessage,
} from '../runtimeEngineBuilders'
import {
  buildAuthModelHookInput,
  buildDomainLoadersHookInput,
  buildFormValidatorsHookInput,
  buildPageLoadersHookInput,
  buildPersistenceHookInput,
  buildRuntimeLoadersHookInput,
  buildSetupDataLoadersResult,
} from './helpers/setupDataLoadersBuilders'

export function useSetupDataLoaders(ctx) {
  const formatErrorMessage = useCallback((err) => formatRuntimeErrorMessage(err), [])

  const authModel = useAppAuthModel(
    buildAuthModelHookInput(ctx, formatErrorMessage),
  )

  const validators = useRuntimeFormValidators(buildFormValidatorsHookInput(ctx))

  const domainLoaders = useRuntimeDomainLoaders(
    buildDomainLoadersHookInput(ctx, authModel.handleError, validators.hasInvalidAuditRange),
  )

  useRuntimePersistenceEffects(
    buildPersistenceHookInput(
      ctx,
      authModel.saveAuth,
      domainLoaders.loadSsoConfig,
      authModel.handleLoginError,
    ),
  )

  const pageLoaders = useRuntimePageLoaders(
    buildPageLoadersHookInput(ctx, domainLoaders),
  )

  const runtimeLoaders = useRuntimeLoaders(
    buildRuntimeLoadersHookInput(ctx, pageLoaders, authModel.handleError),
  )

  return buildSetupDataLoadersResult({
    authModel,
    validators,
    domainLoaders,
    pageLoaders,
    runtimeLoaders,
  })
}
