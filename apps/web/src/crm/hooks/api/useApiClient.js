import { useCallback, useState } from 'react'
import { api } from '../../shared'
import { useApiContext } from '../../context/ApiContext'

export function useApiClient() {
  const { token, lang, tenantId } = useApiContext()
  const [activeRequests, setActiveRequests] = useState(0)
  const [error, setError] = useState(null)
  const loading = activeRequests > 0

  const request = useCallback(async (url, options = {}) => {
    setActiveRequests((count) => count + 1)
    setError(null)

    try {
      const path = String(url || '').replace(/^\/api/, '')
      const hasApiContext = Boolean(token || tenantId || (lang && lang !== 'en'))
      let requestOptions = options
      if (tenantId) {
        const headers = { ...(options.headers || {}) }
        if (!headers['X-Tenant-Id'] && !headers['x-tenant-id']) {
          headers['X-Tenant-Id'] = tenantId
        }
        requestOptions = { ...options, headers }
      }
      if (hasApiContext) return await api(path, requestOptions, token, lang || 'en')
      return await api(path, requestOptions)
    } catch (err) {
      setError(err.message)
      throw err
    } finally {
      setActiveRequests((count) => (count > 0 ? count - 1 : 0))
    }
  }, [lang, tenantId, token])

  return { request, loading, error, setError }
}
