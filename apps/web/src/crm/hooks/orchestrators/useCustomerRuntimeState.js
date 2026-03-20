import { useMemo } from 'react'
import { useRuntimeSectionFields } from './useRuntimeSectionFields'

export function useCustomerRuntimeState({ persisted, readPageSize }) {
  const defaults = useMemo(() => ({
    stats: [],
    leads: [],
    customers: [],
    tasks: [],
    opportunities: [],
    followUps: [],
    contacts: [],
    workbenchToday: null,
    customerTimeline: [],
    opportunityTimeline: [],
    customerQ: persisted.customerQ || '',
    leadQ: persisted.leadQ || '',
    leadStatus: persisted.leadStatus || '',
    customerStatus: persisted.customerStatus || '',
    oppStage: persisted.oppStage || '',
    followCustomerId: persisted.followCustomerId || '',
    followQ: persisted.followQ || '',
    customerForm: { id: '', name: '', owner: '', status: '', tag: '', value: '' },
    leadForm: { id: '', name: '', company: '', phone: '', email: '', status: 'NEW', owner: '', source: '' },
    opportunityForm: { id: '', stage: '', count: '', amount: '', progress: '', owner: '' },
    followUpForm: { id: '', customerId: '', summary: '', channel: '', result: '', nextActionDate: '' },
    contactForm: { id: '', customerId: '', name: '', title: '', phone: '', email: '' },
    customerPage: 1,
    leadPage: 1,
    leadTotalPages: 1,
    leadSize: () => readPageSize('crm_page_size_leads'),
    customerTotalPages: 1,
    customerSize: () => readPageSize('crm_page_size_customers'),
    opportunityPage: 1,
    opportunityTotalPages: 1,
    opportunitySize: () => readPageSize('crm_page_size_opportunities'),
    contactQ: persisted.contactQ || '',
    contactPage: 1,
    contactTotalPages: 1,
    contactSize: () => readPageSize('crm_page_size_contacts'),
  }), [persisted, readPageSize])

  return useRuntimeSectionFields('customerDomain', 'ui', defaults)
}
