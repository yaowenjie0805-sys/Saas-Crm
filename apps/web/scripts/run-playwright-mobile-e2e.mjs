import { spawn } from 'node:child_process'

const MOBILE_SPECS = [
  'apps/web/tests/e2e/mobile-responsive.spec.js',
  'apps/web/tests/e2e/mobile-viewport-matrix.spec.js',
]

const child = spawn(process.execPath, ['apps/web/scripts/run-playwright-e2e.mjs'], {
  cwd: process.cwd(),
  stdio: 'inherit',
  env: {
    ...process.env,
    E2E_TEST_GLOB: MOBILE_SPECS.join(','),
  },
})

child.on('error', (err) => {
  console.error(`[mobile-e2e] failed to start: ${err.message}`)
  process.exitCode = 1
})

child.on('close', (code) => {
  process.exitCode = typeof code === 'number' ? code : 1
})

