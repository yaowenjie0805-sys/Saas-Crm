import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'

const root = process.cwd()
const outDir = path.join(root, 'logs', 'staging')
const stamp = new Date().toISOString().replace(/[:.]/g, '-')
const reportFile = path.join(outDir, `staging-verify-${stamp}.json`)
const latestFile = path.join(outDir, 'staging-verify-latest.json')
const HEALTH_RETRY_COUNT = 20
const HEALTH_RETRY_DELAY_MS = 2000
const HEALTH_REQUEST_TIMEOUT_MS = 5000

function ensureDir(dir) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })
}

function run(command, args = [], envOverrides = {}) {
  const result = spawnSync(command, args, {
    cwd: root,
    encoding: 'utf8',
    env: { ...process.env, ...envOverrides },
  })
  return {
    ok: result.status === 0,
    status: result.status,
    stdout: result.stdout || '',
    stderr: result.stderr || '',
  }
}

function runNpm(script, envOverrides = {}) {
  const isWin = process.platform === 'win32'
  return isWin
    ? run('cmd.exe', ['/c', 'npm', 'run', script], envOverrides)
    : run('npm', ['run', script], envOverrides)
}

function writeReport(report) {
  ensureDir(outDir)
  fs.writeFileSync(reportFile, JSON.stringify(report, null, 2))
  fs.writeFileSync(latestFile, JSON.stringify(report, null, 2))
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function fetchWithTimeout(url) {
  if (typeof AbortSignal !== 'undefined' && typeof AbortSignal.timeout === 'function') {
    return fetch(url, { signal: AbortSignal.timeout(HEALTH_REQUEST_TIMEOUT_MS) })
  }
  return fetch(url)
}

async function checkHealthEndpoint(baseUrl, endpoint) {
  const url = `${baseUrl.replace(/\/$/, '')}${endpoint}`
  let ok = false
  let status = 0
  let body = ''
  for (let i = 0; i < HEALTH_RETRY_COUNT; i += 1) {
    try {
      const res = await fetchWithTimeout(url)
      status = res.status
      body = await res.text()
      if (res.ok && /UP/i.test(body)) {
        ok = true
        break
      }
    } catch (err) {
      body = String(err?.message || err)
    }
    await sleep(HEALTH_RETRY_DELAY_MS)
  }
  return { endpoint, ok, status, body: body.slice(0, 200) }
}

async function checkHealth(baseUrl) {
  const endpoints = ['/api/health/live', '/api/health/ready', '/api/health/deps']
  return Promise.all(endpoints.map((endpoint) => checkHealthEndpoint(baseUrl, endpoint)))
}

async function main() {
  const beginAt = Date.now()
  const checks = {}
  const healthChecks = []
  const stagingBaseUrl = process.env.STAGING_BASE_URL || ''
  const normalizedBaseUrl = stagingBaseUrl ? stagingBaseUrl.replace(/\/$/, '') : ''
  const remoteCheckEnv = normalizedBaseUrl
    ? {
      SRE_BASE_URL: process.env.SRE_BASE_URL || normalizedBaseUrl,
      PERF_BASE_URL: process.env.PERF_BASE_URL || normalizedBaseUrl,
    }
    : {}

  if (stagingBaseUrl) {
    const deployedHealth = await checkHealth(stagingBaseUrl)
    healthChecks.push(...deployedHealth)
    checks.stagingHealth = {
      ok: deployedHealth.every((item) => item.ok),
      status: deployedHealth.every((item) => item.ok) ? 0 : 1,
    }
  }

  const e2eScript = stagingBaseUrl ? 'staging:e2e' : 'test:e2e'
  const e2e = runNpm(e2eScript)
  checks.e2e = { ok: e2e.ok, status: e2e.status }

  const sre = runNpm('sre:daily-check', remoteCheckEnv)
  checks.sreDaily = { ok: sre.ok, status: sre.status }

  const perfGate = runNpm('perf:gate', remoteCheckEnv)
  checks.perfGate = { ok: perfGate.ok, status: perfGate.status }

  const pass = Object.values(checks).every((c) => c.ok)
  const report = {
    generatedAt: new Date().toISOString(),
    pass,
    durationMs: Date.now() - beginAt,
    checks,
    healthChecks,
    rollbackRecommendation: pass ? 'none' : 'rollback_recommended',
  }
  writeReport(report)
  if (!pass) {
    console.error(`STAGING_VERIFY_FAIL ${path.relative(root, reportFile)}`)
    process.exit(1)
  }
  console.log(`STAGING_VERIFY_OK ${path.relative(root, reportFile)}`)
}

try {
  await main()
} catch (err) {
  const report = {
    generatedAt: new Date().toISOString(),
    pass: false,
    error: err?.message || String(err),
    rollbackRecommendation: 'rollback_recommended',
  }
  writeReport(report)
  console.error(`STAGING_VERIFY_ERROR ${err?.message || err}`)
  process.exit(1)
}
