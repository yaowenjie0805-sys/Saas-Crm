export function useRuntimeAuthBranch({
  hasAuthToken,
  isAuthRoute,
  sessionBootstrapping,
  authShell,
}) {
  return {
    authShell,
    showBootstrapping: sessionBootstrapping && !hasAuthToken && !isAuthRoute,
    showAuthShell: !hasAuthToken || isAuthRoute,
  }
}
