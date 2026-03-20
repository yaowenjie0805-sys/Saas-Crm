import { useAppStore } from '../../store/appStore'
import { shallow } from 'zustand/shallow'

const EMPTY_OBJECT = Object.freeze({})
const NOOP = () => {}
const FALLBACK_BASE_MODEL = Object.freeze({
  currentPageLabel: '',
  lang: 'en',
  setLang: NOOP,
  refreshPage: NOOP,
  t: (key) => String(key || ''),
  canWrite: false,
  role: '',
  error: '',
  loading: false,
  activePage: 'dashboard',
  stats: [],
  reports: [],
  workbenchToday: {},
  quoteOpportunityFilter: '',
  orderOpportunityFilter: '',
  quotePrefill: null,
  consumeQuotePrefill: NOOP,
  auth: {},
  onLogout: NOOP,
})

const selectAuth = (state) => state.auth || {}
const selectCustomerDomain = (state) => state.customerDomain || {}
const selectCommerceDomain = (state) => state.commerceDomain || {}
const selectGovernanceDomain = (state) => state.governanceDomain || {}
const selectApprovalDomain = (state) => state.approvalDomain || {}
const selectReportingDomain = (state) => state.reportingDomain || {}

export function useBasePageModel() {
  return useAppStore((state) => selectAuth(state)?.data?.baseModel || FALLBACK_BASE_MODEL, shallow)
}

export function usePermissionsPageModel() {
  return useAppStore((state) => selectGovernanceDomain(state).permissions || EMPTY_OBJECT, shallow)
}

export function useUsersPageModel() {
  return useAppStore((state) => selectGovernanceDomain(state).users || EMPTY_OBJECT, shallow)
}

export function useSalesAutomationPageModel() {
  return useAppStore((state) => selectGovernanceDomain(state).salesAutomation || EMPTY_OBJECT, shallow)
}

export function useCommercePageModel() {
  return useAppStore((state) => selectCommerceDomain(state).commerce || EMPTY_OBJECT, shallow)
}

export function useLeadsPageModel() {
  return useAppStore((state) => selectCustomerDomain(state).leads || EMPTY_OBJECT, shallow)
}

export function useCustomersPageModel() {
  return useAppStore((state) => selectCustomerDomain(state).customers || EMPTY_OBJECT, shallow)
}

export function usePipelinePageModel() {
  return useAppStore((state) => selectCustomerDomain(state).pipeline || EMPTY_OBJECT, shallow)
}

export function useFollowUpsPageModel() {
  return useAppStore((state) => selectCustomerDomain(state).followUps || EMPTY_OBJECT, shallow)
}

export function useContactsPageModel() {
  return useAppStore((state) => selectCustomerDomain(state).contacts || EMPTY_OBJECT, shallow)
}

export function useContractsPageModel() {
  return useAppStore((state) => selectCommerceDomain(state).contracts || EMPTY_OBJECT, shallow)
}

export function usePaymentsPageModel() {
  return useAppStore((state) => selectCommerceDomain(state).payments || EMPTY_OBJECT, shallow)
}

export function useReportDesignerPageModel() {
  return useAppStore((state) => selectCommerceDomain(state).reportDesigner || EMPTY_OBJECT, shallow)
}

export function useTasksPageModel() {
  return useAppStore((state) => selectCustomerDomain(state).tasks || EMPTY_OBJECT, shallow)
}

export function useAuditPageModel() {
  return useAppStore((state) => selectReportingDomain(state).audit || EMPTY_OBJECT, shallow)
}

export function useApprovalsPageModel() {
  return useAppStore((state) => selectApprovalDomain(state).approvals || EMPTY_OBJECT, shallow)
}

export function useTenantsPageModel() {
  return useAppStore((state) => selectGovernanceDomain(state).tenants || EMPTY_OBJECT, shallow)
}
