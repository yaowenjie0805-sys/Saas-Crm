import { useCallback, useState } from 'react'
import { api } from '../../shared'

export function useApiClient() {
  const [activeRequests, setActiveRequests] = useState(0)
  const [error, setError] = useState(null)
  const loading = activeRequests > 0

  const request = useCallback(async (url, options = {}) => {
    setActiveRequests((count) => count + 1)
    setError(null)

    try {
      const path = String(url || '').replace(/^\/api/, '')
      return await api(path, options)
    } catch (err) {
      setError(err.message)
      throw err
    } finally {
      setActiveRequests((count) => (count > 0 ? count - 1 : 0))
    }
  }, [])

  return { request, loading, error, setError }
}
