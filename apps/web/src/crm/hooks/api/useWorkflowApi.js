import { useCallback } from 'react'
import { useApiClient } from './useApiClient'
import { requestWithJsonBody, withQueryString } from './utils'

export function useWorkflowApi() {
  const { request, loading, error } = useApiClient()

  const getWorkflows = useCallback(async (params = {}) => {
    return request(withQueryString('/api/v2/workflows', params))
  }, [request])
  const getWorkflowDetail = useCallback(async (workflowId) => request(`/api/v2/workflows/${workflowId}`), [request])
  const createWorkflow = useCallback((data) => requestWithJsonBody(request, '/api/v2/workflows', data, {
    method: 'POST',
  }), [request])
  const updateWorkflow = useCallback((workflowId, data) => requestWithJsonBody(request, `/api/v2/workflows/${workflowId}`, data, {
    method: 'PUT',
  }), [request])
  const deleteWorkflow = useCallback(async (workflowId) => request(`/api/v2/workflows/${workflowId}`, {
    method: 'DELETE',
  }), [request])
  const activateWorkflow = useCallback((workflowId, activatedBy) => requestWithJsonBody(request, `/api/v2/workflows/${workflowId}/activate`, { activatedBy }, {
    method: 'POST',
  }), [request])
  const deactivateWorkflow = useCallback(async (workflowId) => request(`/api/v2/workflows/${workflowId}/deactivate`, {
    method: 'POST',
  }), [request])
  const validateWorkflow = useCallback(async (workflowId) => request(`/api/v2/workflows/${workflowId}/validate`, {
    method: 'POST',
  }), [request])
  const executeWorkflow = useCallback((workflowId, payload) => requestWithJsonBody(request, `/api/v2/workflows/${workflowId}/execute`, payload, {
    method: 'POST',
  }), [request])
  const getExecutionDetail = useCallback(async (executionId) => request(`/api/v2/workflows/executions/${executionId}`), [request])
  const getExecutionHistory = useCallback(async (workflowId, params = {}) => {
    return request(withQueryString(`/api/v2/workflows/${workflowId}/executions`, params))
  }, [request])
  const cancelExecution = useCallback((executionId, cancelledBy) => requestWithJsonBody(request, `/api/v2/workflows/executions/${executionId}/cancel`, { cancelledBy }, {
    method: 'POST',
  }), [request])
  const retryExecution = useCallback(async (executionId) => request(`/api/v2/workflows/executions/${executionId}/retry`, {
    method: 'POST',
  }), [request])
  const getNodeTypes = useCallback(async () => request('/api/v2/workflows/node-types'), [request])

  return {
    loading,
    error,
    getWorkflows,
    getWorkflowDetail,
    createWorkflow,
    updateWorkflow,
    deleteWorkflow,
    activateWorkflow,
    deactivateWorkflow,
    validateWorkflow,
    executeWorkflow,
    getExecutionDetail,
    getExecutionHistory,
    cancelExecution,
    retryExecution,
    getNodeTypes,
  }
}
