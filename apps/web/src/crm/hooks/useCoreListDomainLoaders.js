import { useCallback } from 'react'
import { api } from '../shared'

export function useCoreListDomainLoaders({
  authToken,
  lang,
  seqRef,
  beginPageRequest,
  _handleError,

  leadQ,
  leadStatus,
  leadPage,
  leadSize,
  setLeads,
  setLeadPage,
  setLeadTotalPages,
  setLeadSize,

  customerQ,
  customerStatus,
  customerPage,
  customerSize,
  setCustomers,
  setCustomerPage,
  setCustomerTotalPages,
  setCustomerSize,

  oppStage,
  opportunityPage,
  opportunitySize,
  setOpportunities,
  setOpportunityPage,
  setOpportunityTotalPages,
  setOpportunitySize,

  contactQ,
  contactPage,
  contactSize,
  setContacts,
  setContactPage,
  setContactTotalPages,
  setContactSize,

  contractQ,
  contractStatus,
  contractPage,
  contractSize,
  setContracts,
  setContractPage,
  setContractTotalPages,
  setContractSize,

  paymentStatus,
  paymentPage,
  paymentSize,
  setPayments,
  setPaymentPage,
  setPaymentTotalPages,
  setPaymentSize,
}) {
  const loadLeads = useCallback(async (page = leadPage, size = leadSize, options = {}) => {
    const reqId = ++seqRef.current.leads
    const q = new URLSearchParams({ q: leadQ, status: leadStatus, page: String(page), size: String(size) })
    const signal = options.signal || beginPageRequest('leads').signal
    const d = await api('/v1/leads?' + q, { signal }, authToken, lang)
    if (reqId !== seqRef.current.leads) return
    setLeads(d.items || [])
    setLeadPage(d.page || page)
    setLeadTotalPages(Math.max(1, d.totalPages || 1))
  }, [leadPage, leadSize, seqRef, leadQ, leadStatus, beginPageRequest, authToken, lang, setLeads, setLeadPage, setLeadTotalPages])

  const loadCustomers = useCallback(async (page = customerPage, size = customerSize, options = {}) => {
    const reqId = ++seqRef.current.customers
    const q = new URLSearchParams({ q: customerQ, status: customerStatus, page: String(page), size: String(size) })
    const signal = options.signal || beginPageRequest('customers').signal
    const d = await api('/customers/search?' + q, { signal }, authToken, lang)
    if (reqId !== seqRef.current.customers) return
    setCustomers(d.items || [])
    setCustomerPage(d.page || page)
    setCustomerTotalPages(Math.max(1, d.totalPages || 1))
  }, [customerPage, customerSize, seqRef, customerQ, customerStatus, beginPageRequest, authToken, lang, setCustomers, setCustomerPage, setCustomerTotalPages])

  const loadOpportunities = useCallback(async (page = opportunityPage, size = opportunitySize) => {
    const reqId = ++seqRef.current.opportunities
    const q = new URLSearchParams({ stage: oppStage, page: String(page), size: String(size) })
    const controller = beginPageRequest('pipeline')
    const d = await api('/opportunities/search?' + q, { signal: controller.signal }, authToken, lang)
    if (reqId !== seqRef.current.opportunities) return
    setOpportunities(d.items || [])
    setOpportunityPage(d.page || page)
    setOpportunityTotalPages(Math.max(1, d.totalPages || 1))
  }, [opportunityPage, opportunitySize, seqRef, oppStage, beginPageRequest, authToken, lang, setOpportunities, setOpportunityPage, setOpportunityTotalPages])

  const loadContacts = useCallback(async (page = contactPage, size = contactSize) => {
    const reqId = ++seqRef.current.contacts
    const q = new URLSearchParams({ customerId: '', q: contactQ, page: String(page), size: String(size) })
    const controller = beginPageRequest('contacts')
    const d = await api('/contacts/search?' + q, { signal: controller.signal }, authToken, lang)
    if (reqId !== seqRef.current.contacts) return
    setContacts(d.items || [])
    setContactPage(d.page || page)
    setContactTotalPages(Math.max(1, d.totalPages || 1))
  }, [contactPage, contactSize, seqRef, contactQ, beginPageRequest, authToken, lang, setContacts, setContactPage, setContactTotalPages])

  const loadContracts = useCallback(async (page = contractPage, size = contractSize) => {
    const reqId = ++seqRef.current.contracts
    const q = new URLSearchParams({ customerId: '', status: contractStatus, q: contractQ, page: String(page), size: String(size) })
    const controller = beginPageRequest('contracts')
    const d = await api('/contracts/search?' + q, { signal: controller.signal }, authToken, lang)
    if (reqId !== seqRef.current.contracts) return
    setContracts(d.items || [])
    setContractPage(d.page || page)
    setContractTotalPages(Math.max(1, d.totalPages || 1))
  }, [contractPage, contractSize, seqRef, contractStatus, contractQ, beginPageRequest, authToken, lang, setContracts, setContractPage, setContractTotalPages])

  const loadPayments = useCallback(async (page = paymentPage, size = paymentSize) => {
    const reqId = ++seqRef.current.payments
    const q = new URLSearchParams({ customerId: '', contractId: '', status: paymentStatus, page: String(page), size: String(size) })
    const controller = beginPageRequest('payments')
    const d = await api('/payments/search?' + q, { signal: controller.signal }, authToken, lang)
    if (reqId !== seqRef.current.payments) return
    setPayments(d.items || [])
    setPaymentPage(d.page || page)
    setPaymentTotalPages(Math.max(1, d.totalPages || 1))
  }, [paymentPage, paymentSize, seqRef, paymentStatus, beginPageRequest, authToken, lang, setPayments, setPaymentPage, setPaymentTotalPages])

  const onLeadPageChange = useCallback(async (next) => {
    if (!authToken) return
    setLeadPage(next)
  }, [authToken, setLeadPage])
  const onCustomerPageChange = useCallback(async (next) => {
    if (!authToken) return
    setCustomerPage(next)
  }, [authToken, setCustomerPage])
  const onOpportunityPageChange = useCallback((next) => {
    if (!authToken) return
    setOpportunityPage(next)
  }, [authToken, setOpportunityPage])
  const onContactPageChange = useCallback((next) => {
    if (!authToken) return
    setContactPage(next)
  }, [authToken, setContactPage])
  const onContractPageChange = useCallback((next) => {
    if (!authToken) return
    setContractPage(next)
  }, [authToken, setContractPage])
  const onPaymentPageChange = useCallback((next) => {
    if (!authToken) return
    setPaymentPage(next)
  }, [authToken, setPaymentPage])

  const onLeadSizeChange = useCallback(async (nextSize) => {
    if (!authToken) return
    setLeadSize(nextSize)
    setLeadPage(1)
  }, [authToken, setLeadSize, setLeadPage])
  const onCustomerSizeChange = useCallback(async (nextSize) => {
    if (!authToken) return
    setCustomerSize(nextSize)
    setCustomerPage(1)
  }, [authToken, setCustomerSize, setCustomerPage])
  const onOpportunitySizeChange = useCallback((nextSize) => {
    if (!authToken) return
    setOpportunitySize(nextSize)
    setOpportunityPage(1)
  }, [authToken, setOpportunitySize, setOpportunityPage])
  const onContactSizeChange = useCallback((nextSize) => {
    if (!authToken) return
    setContactSize(nextSize)
    setContactPage(1)
  }, [authToken, setContactSize, setContactPage])
  const onContractSizeChange = useCallback((nextSize) => {
    if (!authToken) return
    setContractSize(nextSize)
    setContractPage(1)
  }, [authToken, setContractSize, setContractPage])
  const onPaymentSizeChange = useCallback((nextSize) => {
    if (!authToken) return
    setPaymentSize(nextSize)
    setPaymentPage(1)
  }, [authToken, setPaymentSize, setPaymentPage])

  return {
    loaders: {
      loadLeads,
      loadCustomers,
      loadOpportunities,
      loadContacts,
      loadContracts,
      loadPayments,
    },
    paginationActions: {
      onLeadPageChange,
      onCustomerPageChange,
      onOpportunityPageChange,
      onContactPageChange,
      onContractPageChange,
      onPaymentPageChange,
      onLeadSizeChange,
      onCustomerSizeChange,
      onOpportunitySizeChange,
      onContactSizeChange,
      onContractSizeChange,
      onPaymentSizeChange,
    },
    filters: {
      leadQ,
      leadStatus,
      customerQ,
      customerStatus,
      oppStage,
      contactQ,
      contractQ,
      contractStatus,
      paymentStatus,
    },
  }
}
