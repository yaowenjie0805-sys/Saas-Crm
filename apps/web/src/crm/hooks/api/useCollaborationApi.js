import { useCallback } from 'react'
import { useApiClient } from './useApiClient'
import { requestWithJsonBody, withQueryString } from './utils'

export function useCollaborationApi() {
  const { request, loading, error } = useApiClient()

  const addComment = useCallback((data) => requestWithJsonBody(request, '/api/v2/collaboration/comments', data, {
    method: 'POST',
  }), [request])
  const replyToComment = useCallback(
    (commentId, data) => requestWithJsonBody(request, `/api/v2/collaboration/comments/${commentId}/reply`, data, {
      method: 'POST',
    }),
    [request],
  )
  const deleteComment = useCallback(
    async (commentId, userId) => request(withQueryString(`/api/v2/collaboration/comments/${commentId}`, { userId }), {
      method: 'DELETE',
    }),
    [request],
  )
  const likeComment = useCallback(
    async (commentId, userId) => request(withQueryString(`/api/v2/collaboration/comments/${commentId}/like`, { userId }), {
      method: 'POST',
    }),
    [request],
  )
  const editComment = useCallback((commentId, data) => requestWithJsonBody(request, `/api/v2/collaboration/comments/${commentId}`, data, {
    method: 'PUT',
  }), [request])
  const getComments = useCallback(async (entityType, entityId, params = {}) => {
    return request(withQueryString(`/api/v2/collaboration/entities/${entityType}/${entityId}/comments`, params))
  }, [request])
  const getMentions = useCallback(async (userId, params = {}) => {
    return request(withQueryString('/api/v2/collaboration/mentions', { userId, ...params }))
  }, [request])
  const getDiscussions = useCallback(async (userId, limit = 20) => request(withQueryString('/api/v2/collaboration/discussions', { userId, limit })), [request])
  const searchComments = useCallback(async (keyword, params = {}) => {
    return request(withQueryString('/api/v2/collaboration/comments/search', { keyword, ...params }))
  }, [request])
  const getCommentStats = useCallback(async (entityType, entityId) => request(`/api/v2/collaboration/entities/${entityType}/${entityId}/comments/stats`), [request])

  return {
    loading,
    error,
    addComment,
    replyToComment,
    deleteComment,
    likeComment,
    editComment,
    getComments,
    getMentions,
    getDiscussions,
    searchComments,
    getCommentStats,
  }
}
