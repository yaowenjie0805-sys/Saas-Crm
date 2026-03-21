import { buildRuntimeStateContextsAuthUi } from './buildRuntimeStateContextsAuthUi'
import { buildRuntimeStateContextsCustomerCommerce } from './buildRuntimeStateContextsCustomerCommerce'
import { buildRuntimeStateContextsGovernanceApproval } from './buildRuntimeStateContextsGovernanceApproval'
import { buildRuntimeStateContextsReportingMisc } from './buildRuntimeStateContextsReportingMisc'

export function buildRuntimeStateContexts(runtime) {
  return {
    ...buildRuntimeStateContextsAuthUi(runtime),
    ...buildRuntimeStateContextsCustomerCommerce(runtime),
    ...buildRuntimeStateContextsGovernanceApproval(runtime),
    ...buildRuntimeStateContextsReportingMisc(runtime),
  }
}
