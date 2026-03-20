import { useCallback } from 'react'
import { useAppCrudActions } from '../../useAppCrudActions'
import { PAGE_TO_PATH } from './routeConfig'

export function useRuntimeCrudDomainActions({
  auth,
  lang,
  t,
  canWrite,
  canDeleteCustomer,
  canDeleteOpportunity,
  handleError,
  validateCustomerForm,
  validateContactForm,
  validateContractForm,
  validatePaymentForm,
  leadForm,
  setLeadForm,
  leads,
  customerForm,
  setCustomerForm,
  customerPage,
  setCustomerPage,
  customers,
  opportunityForm,
  setOpportunityForm,
  opportunityPage,
  setOpportunityPage,
  opportunities,
  followUpForm,
  setFollowUpForm,
  contactForm,
  setContactForm,
  contactPage,
  setContactPage,
  contacts,
  contractForm,
  setContractForm,
  contractPage,
  setContractPage,
  contracts,
  paymentForm,
  setPaymentForm,
  paymentPage,
  setPaymentPage,
  payments,
  refreshPage,
  setQuotePrefill,
  activePage,
  markNavStart,
  loadReasonRef,
  setActivePage,
  navigate,
  setQuoteOpportunityFilter,
  setCrudErrors,
  setCrudFieldErrors,
}) {
  const formatValidation = (err) => {
    const ve = err?.validationErrors
    if (!ve || typeof ve !== 'object') return err.message
    const first = Object.entries(ve)[0]
    return first ? `${first[0]}: ${first[1]}` : err.message
  }
  const setCrudError = (key, msg) => setCrudErrors((prev) => ({ ...prev, [key]: msg || '' }))
  const setCrudFieldError = (key, fields) => setCrudFieldErrors((prev) => ({ ...prev, [key]: fields || {} }))
  const pickFieldErrors = (err, allowed) => {
    const ve = err?.validationErrors
    if (!ve || typeof ve !== 'object') return {}
    const out = {}
    allowed.forEach((f) => {
      if (ve[f]) out[f] = ve[f]
    })
    return out
  }

  const jumpToQuoteAfterLeadConvert = useCallback(
    (prefill = {}) => {
      const nextOpportunityId = String(prefill?.opportunityId || '')
      setQuoteOpportunityFilter(nextOpportunityId)
      markNavStart(activePage, 'quotes')
      loadReasonRef.current = 'workbench_jump'
      setActivePage('quotes')
      navigate(PAGE_TO_PATH.quotes)
    },
    [activePage, markNavStart, navigate, setActivePage, setQuoteOpportunityFilter, loadReasonRef],
  )

  return useAppCrudActions({
    authToken: auth?.token,
    lang,
    t,
    canWrite,
    canDeleteCustomer,
    canDeleteOpportunity,
    handleError,
    setCrudError,
    setCrudFieldError,
    pickFieldErrors,
    formatValidation,
    validateCustomerForm,
    validateContactForm,
    validateContractForm,
    validatePaymentForm,
    leadForm,
    setLeadForm,
    leads,
    customerForm,
    setCustomerForm,
    customerPage,
    setCustomerPage,
    customers,
    opportunityForm,
    setOpportunityForm,
    opportunityPage,
    setOpportunityPage,
    opportunities,
    followUpForm,
    setFollowUpForm,
    contactForm,
    setContactForm,
    contactPage,
    setContactPage,
    contacts,
    contractForm,
    setContractForm,
    contractPage,
    setContractPage,
    contracts,
    paymentForm,
    setPaymentForm,
    paymentPage,
    setPaymentPage,
    payments,
    refreshPage,
    setQuotePrefill,
    onLeadConvertedToQuote: jumpToQuoteAfterLeadConvert,
  })
}
