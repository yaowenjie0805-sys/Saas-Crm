import { expect, test } from 'vitest';
import { useRuntimeAuthBranch } from '../hooks/orchestrators/runtime/useRuntimeAuthBranch';

test('shows auth shell whenever auth token is missing', () => {
  const result = useRuntimeAuthBranch({
    hasAuthToken: false,
    isAuthRoute: false,
    sessionBootstrapping: false,
    authShell: {},
  });

  expect(result.showAuthShell).toBe(true);
});

test('hides auth shell when auth token is present', () => {
  const result = useRuntimeAuthBranch({
    hasAuthToken: true,
    isAuthRoute: true,
    sessionBootstrapping: false,
    authShell: {},
  });

  expect(result.showAuthShell).toBe(false);
});
