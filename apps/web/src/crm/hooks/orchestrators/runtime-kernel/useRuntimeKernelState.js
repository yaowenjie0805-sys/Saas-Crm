import { useCallback, useRef } from 'react'
import { PAGE_DOMAIN_MAP } from '../runtime'
import { usePageDataPolicy } from '../../usePageDataPolicy'
import { useNavPerf } from '../../useNavPerf'

/**
 * Runtime kernel-level state and perf instrumentation.
 * Keeps orchestration internals centralized and stable across render cycles.
 */
export function useRuntimeKernelState({
  auth,
  setRecentWorkbenchJump,
  setDomainLoadSource,
}) {
  const seqRef = useRef({ leads: 0, customers: 0, opportunities: 0, contacts: 0, contracts: 0, payments: 0 })
  const logoutGuardRef = useRef(false)
  const suppressLoginErrorUntilRef = useRef(0)
  const pagePolicy = usePageDataPolicy(10000)

  const perf = useNavPerf('[perf]')
  const loadReasonRef = useRef('default')
  const workbenchJumpRef = useRef({ page: '', signature: '' })
  const markWorkbenchJumpMeta = useCallback((meta) => {
    setRecentWorkbenchJump({
      targetPage: String(meta?.targetPage || ''),
      signature: String(meta?.signature || ''),
      reason: String(meta?.reason || 'workbench_jump'),
      hit: !!meta?.hit,
      at: new Date().toISOString(),
    })
  }, [setRecentWorkbenchJump])
  const markDomainLoadSource = useCallback(({ pageKey, reason, event }) => {
    const domain = PAGE_DOMAIN_MAP[pageKey]
    if (!domain) return
    const shortReason = String(reason || 'default')
    const source = `${event}:${pageKey}:${shortReason}`
    const at = new Date().toISOString()
    setDomainLoadSource((prev) => {
      const current = prev[domain]
      if (current?.source === source) return prev
      return {
        ...prev,
        [domain]: { source, at },
      }
    })
  }, [setDomainLoadSource])

  const hasAuthToken = !!String(auth?.token || '').trim()
  const role = auth?.role || ''
  const canWrite = ['ADMIN', 'MANAGER', 'SALES'].includes(role)
  const canDeleteCustomer = ['ADMIN', 'MANAGER'].includes(role)
  const canDeleteOpportunity = ['ADMIN', 'MANAGER'].includes(role)
  const canViewAudit = ['ADMIN', 'MANAGER', 'ANALYST'].includes(role)
  const canViewReports = ['ADMIN', 'MANAGER', 'ANALYST'].includes(role)
  const canViewOpsMetrics = ['ADMIN', 'MANAGER', 'ANALYST'].includes(role)
  const canManagePermissions = role === 'ADMIN'
  const canManageUsers = role === 'ADMIN'
  const canManageSalesAutomation = ['ADMIN', 'MANAGER'].includes(role)

  return {
    seqRef,
    logoutGuardRef,
    suppressLoginErrorUntilRef,
    pagePolicy,
    perf,
    loadReasonRef,
    workbenchJumpRef,
    markWorkbenchJumpMeta,
    markDomainLoadSource,
    hasAuthToken,
    role,
    canWrite,
    canDeleteCustomer,
    canDeleteOpportunity,
    canViewAudit,
    canViewReports,
    canViewOpsMetrics,
    canManagePermissions,
    canManageUsers,
    canManageSalesAutomation,
  }
}
