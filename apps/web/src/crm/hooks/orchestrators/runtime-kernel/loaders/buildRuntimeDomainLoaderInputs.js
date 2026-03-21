export function buildCoreListLoadersInput(params, shared) {
  return {
    ...shared,
    seqRef: params.seqRef,
    beginPageRequest: params.beginPageRequest,
    handleError: params.handleError,
    leadQ: params.leadQ,
    leadStatus: params.leadStatus,
    leadPage: params.leadPage,
    leadSize: params.leadSize,
    setLeads: params.setLeads,
    setLeadPage: params.setLeadPage,
    setLeadTotalPages: params.setLeadTotalPages,
    setLeadSize: params.setLeadSize,
    customerQ: params.customerQ,
    customerStatus: params.customerStatus,
    customerPage: params.customerPage,
    customerSize: params.customerSize,
    setCustomers: params.setCustomers,
    setCustomerPage: params.setCustomerPage,
    setCustomerTotalPages: params.setCustomerTotalPages,
    setCustomerSize: params.setCustomerSize,
    oppStage: params.oppStage,
    opportunityPage: params.opportunityPage,
    opportunitySize: params.opportunitySize,
    setOpportunities: params.setOpportunities,
    setOpportunityPage: params.setOpportunityPage,
    setOpportunityTotalPages: params.setOpportunityTotalPages,
    setOpportunitySize: params.setOpportunitySize,
    contactQ: params.contactQ,
    contactPage: params.contactPage,
    contactSize: params.contactSize,
    setContacts: params.setContacts,
    setContactPage: params.setContactPage,
    setContactTotalPages: params.setContactTotalPages,
    setContactSize: params.setContactSize,
    contractQ: params.contractQ,
    contractStatus: params.contractStatus,
    contractPage: params.contractPage,
    contractSize: params.contractSize,
    setContracts: params.setContracts,
    setContractPage: params.setContractPage,
    setContractTotalPages: params.setContractTotalPages,
    setContractSize: params.setContractSize,
    paymentStatus: params.paymentStatus,
    paymentPage: params.paymentPage,
    paymentSize: params.paymentSize,
    setPayments: params.setPayments,
    setPaymentPage: params.setPaymentPage,
    setPaymentTotalPages: params.setPaymentTotalPages,
    setPaymentSize: params.setPaymentSize,
  }
}

export function buildCommerceLoadersInput(params, shared) {
  return {
    ...shared,
    quoteOpportunityFilter: params.quoteOpportunityFilter,
    orderOpportunityFilter: params.orderOpportunityFilter,
  }
}

export function buildLeadImportLoadersInput(params, shared) {
  return {
    ...shared,
    canViewOpsMetrics: params.canViewOpsMetrics,
    leadImportStatusFilter: params.leadImportStatusFilter,
    leadImportPage: params.leadImportPage,
    leadImportSize: params.leadImportSize,
    leadImportJob: params.leadImportJob,
    setLeadImportJobs: params.setLeadImportJobs,
    setLeadImportPage: params.setLeadImportPage,
    setLeadImportTotalPages: params.setLeadImportTotalPages,
    setLeadImportJob: params.setLeadImportJob,
    setLeadImportFailedRows: params.setLeadImportFailedRows,
    setLeadImportMetrics: params.setLeadImportMetrics,
    leadImportExportStatusFilter: params.leadImportExportStatusFilter,
    leadImportExportPage: params.leadImportExportPage,
    leadImportExportSize: params.leadImportExportSize,
    setLeadImportExportJobs: params.setLeadImportExportJobs,
    setLeadImportExportPage: params.setLeadImportExportPage,
    setLeadImportExportTotalPages: params.setLeadImportExportTotalPages,
  }
}

export function buildInlineLoadersInput(params, shared) {
  return {
    beginPageRequest: params.beginPageRequest,
    ...shared,
    followCustomerId: params.followCustomerId,
    followQ: params.followQ,
    setTasks: params.setTasks,
    setFollowUps: params.setFollowUps,
    setSsoConfig: params.setSsoConfig,
  }
}

export function buildGovernanceLoadersInput(params, shared, normalizeDateFormat) {
  return {
    canManageUsers: params.canManageUsers,
    canManageSalesAutomation: params.canManageSalesAutomation,
    ...shared,
    setPermissionMatrix: params.setPermissionMatrix,
    setPermissionConflicts: params.setPermissionConflicts,
    setLeadAssignmentRules: params.setLeadAssignmentRules,
    setAutomationRules: params.setAutomationRules,
    setAdminUsers: params.setAdminUsers,
    setTenantRows: params.setTenantRows,
    normalizeDateFormat,
  }
}

export function buildApprovalLoadersInput(params, shared) {
  return {
    ...shared,
    approvalTaskStatus: params.approvalTaskStatus,
    approvalOverdueOnly: params.approvalOverdueOnly,
    approvalEscalatedOnly: params.approvalEscalatedOnly,
    notificationStatusFilter: params.notificationStatusFilter,
    notificationPage: params.notificationPage,
    notificationSize: params.notificationSize,
    setApprovalTemplates: params.setApprovalTemplates,
    setApprovalStats: params.setApprovalStats,
    setApprovalTasks: params.setApprovalTasks,
    setApprovalInstances: params.setApprovalInstances,
    setApprovalDetail: params.setApprovalDetail,
    setApprovalTemplateVersions: params.setApprovalTemplateVersions,
    setApprovalVersionTemplateId: params.setApprovalVersionTemplateId,
    setNotificationJobs: params.setNotificationJobs,
    setNotificationPage: params.setNotificationPage,
    setNotificationTotalPages: params.setNotificationTotalPages,
    setSelectedNotificationJobs: params.setSelectedNotificationJobs,
  }
}

export function buildReportingAuditLoadersInput(params, shared) {
  return {
    canViewAudit: params.canViewAudit,
    canViewReports: params.canViewReports,
    ...shared,
    auditUser: params.auditUser,
    auditRole: params.auditRole,
    auditAction: params.auditAction,
    auditFrom: params.auditFrom,
    auditTo: params.auditTo,
    reportOwner: params.reportOwner,
    reportDepartment: params.reportDepartment,
    reportTimezone: params.reportTimezone,
    reportCurrency: params.reportCurrency,
    exportStatusFilter: params.exportStatusFilter,
    exportJobsPage: params.exportJobsPage,
    exportJobsSize: params.exportJobsSize,
    reportExportStatusFilter: params.reportExportStatusFilter,
    reportExportJobsPage: params.reportExportJobsPage,
    reportExportJobsSize: params.reportExportJobsSize,
    hasInvalidAuditRange: params.hasInvalidAuditRange,
    setAuditRangeError: params.setAuditRangeError,
    t: params.t,
    setAuditLogs: params.setAuditLogs,
    setReports: params.setReports,
    setExportJobs: params.setExportJobs,
    setExportJobsPage: params.setExportJobsPage,
    setExportJobsTotalPages: params.setExportJobsTotalPages,
    setReportExportJobs: params.setReportExportJobs,
    setReportExportJobsPage: params.setReportExportJobsPage,
    setReportExportJobsTotalPages: params.setReportExportJobsTotalPages,
    setDesignerTemplates: params.setDesignerTemplates,
  }
}

export function buildWorkbenchLoadersInput(params, shared) {
  return {
    ...shared,
    auditFrom: params.auditFrom,
    auditTo: params.auditTo,
    reportOwner: params.reportOwner,
    reportDepartment: params.reportDepartment,
    reportTimezone: params.reportTimezone,
    setWorkbenchToday: params.setWorkbenchToday,
    setCustomerTimeline: params.setCustomerTimeline,
    setOpportunityTimeline: params.setOpportunityTimeline,
  }
}