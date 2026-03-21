import { normalizeDateFormat } from '../runtime/routeConfig'
import { useCoreListDomainLoaders } from '../../useCoreListDomainLoaders'
import { useGovernanceDomainLoaders } from '../../useGovernanceDomainLoaders'
import { useApprovalDomainLoaders } from '../../useApprovalDomainLoaders'
import { useReportingAuditDomainLoaders } from '../../useReportingAuditDomainLoaders'
import { useWorkbenchDomainLoaders } from '../../useWorkbenchDomainLoaders'
import { useCommerceDomainLoaders } from '../../useCommerceDomainLoaders'
import { useLeadImportDomainLoaders } from '../../useLeadImportDomainLoaders'
import { useRuntimeInlineLoaders } from './useRuntimeInlineLoaders'
import {
  buildApprovalLoadersInput,
  buildCommerceLoadersInput,
  buildCoreListLoadersInput,
  buildGovernanceLoadersInput,
  buildInlineLoadersInput,
  buildLeadImportLoadersInput,
  buildReportingAuditLoadersInput,
  buildWorkbenchLoadersInput,
} from './loaders/buildRuntimeDomainLoaderInputs'

export function useRuntimeDomainLoaders(params) {
  const shared = {
    authToken: params.auth?.token,
    lang: params.lang,
  }

  const {
    loaders: coreListLoaders,
    paginationActions: corePaginationActions,
  } = useCoreListDomainLoaders(buildCoreListLoadersInput(params, shared))

  const commerceDomain = useCommerceDomainLoaders(buildCommerceLoadersInput(params, shared))

  const {
    loadLeadImportJobs,
    loadLeadImportFailedRows,
    loadLeadImportMetrics,
    loadLeadImportExportJobs,
  } = useLeadImportDomainLoaders(buildLeadImportLoadersInput(params, shared))

  const {
    loadLeads,
    loadCustomers,
    loadOpportunities,
    loadContacts,
    loadContracts,
    loadPayments,
  } = coreListLoaders

  const { loadTasks, loadFollowUps, loadSsoConfig } = useRuntimeInlineLoaders(
    buildInlineLoadersInput(params, shared),
  )

  const {
    loadPermissionMatrix,
    loadPermissionConflicts,
    loadLeadAssignmentRules,
    loadAutomationRulesV1,
    loadAdminUsers,
    loadTenants,
  } = useGovernanceDomainLoaders(buildGovernanceLoadersInput(params, shared, normalizeDateFormat))

  const {
    loadApprovalTemplates,
    loadApprovalStats,
    loadApprovalTasks,
    loadApprovalInstances,
    loadApprovalDetail,
    loadApprovalTemplateVersions,
    loadNotificationJobs,
  } = useApprovalDomainLoaders(buildApprovalLoadersInput(params, shared))

  const {
    loadAudit,
    loadReports,
    loadExportJobs,
    loadReportExportJobs,
    loadDesignerTemplates,
  } = useReportingAuditDomainLoaders(buildReportingAuditLoadersInput(params, shared))

  const {
    loadWorkbenchToday,
    loadCustomerTimeline,
    loadOpportunityTimeline,
  } = useWorkbenchDomainLoaders(buildWorkbenchLoadersInput(params, shared))

  return {
    corePaginationActions,
    commerceDomain,
    loadLeadImportJobs,
    loadLeadImportFailedRows,
    loadLeadImportMetrics,
    loadLeadImportExportJobs,
    loadLeads,
    loadCustomers,
    loadOpportunities,
    loadContacts,
    loadContracts,
    loadPayments,
    loadTasks,
    loadFollowUps,
    loadSsoConfig,
    loadPermissionMatrix,
    loadPermissionConflicts,
    loadLeadAssignmentRules,
    loadAutomationRulesV1,
    loadAdminUsers,
    loadTenants,
    loadApprovalTemplates,
    loadApprovalStats,
    loadApprovalTasks,
    loadApprovalInstances,
    loadApprovalDetail,
    loadApprovalTemplateVersions,
    loadNotificationJobs,
    loadAudit,
    loadReports,
    loadExportJobs,
    loadReportExportJobs,
    loadDesignerTemplates,
    loadWorkbenchToday,
    loadCustomerTimeline,
    loadOpportunityTimeline,
  }
}