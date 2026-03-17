import fs from 'node:fs'
import path from 'node:path'
import { spawn } from 'node:child_process'

const root = process.cwd()
const outDir = path.join(root, 'logs', 'sre')
const now = new Date()
const day = now.toISOString().slice(0, 10)
const outFile = path.join(outDir, `daily-${day}.json`)
const latestFile = path.join(outDir, 'daily-latest.json')

const API_PORT = process.env.API_PORT || '18080'
const BASE_URL = process.env.SRE_BASE_URL || `http://127.0.0.1:${API_PORT}`
const DB_NAME = process.env.DB_NAME || 'crm_local_e2e'
const DB_USER = process.env.DB_USER || 'root'
const DB_PASSWORD = process.env.DB_PASSWORD || 'root'
const DB_URL = process.env.DB_URL || `jdbc:mysql://127.0.0.1:3306/${DB_NAME}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true`
const TENANT_ID = process.env.SRE_TENANT_ID || 'tenant_default'
const USERNAME = process.env.SRE_ADMIN_USER || 'admin'
const PASSWORD = process.env.SRE_ADMIN_PASSWORD || 'admin123'

function ensureDir(dir) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function waitForHealth(baseUrl, timeoutMs, isExited) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    if (isExited && isExited()) return false
    try {
      const res = await fetch(`${baseUrl}/api/health/live`)
      if (res.ok) return true
    } catch (_) {}
    await sleep(400)
  }
  return false
}

function startBackendIfNeeded() {
  if (String(process.env.SRE_AUTO_START_BACKEND || 'true').toLowerCase() === 'false') {
    return null
  }
  const jar = path.join(root, 'backend', 'target', 'crm-backend-1.0.0.jar')
  if (!fs.existsSync(jar)) {
    return null
  }

  const child = spawn('java', [
    '-Dspring.profiles.active=dev',
    '-Dlead.import.listener.enabled=false',
    '-Dlead.import.mq.declare.enabled=false',
    '-Dlead.import.mq.publish.enabled=false',
    '-Dspring.rabbitmq.listener.simple.auto-startup=false',
    '-Dspring.rabbitmq.listener.direct.auto-startup=false',
    '-Dspring.rabbitmq.dynamic=false',
    `-Dserver.port=${API_PORT}`,
    `-DDB_URL=${DB_URL}`,
    `-DDB_USER=${DB_USER}`,
    `-DDB_PASSWORD=${DB_PASSWORD}`,
    '-jar',
    jar,
  ], {
    cwd: root,
    stdio: ['ignore', 'pipe', 'pipe'],
  })

  let exited = false
  let exitCode = null
  let startupLog = ''
  child.on('exit', (code) => {
    exited = true
    exitCode = code
  })
  child.stdout.on('data', (chunk) => {
    startupLog += String(chunk)
    if (startupLog.length > 120000) startupLog = startupLog.slice(-120000)
  })
  child.stderr.on('data', (chunk) => {
    startupLog += String(chunk)
    if (startupLog.length > 120000) startupLog = startupLog.slice(-120000)
  })

  return {
    child,
    isExited: () => exited,
    exitCode: () => exitCode,
    startupLog: () => startupLog,
  }
}

async function login() {
  const res = await fetch(`${BASE_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ username: USERNAME, password: PASSWORD }),
  })
  const body = await res.json()
  if (!res.ok || !body?.token) {
    throw new Error(`login_failed status=${res.status}`)
  }
  return body.token
}

async function getJson(pathname, token) {
  const res = await fetch(`${BASE_URL}${pathname}`, {
    headers: {
      Authorization: `Bearer ${token}`,
      'X-Tenant-Id': TENANT_ID,
    },
  })
  const body = await res.json()
  if (!res.ok) {
    throw new Error(`${pathname}_failed status=${res.status} body=${JSON.stringify(body)}`)
  }
  return body
}

function evaluate(snapshot, ready) {
  const reasons = []
  if (!ready?.ok) reasons.push('readiness_not_ok')
  if (snapshot?.overallStatus !== 'PASS') reasons.push('slo_snapshot_failed')
  if (Array.isArray(snapshot?.alerts) && snapshot.alerts.length > 0) {
    reasons.push(`slo_alerts:${snapshot.alerts.join(',')}`)
  }
  return {
    pass: reasons.length === 0,
    reasons,
  }
}

async function main() {
  ensureDir(outDir)
  const beginAt = Date.now()
  let backend = null
  let backendStartedByScript = false
  try {
    let healthy = await waitForHealth(BASE_URL, 3000)
    if (!healthy) {
      backend = startBackendIfNeeded()
      if (backend) {
        backendStartedByScript = true
        healthy = await waitForHealth(BASE_URL, 90000, backend.isExited)
      }
    }
    if (!healthy) {
      const extra = backend ? ` exitCode=${backend.exitCode()} startupLog=${backend.startupLog().slice(-500)}` : ''
      throw new Error(`backend_not_ready${extra}`)
    }

    const token = await login()
    const [sloSnapshot, metricsSummary, healthReady, healthDeps] = await Promise.all([
      getJson('/api/v1/ops/slo-snapshot', token),
      getJson('/api/v1/ops/metrics/summary', token),
      fetch(`${BASE_URL}/api/health/ready`).then((r) => r.json()),
      fetch(`${BASE_URL}/api/health/deps`).then((r) => r.json()),
    ])
    const verdict = evaluate(sloSnapshot, healthReady)

    const report = {
      generatedAt: new Date().toISOString(),
      baseUrl: BASE_URL,
      tenantId: TENANT_ID,
      backendStartedByScript,
      durationMs: Date.now() - beginAt,
      checks: {
        sloSnapshot,
        metricsSummary,
        healthReady,
        healthDeps,
      },
      verdict,
    }
    fs.writeFileSync(outFile, JSON.stringify(report, null, 2))
    fs.writeFileSync(latestFile, JSON.stringify(report, null, 2))

    if (!verdict.pass) {
      console.error(`SRE_DAILY_CHECK_FAIL ${path.relative(root, outFile)} reasons=${verdict.reasons.join(';')}`)
      process.exit(1)
    }
    console.log(`SRE_DAILY_CHECK_OK ${path.relative(root, outFile)}`)
  } finally {
    if (backend && backend.child && !backend.isExited()) {
      backend.child.kill()
    }
  }
}

main().catch((err) => {
  ensureDir(outDir)
  const report = {
    generatedAt: new Date().toISOString(),
    baseUrl: BASE_URL,
    verdict: {
      pass: false,
      reasons: [err?.message || String(err)],
    },
  }
  fs.writeFileSync(outFile, JSON.stringify(report, null, 2))
  fs.writeFileSync(latestFile, JSON.stringify(report, null, 2))
  console.error(`SRE_DAILY_CHECK_ERROR ${err?.message || err}`)
  process.exit(1)
})
