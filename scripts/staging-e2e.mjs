import { spawn } from 'node:child_process'

const isWin = process.platform === 'win32'
const rawBaseUrl = process.env.STAGING_BASE_URL || ''
const baseUrl = rawBaseUrl.trim().replace(/\/+$/, '')

if (!baseUrl) {
  console.error('[staging:e2e] STAGING_BASE_URL is required')
  process.exit(1)
}

const apiBaseUrl = process.env.E2E_API_BASE_URL || `${baseUrl}/api`
const timeoutMs = Math.max(60_000, Number(process.env.STAGING_E2E_TIMEOUT_MS || 15 * 60 * 1000))
const e2eTestGlob = process.env.E2E_TEST_GLOB || ''
const e2eTestFiles = e2eTestGlob
  .split(',')
  .map((item) => item.trim())
  .filter(Boolean)

const args = ['playwright', 'test', '--config', 'apps/web/playwright.config.js', ...e2eTestFiles]
const command = isWin ? 'cmd.exe' : 'npx'
const commandArgs = isWin ? ['/c', 'npx.cmd', ...args] : args

const child = spawn(command, commandArgs, {
  cwd: process.cwd(),
  stdio: 'inherit',
  env: {
    ...process.env,
    E2E_BASE_URL: baseUrl,
    E2E_API_BASE_URL: apiBaseUrl,
  },
})

child.on('error', (err) => {
  console.error(`[staging:e2e] failed to start: ${err.message}`)
  process.exitCode = 1
})

child.on('close', (code) => {
  clearTimeout(timeoutHandle)
  process.exitCode = typeof code === 'number' ? code : 1
})

const timeoutHandle = setTimeout(() => {
  console.error(`[staging:e2e] timeout after ${timeoutMs}ms`)
  child.kill('SIGTERM')
  setTimeout(() => child.kill('SIGKILL'), 5000).unref()
}, timeoutMs)
