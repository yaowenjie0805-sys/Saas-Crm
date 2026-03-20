import { useCallback } from 'react'

export function useWorkbenchNavigation({
  activePage,
  setActivePage,
  navigate,
  pageToPath,
  markNavStart,
  markWorkbenchJumpDecision,
  markWorkbenchActionResult,
  loadReasonRef,
  setPaymentStatus,
  setContractStatus,
  setApprovalTaskStatus,
  setFollowCustomerId,
  setFollowQ,
  setFollowUpForm,
  setQuoteOpportunityFilter,
  setQuoteStatusFilter,
  setQuoteOwnerFilter,
  setQuotePrefill,
  setOrderOpportunityFilter,
  setOrderStatusFilter,
  setOrderOwnerFilter,
  setLeadQ,
  setLeadStatus,
  setLeadPage,
  setLeadSize,
  leadSize,
  setCustomerQ,
  setCustomerStatus,
  setCustomerPage,
  setCustomerSize,
  customerSize,
  workbenchJumpRef,
  onWorkbenchJumpMeta,
}) {

  const trackWorkbenchEvent = useCallback((event, payload = {}) => {
    if (typeof window !== 'undefined' && window.console?.debug) {
      window.console.debug('[workbench]', event, payload)
    }
  }, [])

  const buildWorkbenchFilterSignature = useCallback((targetPage, payload = {}) => {
    const normalizedPage = Number(payload.page || 1)
    const normalizedSize = Number(payload.size || (targetPage === 'leads'
      ? (leadSize || 10)
      : targetPage === 'customers'
        ? (customerSize || 10)
        : 50))
    const normalized = {
      targetPage: targetPage || '',
      status: String(payload.status || ''),
      owner: String(payload.owner || ''),
      customerId: String(payload.customerId || ''),
      opportunityId: String(payload.opportunityId || ''),
      q: String(payload.q || ''),
      department: String(payload.department || ''),
      timezone: String(payload.timezone || ''),
      from: String(payload.from || ''),
      to: String(payload.to || ''),
      page: Number.isFinite(normalizedPage) && normalizedPage > 0 ? Math.floor(normalizedPage) : 1,
      size: Number.isFinite(normalizedSize) && normalizedSize > 0 ? Math.floor(normalizedSize) : 10,
    }
    return JSON.stringify(normalized)
  }, [customerSize, leadSize])

  const navigateToWorkbenchTarget = useCallback((targetPage, payload = {}) => {
    if (!targetPage || !pageToPath[targetPage]) {
      markWorkbenchActionResult?.(false)
      return
    }
    const signature = buildWorkbenchFilterSignature(targetPage, payload)
    const repeatedJump = workbenchJumpRef.current.page === targetPage
      && workbenchJumpRef.current.signature === signature
      && activePage === targetPage
    workbenchJumpRef.current = { page: targetPage, signature }
    if (repeatedJump) {
      markWorkbenchJumpDecision(true)
      markWorkbenchActionResult?.(true)
      onWorkbenchJumpMeta?.({
        targetPage,
        signature,
        reason: 'workbench_jump',
        hit: true,
      })
      return
    }
    trackWorkbenchEvent('navigate', { targetPage, ...payload })
    if (targetPage === 'payments' && payload.status) setPaymentStatus(payload.status)
    if (targetPage === 'contracts' && payload.status) setContractStatus(payload.status)
    if (targetPage === 'approvals' && payload.status) setApprovalTaskStatus(payload.status)
    if (targetPage === 'followUps') {
      if (payload.customerId) setFollowCustomerId(payload.customerId)
      if (payload.q) setFollowQ(payload.q)
      setFollowUpForm((prev) => ({ ...prev, customerId: payload.customerId || prev.customerId, summary: payload.q || prev.summary }))
    }
    if (targetPage === 'leads') {
      const nextQ = String(payload.q || payload.owner || '')
      const nextStatus = String(payload.status || '')
      const nextSize = Number(payload.size || leadSize || 10)
      setLeadQ(nextQ)
      setLeadStatus(nextStatus)
      setLeadPage(1)
      setLeadSize(Number.isFinite(nextSize) && nextSize > 0 ? Math.floor(nextSize) : 10)
    }
    if (targetPage === 'customers') {
      const nextQ = String(payload.q || payload.owner || '')
      const nextStatus = String(payload.status || '')
      const nextSize = Number(payload.size || customerSize || 10)
      setCustomerQ(nextQ)
      setCustomerStatus(nextStatus)
      setCustomerPage(1)
      setCustomerSize(Number.isFinite(nextSize) && nextSize > 0 ? Math.floor(nextSize) : 10)
    }
    if (targetPage === 'quotes') {
      const nextOpportunity = String(payload.opportunityId || '')
      setQuoteOpportunityFilter(nextOpportunity)
      setQuoteStatusFilter(String(payload.status || ''))
      setQuoteOwnerFilter(String(payload.owner || ''))
      setQuotePrefill({ opportunityId: nextOpportunity, owner: payload.owner || '', customerId: payload.customerId || '' })
    }
    if (targetPage === 'orders') {
      setOrderOpportunityFilter(String(payload.opportunityId || ''))
      setOrderStatusFilter(String(payload.status || ''))
      setOrderOwnerFilter(String(payload.owner || ''))
    }
    markNavStart(activePage, targetPage)
    loadReasonRef.current = 'workbench_jump'
    setActivePage(targetPage)
    navigate(pageToPath[targetPage])
    markWorkbenchActionResult?.(true)
    onWorkbenchJumpMeta?.({
      targetPage,
      signature,
      reason: 'workbench_jump',
      hit: false,
    })
  }, [pageToPath, buildWorkbenchFilterSignature, activePage, markWorkbenchJumpDecision, markWorkbenchActionResult, trackWorkbenchEvent, setPaymentStatus, setContractStatus, setApprovalTaskStatus, setFollowCustomerId, setFollowQ, setFollowUpForm, setQuoteOpportunityFilter, setQuoteStatusFilter, setQuoteOwnerFilter, setQuotePrefill, setOrderOpportunityFilter, setOrderStatusFilter, setOrderOwnerFilter, setLeadQ, setLeadStatus, setLeadPage, setLeadSize, leadSize, setCustomerQ, setCustomerStatus, setCustomerPage, setCustomerSize, customerSize, markNavStart, loadReasonRef, setActivePage, navigate, workbenchJumpRef, onWorkbenchJumpMeta])

  const createQuoteFromOpportunity = useCallback((row) => {
    if (!row?.id) { markWorkbenchActionResult?.(false); return }
    navigateToWorkbenchTarget('quotes', { opportunityId: row.id, owner: row.owner || '', customerId: '' })
  }, [markWorkbenchActionResult, navigateToWorkbenchTarget])

  const viewOrdersFromOpportunity = useCallback((row) => {
    if (!row?.id) { markWorkbenchActionResult?.(false); return }
    navigateToWorkbenchTarget('orders', { opportunityId: row.id })
  }, [markWorkbenchActionResult, navigateToWorkbenchTarget])

  const createFollowUpShortcut = useCallback((row) => {
    const customerId = row?.customerId || row?.id
    if (!customerId) { markWorkbenchActionResult?.(false); return }
    setFollowCustomerId(customerId)
    setFollowQ('')
    setFollowUpForm({ id: '', customerId, summary: '', channel: '', result: '', nextActionDate: '' })
    navigateToWorkbenchTarget('followUps', { customerId })
  }, [markWorkbenchActionResult, setFollowCustomerId, setFollowQ, setFollowUpForm, navigateToWorkbenchTarget])

  const createTaskShortcut = useCallback((row) => {
    navigateToWorkbenchTarget('tasks', { owner: row?.owner || '' })
  }, [navigateToWorkbenchTarget])

  const urgeApprovalShortcut = useCallback((row) => {
    navigateToWorkbenchTarget('approvals', { status: 'PENDING', opportunityId: row?.id || '' })
  }, [navigateToWorkbenchTarget])

  return {
    trackWorkbenchEvent,
    buildWorkbenchFilterSignature,
    navigateToWorkbenchTarget,
    createQuoteFromOpportunity,
    viewOrdersFromOpportunity,
    createFollowUpShortcut,
    createTaskShortcut,
    urgeApprovalShortcut,
  }
}
