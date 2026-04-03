# Runtime Dependency Map

Scope: `apps/web/src/crm/hooks/orchestrators/**`

Only direct relative imports inside this scope are listed below. Files not shown here had no detected direct in-scope relative imports.

## Root

- `AppOrchestratorRuntime.jsx` -> `AppRuntimeEngine.jsx`
- `AppRuntimeEngine.jsx` -> `AppRuntimeEngineImpl.jsx`
- `AppRuntimeEngineImpl.jsx` -> `runtime/index.js`, `runtime-kernel/index.js`
- `index.js` -> `useAppRuntimeOrchestratorState.js`, `useApprovalOrchestrator.js`, `useApprovalRuntimeState.js`, `useAuthOrchestrator.js`, `useAuthRuntimeState.js`, `useCommerceOrchestrator.js`, `useCommerceRuntimeState.js`, `useCustomerOrchestrator.js`, `useCustomerRuntimeState.js`, `useGovernanceOrchestrator.js`, `useGovernanceRuntimeState.js`, `useLeadImportOrchestrator.js`, `useLeadImportRuntimeState.js`, `usePerfOrchestrator.js`, `usePerfRuntimeState.js`, `useReportingOrchestrator.js`, `useReportingRuntimeState.js`, `useRuntimeSectionFields.js`
- `useAppRuntimeOrchestratorState.js` -> `useApprovalRuntimeState.js`, `useAuthRuntimeState.js`, `useCommerceRuntimeState.js`, `useCustomerRuntimeState.js`, `useGovernanceRuntimeState.js`, `useLeadImportRuntimeState.js`, `usePerfRuntimeState.js`, `useReportingRuntimeState.js`
- `useApprovalRuntimeState.js` -> `useRuntimeSectionFields.js`
- `useAuthRuntimeState.js` -> `useRuntimeSectionFields.js`
- `useCommerceRuntimeState.js` -> `useRuntimeSectionFields.js`
- `useCustomerRuntimeState.js` -> `useRuntimeSectionFields.js`
- `useGovernanceRuntimeState.js` -> `useRuntimeSectionFields.js`
- `useLeadImportRuntimeState.js` -> `useRuntimeSectionFields.js`
- `usePerfRuntimeState.js` -> `useRuntimeSectionFields.js`
- `useReportingRuntimeState.js` -> `useRuntimeSectionFields.js`

## `runtime/`

- `runtime/index.js` -> `RuntimeShellBranch.jsx`, `buildRuntimeDomainActionsHookInput.js`, `buildRuntimePageModelConfig.js`, `buildRuntimePageModelSections.js`, `buildRuntimeShellProps.js`, `routeConfig.js`, `useRuntimeAuthActions.js`, `useRuntimeCrudDomainActions.js`, `useRuntimeDomainActions.js`, `useRuntimeFormValidators.js`, `useRuntimeLoaders.js`, `useRuntimePageLoaders.js`, `useRuntimePageModelContext.js`, `useRuntimePageModelSync.js`, `useRuntimePersistenceEffects.js`, `useRuntimeRouting.js`, `useRuntimeStateSlices.js`
- `RuntimeShellBranch.jsx` -> `useRuntimeAuthBranch.jsx`
- `buildRuntimeDomainActionsConfig.js` -> `routeConfig.js`
- `buildRuntimeDomainActionsHookInput.js` -> `buildRuntimeDomainActionsConfig.js`, `buildRuntimeDomainActionsHookBindings.js`
- `buildRuntimeStateContexts.js` -> `buildRuntimeStateContextsAuthUi.js`, `buildRuntimeStateContextsCustomerCommerce.js`, `buildRuntimeStateContextsGovernanceApproval.js`, `buildRuntimeStateContextsReportingMisc.js`
- `buildRuntimeStateContextsReportingMisc.js` -> `runtime-state/index.js`
- `runtime-state/index.js` -> `buildRuntimeStateContextSections.js`
- `useRuntimeCrudDomainActions.js` -> `routeConfig.js`
- `useRuntimeFormValidators.js` -> `routeConfig.js`
- `useRuntimeLoaders.js` -> `routeConfig.js`
- `useRuntimePageLoaders.js` -> `buildRuntimeCommonPageLoaders.js`, `buildRuntimeKeyPageLoaders.js`
- `useRuntimePageModelContext.js` -> `buildRuntimePageModelAuthAndReporting.js`, `buildRuntimePageModelCustomerAndCommerce.js`, `buildRuntimePageModelGovernanceAndApproval.js`, `buildRuntimePageModelMisc.js`
- `useRuntimePageModelSync.js` -> `index.js`
- `useRuntimePersistenceEffects.js` -> `routeConfig.js`
- `useRuntimeStateSlices.js` -> `buildRuntimeStateContexts.js`, `buildRuntimeStateSnapshots.js`, `index.js`

## `runtime-kernel/`

- `runtime-kernel/index.js` -> `phases/index.js`
- `buildRuntimeViewModels.js` -> `runtime/index.js`, `view-model/index.js`
- `runtimeEngineBuilders.js` -> `auth/index.js`, `buildRuntimeViewModels.js`, `domain-actions/index.js`, `loaders/index.js`, `shared/index.js`, `view-model/index.js`
- `useRuntimeDomainLoaders.js` -> `loaders/index.js`, `useRuntimeInlineLoaders.js`, `runtime/index.js`
- `useRuntimeKernelState.js` -> `runtime/index.js`

### `runtime-kernel/auth/`

- `auth/index.js` -> `buildAppAuthModelInput.js`, `buildAppNavigationModelInput.js`, `buildRuntimeAuthActionsInput.js`, `buildRuntimeFormValidatorsInput.js`
- `buildAppAuthModelInput.js` -> `runtime-kernel/shared/index.js`
- `buildRuntimeAuthActionsInput.js` -> `runtime-kernel/shared/index.js`
- `buildRuntimeFormValidatorsInput.js` -> `runtime-kernel/shared/index.js`

### `runtime-kernel/domain-actions/`

- `domain-actions/index.js` -> `buildRuntimeDomainActionsHookPayload.js`, `buildRuntimeDomainActionsInputPayload.js`
- `buildRuntimeDomainActionsHookPayload.js` -> `buildRuntimeDomainActionsInputPayload.js`, `runtime/index.js`
- `buildRuntimeDomainActionsInputPayload.js` -> `runtime-kernel/shared/index.js`

### `runtime-kernel/loaders/`

- `loaders/index.js` -> `buildRuntimeCrudActionsInput.js`, `buildRuntimeDomainLoaderInputs.js`, `buildRuntimeDomainLoadersInput.js`, `buildRuntimeLoadersInput.js`, `buildRuntimePageLoadersInput.js`, `buildRuntimePersistenceInput.js`
- `buildRuntimeCrudActionsInput.js` -> `runtime-kernel/shared/index.js`
- `buildRuntimeDomainLoadersInput.js` -> `runtime-kernel/shared/index.js`
- `buildRuntimeLoadersInput.js` -> `runtime-kernel/shared/index.js`
- `buildRuntimePageLoadersInput.js` -> `runtime-kernel/shared/index.js`
- `buildRuntimePersistenceInput.js` -> `runtime-kernel/shared/index.js`

### `runtime-kernel/phases/`

- `phases/index.js` -> `buildShellViewModel.js`, `setupActions.js`, `setupDataLoaders.js`, `setupNavigationModel.js`, `setupRuntimeContext.js`
- `buildShellViewModel.js` -> `phases/helpers/index.js`, `runtimeEngineBuilders.js`, `runtime/index.js`
- `setupActions.js` -> `phases/helpers/index.js`, `runtime/index.js`
- `setupDataLoaders.js` -> `phases/helpers/index.js`, `runtimeEngineBuilders.js`, `useRuntimeDomainLoaders.js`, `runtime/index.js`
- `setupNavigationModel.js` -> `runtimeEngineBuilders.js`, `runtime/index.js`
- `setupRuntimeContext.js` -> `useRuntimeKernelState.js`, `runtime/index.js`

#### `runtime-kernel/phases/helpers/`

- `phases/helpers/index.js` -> `buildShellViewModelBuilders.js`, `setupActionsBuilders.js`, `setupDataLoadersBuilders.js`, `setupDataLoadersResultBuilder.js`
- `buildShellViewModelBuilders.js` -> `runtimeEngineBuilders.js`
- `setupActionsBuilders.js` -> `runtimeEngineBuilders.js`, `runtime/index.js`
- `setupDataLoadersBuilders.js` -> `setupDataLoadersInputBuilders.js`, `setupDataLoadersResultBuilder.js`
- `setupDataLoadersInputBuilders.js` -> `runtimeEngineBuilders.js`

### `runtime-kernel/shared/`

- `shared/index.js` -> `mergeRuntimeInput.js`, `runtimeErrorFormatting.js`

### `runtime-kernel/view-model/`

- `view-model/index.js` -> `buildRuntimeViewModelPayloads.js`, `buildRuntimeViewModelsInput.js`
- `buildRuntimeViewModelPayloads.js` -> `runtime/index.js`
- `buildRuntimeViewModelsInput.js` -> `runtime-kernel/shared/index.js`

