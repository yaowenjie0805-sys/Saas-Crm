import { useEffect, useRef } from 'react'
import { FILTERS_KEY } from '../../../shared'
import { normalizeRuntimePageSizeValue } from './useRuntimeFilterPersistenceUtils'

const PERSISTENCE_DEBOUNCE_MS = 120

const safeSetLocalStorage = (key, value) => {
  try {
    localStorage.setItem(key, value)
  } catch {
    // ignore storage write failures
  }
}

const safeGetLocalStorage = (key) => {
  try {
    if (typeof window === 'undefined' || !window.localStorage) return ''
    return localStorage.getItem(key) || ''
  } catch {
    return ''
  }
}

export function useRuntimeFilterPersistenceEffects({
  leadQ,
  leadStatus,
  customerQ,
  customerStatus,
  oppStage,
  followCustomerId,
  followQ,
  contactQ,
  contractQ,
  contractStatus,
  paymentStatus,
  auditUser,
  auditRole,
  auditAction,
  auditFrom,
  auditTo,
  reportOwner,
  reportDepartment,
  reportTimezone,
  reportCurrency,
  customerSize,
  leadSize,
  opportunitySize,
  contactSize,
  contractSize,
  paymentSize,
  notificationSize,
  leadImportSize,
  exportJobsSize,
  reportExportJobsSize,
  leadImportExportSize,
}) {
  const initialFiltersValue = safeGetLocalStorage(FILTERS_KEY)
  const persistedFiltersValueRef = useRef(initialFiltersValue)
  const pendingFiltersValueRef = useRef(initialFiltersValue)
  const filtersWriteTimerRef = useRef(null)
  const persistedPageSizeValuesRef = useRef({
    customers: safeGetLocalStorage('crm_page_size_customers'),
    leads: safeGetLocalStorage('crm_page_size_leads'),
    opportunities: safeGetLocalStorage('crm_page_size_opportunities'),
    contacts: safeGetLocalStorage('crm_page_size_contacts'),
    contracts: safeGetLocalStorage('crm_page_size_contracts'),
    payments: safeGetLocalStorage('crm_page_size_payments'),
    notificationJobs: safeGetLocalStorage('crm_page_size_notification_jobs'),
    leadImportJobs: safeGetLocalStorage('crm_page_size_lead_import_jobs'),
    auditExportJobs: safeGetLocalStorage('crm_page_size_audit_export_jobs'),
    reportExportJobs: safeGetLocalStorage('crm_page_size_report_export_jobs'),
    leadImportExportJobs: safeGetLocalStorage('crm_page_size_lead_import_export_jobs'),
  })

  useEffect(() => () => {
    if (filtersWriteTimerRef.current) {
      clearTimeout(filtersWriteTimerRef.current)
      filtersWriteTimerRef.current = null
    }
    if (pendingFiltersValueRef.current !== persistedFiltersValueRef.current) {
      safeSetLocalStorage(FILTERS_KEY, pendingFiltersValueRef.current)
      persistedFiltersValueRef.current = pendingFiltersValueRef.current
    }
  }, [])

  useEffect(() => {
    const nextFiltersValue = JSON.stringify({
      leadQ,
      leadStatus,
      customerQ,
      customerStatus,
      oppStage,
      followCustomerId,
      followQ,
      contactQ,
      contractQ,
      contractStatus,
      paymentStatus,
      auditUser,
      auditRole,
      auditAction,
      auditFrom,
      auditTo,
      reportOwner,
      reportDepartment,
      reportTimezone,
      reportCurrency,
    })
    if (nextFiltersValue === persistedFiltersValueRef.current) return
    pendingFiltersValueRef.current = nextFiltersValue
    if (filtersWriteTimerRef.current) clearTimeout(filtersWriteTimerRef.current)
    filtersWriteTimerRef.current = setTimeout(() => {
      filtersWriteTimerRef.current = null
      if (pendingFiltersValueRef.current === persistedFiltersValueRef.current) return
      safeSetLocalStorage(FILTERS_KEY, pendingFiltersValueRef.current)
      persistedFiltersValueRef.current = pendingFiltersValueRef.current
    }, PERSISTENCE_DEBOUNCE_MS)
    return () => {
      if (filtersWriteTimerRef.current) {
        clearTimeout(filtersWriteTimerRef.current)
        filtersWriteTimerRef.current = null
      }
    }
  }, [
    leadQ,
    leadStatus,
    customerQ,
    customerStatus,
    oppStage,
    followCustomerId,
    followQ,
    contactQ,
    contractQ,
    contractStatus,
    paymentStatus,
    auditUser,
    auditRole,
    auditAction,
    auditFrom,
    auditTo,
    reportOwner,
    reportDepartment,
    reportTimezone,
    reportCurrency,
  ])

  useEffect(() => {
    const nextPageSizeValues = {
      customers: normalizeRuntimePageSizeValue(customerSize),
      leads: normalizeRuntimePageSizeValue(leadSize),
      opportunities: normalizeRuntimePageSizeValue(opportunitySize),
      contacts: normalizeRuntimePageSizeValue(contactSize),
      contracts: normalizeRuntimePageSizeValue(contractSize),
      payments: normalizeRuntimePageSizeValue(paymentSize),
      notificationJobs: normalizeRuntimePageSizeValue(notificationSize),
      leadImportJobs: normalizeRuntimePageSizeValue(leadImportSize),
      auditExportJobs: normalizeRuntimePageSizeValue(exportJobsSize),
      reportExportJobs: normalizeRuntimePageSizeValue(reportExportJobsSize),
      leadImportExportJobs: normalizeRuntimePageSizeValue(leadImportExportSize),
    }
    const storageEntries = [
      ['customers', 'crm_page_size_customers'],
      ['leads', 'crm_page_size_leads'],
      ['opportunities', 'crm_page_size_opportunities'],
      ['contacts', 'crm_page_size_contacts'],
      ['contracts', 'crm_page_size_contracts'],
      ['payments', 'crm_page_size_payments'],
      ['notificationJobs', 'crm_page_size_notification_jobs'],
      ['leadImportJobs', 'crm_page_size_lead_import_jobs'],
      ['auditExportJobs', 'crm_page_size_audit_export_jobs'],
      ['reportExportJobs', 'crm_page_size_report_export_jobs'],
      ['leadImportExportJobs', 'crm_page_size_lead_import_export_jobs'],
    ]
    for (const [valueKey, storageKey] of storageEntries) {
      const nextValue = nextPageSizeValues[valueKey]
      if (nextValue === null) continue
      if (persistedPageSizeValuesRef.current[valueKey] === nextValue) continue
      safeSetLocalStorage(storageKey, nextValue)
      persistedPageSizeValuesRef.current[valueKey] = nextValue
    }
  }, [
    leadSize,
    customerSize,
    opportunitySize,
    contactSize,
    contractSize,
    paymentSize,
    notificationSize,
    leadImportSize,
    exportJobsSize,
    reportExportJobsSize,
    leadImportExportSize,
  ])
}
