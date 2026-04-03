import { useCallback } from 'react'
import { useApiClient } from './useApiClient'
import { requestWithJsonBody, withQueryString } from './utils'

export function useApprovalApi() {
  const { request, loading, error } = useApiClient()

  const delegateTask = useCallback((taskId, data) => requestWithJsonBody(request, `/api/v2/collaboration/approval/tasks/${taskId}/delegate`, data, {
    method: 'POST',
  }), [request])
  const addSign = useCallback((taskId, data) => requestWithJsonBody(request, `/api/v2/collaboration/approval/tasks/${taskId}/add-sign`, data, {
    method: 'POST',
  }), [request])
  const transferTask = useCallback((taskId, data) => requestWithJsonBody(request, `/api/v2/collaboration/approval/tasks/${taskId}/transfer`, data, {
    method: 'POST',
  }), [request])
  const getDelegationHistory = useCallback(async (taskId) => request(`/api/v2/collaboration/approval/tasks/${taskId}/delegations`), [request])
  const getTransferHistory = useCallback(async (taskId) => request(`/api/v2/collaboration/approval/tasks/${taskId}/transfers`), [request])
  const recallDelegation = useCallback((delegationId, userId) => request(withQueryString(`/api/v2/collaboration/approval/delegations/${delegationId}/recall`, { userId }), {
    method: 'POST',
  }), [request])
  const getDelegatableUsers = useCallback((currentUserId) => request(withQueryString('/api/v2/collaboration/approval/tasks/delegatable-users', { currentUserId })), [request])

  return {
    loading,
    error,
    delegateTask,
    addSign,
    transferTask,
    getDelegationHistory,
    getTransferHistory,
    recallDelegation,
    getDelegatableUsers,
  }
}
