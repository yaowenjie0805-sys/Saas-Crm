import { useEffect } from 'react'
import { PAGE_TO_PATH } from './routeConfig'

export function useRuntimeRouteGuardEffect({
  authToken,
  locationPathname,
  navigate,
  sessionBootstrapping,
}) {
  useEffect(() => {
    const onAuthRoute = locationPathname === '/login' || locationPathname === '/activate'
    if (sessionBootstrapping) return
    if (!authToken && !onAuthRoute) {
      navigate('/login', { replace: true })
      return
    }
    if (authToken && onAuthRoute) {
      navigate(PAGE_TO_PATH.dashboard, { replace: true })
      return
    }
    if (authToken && locationPathname === '/') {
      navigate(PAGE_TO_PATH.dashboard, { replace: true })
    }
  }, [authToken, locationPathname, navigate, sessionBootstrapping])
}
