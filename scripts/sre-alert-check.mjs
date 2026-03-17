import fs from 'node:fs'
import path from 'node:path'
import { spawn } from 'node:child_process'

const root = process.cwd()
const outDir = path.join(root, 'logs', 'sre')
const now = new Date()
const day = now.toISOString().slice(0, 10)
const outFile = path.join(outDir, `alerts-${day}.json`)
const latestFile = path.join(outDir, 'alerts-latest.json')

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

function readJson(file) {
  if (!fs.existsSync(file)) return null
  try {
    return JSON.parse(fs.readFileSync(file, 'utf8'))
  } catch {
    return null
  }
}

function buildVerdict(snapshot, evidence) {
  const reasons = []
  const advisories = []
  const detailedAlerts = Array.isArray(snapshot?.alertsDetailed) ? snapshot.alertsDetailed : []
  const level = String(snapshot?.alertsLevel || 'NONE')
  const daily = snapshot?.errorBudget?.daily || {}
  const weekly = snapshot?.errorBudget?.weekly || {}
  const oncall = snapshot?.oncall || {}

  if (level === 'P1') {
    reasons.push('p1_alert_active')
  } else if (level === 'P2') {
    advisories.push('p2_alert_active')
  } else if (level === 'P3') {
    advisories.push('p3_alert_active')
  }

  if (daily.pass === false) reasons.push('error_budget_daily_exceeded')
  if (weekly.pass === false) reasons.push('error_budget_weekly_exceeded')
  if (!oncall.primary || oncall.primary === 'UNASSIGNED') reasons.push('oncall_primary_missing')
  if (!oncall.escalation || oncall.escalation === 'UNDEFINED') reasons.push('oncall_escalation_missing')
  if (evidence.sreDaily && evidence.sreDaily.verdict && evidence.sreDaily.verdict.pass === false) {
    reasons.push('sre_daily_failed')
  }
  if (evidence.perfGate && evidence.perfGate.pass === false) {
    reasons.push('perf_gate_failed')
  }
  if (evidence.securityScan && evidence.securityScan.pass === false) {
    reasons.push('security_scan_failed')
  }

  const recommendation = reasons.includes('p1_alert_active')
    ? 'block_release_and_page_primary'
    : reasons.length > 0
      ? 'block_release_and_investigate'
      : advisories.length > 0
        ? 'allow_release_with_attention'
        : 'allow_release'

  return {
    pass: reasons.length === 0,
    level,
    reasons,
    advisories,
    recommendation,
    detailedAlerts,
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
    const sloSnapshot = await getJson('/api/v1/ops/slo-snapshot', token)

    const evidence = {
      sreDaily: readJson(path.join(outDir, 'daily-latest.json')),
      perfGate: readJson(path.join(root, 'logs', 'perf', 'perf-gate-latest.json')),
      securityScan: readJson(path.join(root, 'logs', 'security', 'security-scan-latest.json')),
    }
    const verdict = buildVerdict(sloSnapshot, evidence)

    const report = {
      generatedAt: new Date().toISOString(),
      baseUrl: BASE_URL,
      tenantId: TENANT_ID,
      backendStartedByScript,
      durationMs: Date.now() - beginAt,
      sloSnapshot,
      evidence,
      verdict,
    }
    fs.writeFileSync(outFile, JSON.stringify(report, null, 2))
    fs.writeFileSync(latestFile, JSON.stringify(report, null, 2))

    if (!verdict.pass) {
      console.error(`SRE_ALERT_CHECK_FAIL ${path.relative(root, outFile)} level=${verdict.level} reasons=${verdict.reasons.join(';')}`)
      process.exit(1)
    }
    console.log(`SRE_ALERT_CHECK_OK ${path.relative(root, outFile)}`)
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
  console.error(`SRE_ALERT_CHECK_ERROR ${err?.message || err}`)
  process.exit(1)
})
