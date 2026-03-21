import { buildRuntimeAuthModel, buildRuntimeReportingModel } from './buildRuntimePageModelAuthAndReporting'
import { buildRuntimeCustomerModel, buildRuntimeCommerceModel } from './buildRuntimePageModelCustomerAndCommerce'
import { buildRuntimeGovernanceModel, buildRuntimeApprovalModel } from './buildRuntimePageModelGovernanceAndApproval'
import { buildRuntimeLeadImportModel, buildRuntimePerfModel } from './buildRuntimePageModelMisc'

export function buildRuntimePageModelContext({
  runtime,
  base,
  reporting,
  customer,
  commerce,
  governance,
  approval,
  leadImport: _leadImport,
  perf: _perf,
}) {
  return {
    authModel: buildRuntimeAuthModel({ runtime, base, reporting, governance }),
    reportingModel: buildRuntimeReportingModel({ runtime, base, customer, reporting, governance }),
    customerModel: buildRuntimeCustomerModel({ runtime, base, customer, reporting, governance }),
    commerceModel: buildRuntimeCommerceModel({ runtime, commerce, customer, governance }),
    governanceModel: buildRuntimeGovernanceModel({ runtime, base, governance }),
    approvalModel: buildRuntimeApprovalModel({ runtime, base, approval, governance }),
    leadImportModel: buildRuntimeLeadImportModel(runtime),
    perfModel: buildRuntimePerfModel(runtime),
  }
}
