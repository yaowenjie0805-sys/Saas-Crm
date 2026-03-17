import { useMemo } from 'react'

export function useCustomerMappers(params) {
  const {
    role,
    refreshLeads,
    refreshCustomers,
    refreshPipeline,
    leadForm, setLeadForm, saveLead, convertLead, crudErrors, crudFieldErrors, leads, editLead, leadQ, setLeadQ,
    leadStatus, setLeadStatus, leadPage, leadTotalPages, leadSize, onLeadPageChange, onLeadSizeChange,
    bulkAssignLeadsByRule, bulkUpdateLeadStatus,
    importLeadsCsv, leadImportJob, leadImportJobs, leadImportStatusFilter, updateLeadImportStatusFilter,
    leadImportPage, leadImportTotalPages, leadImportSize, onLeadImportPageChange, onLeadImportSizeChange,
    selectLeadImportJob, cancelLeadImportJob, retryLeadImportJob, leadImportFailedRows, downloadLeadImportTemplate,
    leadImportMetrics, leadImportExportJobs, leadImportExportStatusFilter, updateLeadImportExportStatusFilter,
    leadImportExportPage, leadImportExportTotalPages, leadImportExportSize, onLeadImportExportPageChange,
    onLeadImportExportSizeChange, createLeadImportFailedRowsExportJob, downloadLeadImportFailedRowsExportJob,
    customerForm, setCustomerForm, saveCustomer, customers, editCustomer, canDeleteCustomer, removeCustomer,
    customerQ, setCustomerQ, customerStatus, setCustomerStatus, customerPage, customerTotalPages, customerSize,
    onCustomerPageChange, onCustomerSizeChange, loadCustomerTimeline, customerTimeline,
    createFollowUpShortcut, createTaskShortcut,
    opportunityForm, setOpportunityForm, saveOpportunity, opportunities, editOpportunity, canDeleteOpportunity,
    removeOpportunity, oppStage, setOppStage, opportunityPage, opportunityTotalPages, opportunitySize,
    onOpportunityPageChange, onOpportunitySizeChange, createQuoteFromOpportunity,
    viewOrdersFromOpportunity, loadOpportunityTimeline, opportunityTimeline, urgeApprovalShortcut,
    followUpForm, setFollowUpForm, saveFollowUp, followUps, editFollowUp, removeFollowUp, followCustomerId,
    setFollowCustomerId, followQ, setFollowQ, refreshFollowUps,
    contactForm, setContactForm, saveContact, contacts, editContact, removeContact, contactQ, setContactQ,
    contactPage, contactTotalPages, contactSize, onContactPageChange, onContactSizeChange, refreshContacts,
    navigateToWorkbenchTarget,
    buildWorkbenchFilterSignature,
    customer360Metrics,
  } = params

  const mainLeads = useMemo(() => ({
    leadForm, setLeadForm, saveLead, convertLead,
    formError: crudErrors.lead, fieldErrors: crudFieldErrors.lead, leads, editLead, leadQ, setLeadQ, leadStatus, setLeadStatus,
    pagination: { page: leadPage, totalPages: leadTotalPages, size: leadSize }, onPageChange: onLeadPageChange, onSizeChange: onLeadSizeChange,
    onRefresh: refreshLeads,
    bulkAssignByRule: bulkAssignLeadsByRule, bulkUpdateStatus: bulkUpdateLeadStatus,
    importCsv: importLeadsCsv, importJob: leadImportJob, importJobs: leadImportJobs, importStatus: leadImportStatusFilter, setImportStatus: updateLeadImportStatusFilter,
    importPaging: { page: leadImportPage, totalPages: leadImportTotalPages, size: leadImportSize }, onImportPageChange: onLeadImportPageChange, onImportSizeChange: onLeadImportSizeChange,
    selectImportJob: selectLeadImportJob, cancelImportJob: cancelLeadImportJob, retryImportJob: retryLeadImportJob, importFailedRows: leadImportFailedRows, downloadTemplate: downloadLeadImportTemplate,
    importMetrics: leadImportMetrics,
    canCreateImportExport: ['ADMIN', 'MANAGER'].includes(role),
    importExportJobs: leadImportExportJobs,
    importExportStatus: leadImportExportStatusFilter,
    setImportExportStatus: updateLeadImportExportStatusFilter,
    importExportPaging: { page: leadImportExportPage, totalPages: leadImportExportTotalPages, size: leadImportExportSize },
    onImportExportPageChange: onLeadImportExportPageChange,
    onImportExportSizeChange: onLeadImportExportSizeChange,
    createImportFailedRowsExportJob: createLeadImportFailedRowsExportJob,
    downloadImportFailedRowsExportJob: downloadLeadImportFailedRowsExportJob,
  }), [leadForm, setLeadForm, saveLead, convertLead, crudErrors.lead, crudFieldErrors.lead, leads, editLead, leadQ, setLeadQ, leadStatus, setLeadStatus, leadPage, leadTotalPages, leadSize, onLeadPageChange, onLeadSizeChange, refreshLeads, bulkAssignLeadsByRule, bulkUpdateLeadStatus, importLeadsCsv, leadImportJob, leadImportJobs, leadImportStatusFilter, updateLeadImportStatusFilter, leadImportPage, leadImportTotalPages, leadImportSize, onLeadImportPageChange, onLeadImportSizeChange, selectLeadImportJob, cancelLeadImportJob, retryLeadImportJob, leadImportFailedRows, downloadLeadImportTemplate, leadImportMetrics, role, leadImportExportJobs, leadImportExportStatusFilter, updateLeadImportExportStatusFilter, leadImportExportPage, leadImportExportTotalPages, leadImportExportSize, onLeadImportExportPageChange, onLeadImportExportSizeChange, createLeadImportFailedRowsExportJob, downloadLeadImportFailedRowsExportJob])

  const mainCustomers = useMemo(() => ({
    customerForm, setCustomerForm, saveCustomer, formError: crudErrors.customer, fieldErrors: crudFieldErrors.customer, customers, editCustomer, canDeleteCustomer, removeCustomer, customerQ, setCustomerQ, customerStatus, setCustomerStatus, pagination: { page: customerPage, totalPages: customerTotalPages, size: customerSize }, onPageChange: onCustomerPageChange, onSizeChange: onCustomerSizeChange, onRefresh: refreshCustomers, loadTimeline: loadCustomerTimeline, timeline: customerTimeline, quickCreateFollowUp: createFollowUpShortcut, quickCreateTask: createTaskShortcut,
    onWorkbenchNavigate: navigateToWorkbenchTarget,
    buildWorkbenchFilterSignature,
    customer360Metrics,
  }), [customerForm, setCustomerForm, saveCustomer, crudErrors.customer, crudFieldErrors.customer, customers, editCustomer, canDeleteCustomer, removeCustomer, customerQ, setCustomerQ, customerStatus, setCustomerStatus, customerPage, customerTotalPages, customerSize, onCustomerPageChange, onCustomerSizeChange, refreshCustomers, loadCustomerTimeline, customerTimeline, createFollowUpShortcut, createTaskShortcut, navigateToWorkbenchTarget, buildWorkbenchFilterSignature, customer360Metrics])

  const mainPipeline = useMemo(() => ({
    opportunityForm, setOpportunityForm, saveOpportunity, formError: crudErrors.opportunity, fieldErrors: crudFieldErrors.opportunity, opportunities, editOpportunity, canDeleteOpportunity, removeOpportunity, oppStage, setOppStage, pagination: { page: opportunityPage, totalPages: opportunityTotalPages, size: opportunitySize }, onPageChange: onOpportunityPageChange, onSizeChange: onOpportunitySizeChange, onRefresh: refreshPipeline, createQuoteFromOpportunity, viewOrdersFromOpportunity, loadTimeline: loadOpportunityTimeline, timeline: opportunityTimeline, quickCreateFollowUp: createFollowUpShortcut, quickCreateTask: createTaskShortcut, quickUrgeApproval: urgeApprovalShortcut,
  }), [opportunityForm, setOpportunityForm, saveOpportunity, crudErrors.opportunity, crudFieldErrors.opportunity, opportunities, editOpportunity, canDeleteOpportunity, removeOpportunity, oppStage, setOppStage, opportunityPage, opportunityTotalPages, opportunitySize, onOpportunityPageChange, onOpportunitySizeChange, refreshPipeline, createQuoteFromOpportunity, viewOrdersFromOpportunity, loadOpportunityTimeline, opportunityTimeline, createFollowUpShortcut, createTaskShortcut, urgeApprovalShortcut])

  const mainFollowUps = useMemo(() => ({
    followUpForm, setFollowUpForm, saveFollowUp, formError: crudErrors.followUp, fieldErrors: crudFieldErrors.followUp, followUps, editFollowUp, removeFollowUp, followCustomerId, setFollowCustomerId, followQ, setFollowQ, onRefresh: refreshFollowUps,
  }), [followUpForm, setFollowUpForm, saveFollowUp, crudErrors.followUp, crudFieldErrors.followUp, followUps, editFollowUp, removeFollowUp, followCustomerId, setFollowCustomerId, followQ, setFollowQ, refreshFollowUps])

  const mainContacts = useMemo(() => ({
    contactForm, setContactForm, saveContact, formError: crudErrors.contact, fieldErrors: crudFieldErrors.contact, contacts, editContact, removeContact, contactQ, setContactQ, pagination: { page: contactPage, totalPages: contactTotalPages, size: contactSize }, onPageChange: onContactPageChange, onSizeChange: onContactSizeChange, onRefresh: refreshContacts,
  }), [contactForm, setContactForm, saveContact, crudErrors.contact, crudFieldErrors.contact, contacts, editContact, removeContact, contactQ, setContactQ, contactPage, contactTotalPages, contactSize, onContactPageChange, onContactSizeChange, refreshContacts])

  return {
    mainLeads,
    mainCustomers,
    mainPipeline,
    mainFollowUps,
    mainContacts,
  }
}
