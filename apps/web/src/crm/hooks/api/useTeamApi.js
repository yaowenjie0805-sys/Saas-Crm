import { useCallback } from 'react'
import { useApiClient } from './useApiClient'
import { requestWithJsonBody, withQueryString } from './utils'

export function useTeamApi() {
  const { request, loading, error } = useApiClient()

  const createTeam = useCallback((data) => requestWithJsonBody(request, '/api/v2/collaboration/teams', data, {
    method: 'POST',
  }), [request])
  const getTeams = useCallback(async (params = {}) => {
    return request(withQueryString('/api/v2/collaboration/teams', params))
  }, [request])
  const getTeamDetail = useCallback(async (teamId) => request(`/api/v2/collaboration/teams/${teamId}`), [request])
  const addTeamMember = useCallback((teamId, data) => requestWithJsonBody(request, `/api/v2/collaboration/teams/${teamId}/members`, data, {
    method: 'POST',
  }), [request])
  const removeTeamMember = useCallback(async (teamId, userId) => request(`/api/v2/collaboration/teams/${teamId}/members/${userId}`, {
    method: 'DELETE',
  }), [request])

  return {
    loading,
    error,
    createTeam,
    getTeams,
    getTeamDetail,
    addTeamMember,
    removeTeamMember,
  }
}
