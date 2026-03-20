import { useMemo } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'

export function useRuntimeRouting({ auth, lang }) {
  const location = useLocation()
  const navigate = useNavigate()

  const perfEnabledByQuery = useMemo(() => {
    const params = new URLSearchParams(location.search || '')
    return params.get('perf') === '1'
  }, [location.search])

  const apiContext = useMemo(() => ({
    token: auth?.token || '',
    lang,
    tenantId: auth?.tenantId || '',
  }), [auth?.token, auth?.tenantId, lang])

  const isAuthRoute = location.pathname === '/login' || location.pathname === '/activate'

  return {
    location,
    navigate,
    perfEnabledByQuery,
    apiContext,
    isAuthRoute,
  }
}
