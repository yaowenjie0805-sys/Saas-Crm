import { lazy, Suspense } from 'react'
import { AppProviders } from '../../../context/AppProviders'
import { useRuntimeAuthBranch } from './useRuntimeAuthBranch.jsx'

const AuthShell = lazy(() => import('../../../components/shell/AuthShell'))
const AppShell = lazy(() => import('../../../components/shell/AppShell'))

function RuntimeShellBranch({
  hasAuthToken,
  isAuthRoute,
  sessionBootstrapping,
  apiContext,
  authShellProps,
  appShellProps,
}) {
  const authShell = <AuthShell {...authShellProps} />
  const { showBootstrapping, showAuthShell } = useRuntimeAuthBranch({
    hasAuthToken,
    isAuthRoute,
    sessionBootstrapping,
    authShell,
  })

  if (showBootstrapping) {
    return <div className="app-bootstrapping">Loading session...</div>
  }

  if (showAuthShell) {
    return (
      <AppProviders apiContext={apiContext}>
        <Suspense fallback={<div className="app-bootstrapping">Loading shell...</div>}>
          {authShell}
        </Suspense>
      </AppProviders>
    )
  }

  return (
    <AppProviders apiContext={apiContext}>
      <Suspense fallback={<div className="app-bootstrapping">Loading shell...</div>}>
        <AppShell {...appShellProps} />
      </Suspense>
    </AppProviders>
  )
}

export default RuntimeShellBranch
