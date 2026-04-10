import test from 'node:test';
import assert from 'node:assert/strict';
import { parseScopePatterns, runGuard } from './ci-scope-guard.mjs';

test('parseScopePatterns extracts frontend/backend/security patterns', () => {
  const yml = `
filters: |
  frontend:
    - 'apps/web/**'
    - 'scripts/runtime-*.mjs'
  backend:
    - 'apps/api/**'
    - 'scripts/run-maven.mjs'
  security:
    - 'scripts/security-scan.mjs'
`;

  const scopes = parseScopePatterns(yml);
  assert.deepEqual(scopes.frontend, ['apps/web/**', 'scripts/runtime-*.mjs']);
  assert.deepEqual(scopes.backend, ['apps/api/**', 'scripts/run-maven.mjs']);
  assert.deepEqual(scopes.security, ['scripts/security-scan.mjs']);
});

test('runGuard returns no errors for compliant scope config', () => {
  const yml = `
frontend:
  - 'scripts/runtime-*.mjs'
backend:
  - 'scripts/run-maven.mjs'
security:
  - 'scripts/security-scan.mjs'
`;

  const errors = runGuard(yml);
  assert.equal(errors.length, 0);
});

test('runGuard detects forbidden wildcard and missing required patterns', () => {
  const yml = `
frontend:
  - 'scripts/**'
backend:
  - 'apps/api/**'
security:
  - 'apps/web/**'
`;

  const errors = runGuard(yml);
  assert.ok(errors.some((msg) => msg.includes('[forbidden] frontend contains disallowed pattern "scripts/**"')));
  assert.ok(errors.some((msg) => msg.includes('[missing] backend must include required pattern "scripts/run-maven.mjs"')));
  assert.ok(errors.some((msg) => msg.includes('[missing] security must include required pattern "scripts/security-scan.mjs"')));
});
