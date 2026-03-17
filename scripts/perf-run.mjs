import fs from 'node:fs'
import path from 'node:path'
import { spawn, spawnSync } from 'node:child_process'

const root = process.cwd()
const mode = (process.argv[2] || 'baseline').toLowerCase()
const outDir = path.join(root, 'logs', 'perf')
const stamp = new Date().toISOString().replace(/[:.]/g, '-')
const rawFile = path.join(outDir, `perf-${mode}-${stamp}-raw.json`)
const reportFile = path.join(outDir, `perf-${mode}-${stamp}.json`)
const latestFile = path.join(outDir, `perf-${mode}-latest.json`)
const summaryFile = path.join(outDir, `perf-${mode}-${stamp}.txt`)

const API_PORT = process.env.API_PORT || '18080'
const BASE_URL = process.env.PERF_BASE_URL || `http://127.0.0.1:${API_PORT}`
const REQUIRE_K6 = String(process.env.PERF_REQUIRE_K6 || 'false').toLowerCase() === 'true'
const DB_NAME = process.env.DB_NAME || 'crm_local_e2e'
const DB_USER = process.env.DB_USER || 'root'
const DB_PASSWORD = process.env.DB_PASSWORD || 'root'
const DB_URL = process.env.DB_URL || `jdbc:mysql://127.0.0.1:3306/${DB_NAME}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true`

function ensureDir(dir) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })
}

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms))
}

async function waitForLive(baseUrl, timeoutMs, isExited) {
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
  if (String(process.env.PERF_AUTO_START_BACKEND || 'true').toLowerCase() === 'false') return null
  const jar = path.join(root, 'backend', 'target', 'crm-backend-1.0.0.jar')
  if (!fs.existsSync(jar)) return null
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
  ], { cwd: root, stdio: ['ignore', 'pipe', 'pipe'] })

  let exited = false
  child.on('exit', () => {
    exited = true
  })
  return { child, isExited: () => exited }
}

function runK6() {
  const result = spawnSync('k6', [
    'run',
    '--summary-export',
    rawFile,
    'scripts/perf-k6-scenario.js',
  ], {
    cwd: root,
    env: {
      ...process.env,
      PERF_MODE: mode,
      PERF_BASE_URL: BASE_URL,
    },
    encoding: 'utf8',
  })
  return result
}

async function runFallback() {
  const loginRes = await fetch(`${BASE_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({
      username: process.env.PERF_ADMIN_USER || 'admin',
      password: process.env.PERF_ADMIN_PASSWORD || 'admin123',
    }),
  })
  const loginBody = await loginRes.json()
  if (!loginRes.ok || !loginBody?.token) {
    throw new Error(`perf_fallback_login_failed status=${loginRes.status}`)
  }
  const token = loginBody.token
  const headers = {
    Authorization: `Bearer ${token}`,
    'X-Tenant-Id': process.env.PERF_TENANT_ID || 'tenant_default',
  }
  const requests = []
  const started = Date.now()
  const loops = mode === 'smoke' ? 20 : 120
  for (let i = 0; i < loops; i += 1) {
    const paths = ['/api/dashboard', '/api/customers/search?q=perf&page=1&size=20', '/api/v1/reports/overview']
    for (const p of paths) {
      const t0 = Date.now()
      try {
        const res = await fetch(`${BASE_URL}${p}`, { headers })
        const dt = Date.now() - t0
        requests.push({ path: p, status: res.status, duration: dt, timeout: res.status === 0 })
      } catch (_) {
        const dt = Date.now() - t0
        requests.push({ path: p, status: 0, duration: dt, timeout: true })
      }
    }
  }
  const totalDurationMs = Date.now() - started
  const byRoute = {}
  for (const r of requests) {
    if (!byRoute[r.path]) byRoute[r.path] = []
    byRoute[r.path].push(r)
  }
  const percentile = (values, p) => {
    if (!values.length) return 0
    const sorted = [...values].sort((a, b) => a - b)
    const idx = Math.max(0, Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1))
    return sorted[idx]
  }
  const allDurations = requests.map((r) => r.duration)
  const errors = requests.filter((r) => r.status >= 500 || r.status === 0).length
  const timeouts = requests.filter((r) => r.timeout).length
  const rps = totalDurationMs <= 0 ? 0 : requests.length / (totalDurationMs / 1000)
  const raw = {
    fallback: true,
    metrics: {
      http_reqs: { values: { count: requests.length, rate: rps } },
      http_req_duration: { values: { 'p(95)': percentile(allDurations, 95), 'p(99)': percentile(allDurations, 99) } },
      http_req_failed: { values: { count: errors, rate: requests.length ? errors / requests.length : 0 } },
      perf_timeout_count: { values: { count: timeouts } },
      perf_dashboard_requests: { values: { count: (byRoute['/api/dashboard'] || []).length } },
      perf_customers_requests: { values: { count: (byRoute['/api/customers/search?q=perf&page=1&size=20'] || []).length } },
      perf_reports_requests: { values: { count: (byRoute['/api/v1/reports/overview'] || []).length } },
      perf_dashboard_duration: { values: { 'p(95)': percentile((byRoute['/api/dashboard'] || []).map((x) => x.duration), 95), 'p(99)': percentile((byRoute['/api/dashboard'] || []).map((x) => x.duration), 99) } },
      perf_customers_duration: { values: { 'p(95)': percentile((byRoute['/api/customers/search?q=perf&page=1&size=20'] || []).map((x) => x.duration), 95), 'p(99)': percentile((byRoute['/api/customers/search?q=perf&page=1&size=20'] || []).map((x) => x.duration), 99) } },
      perf_reports_duration: { values: { 'p(95)': percentile((byRoute['/api/v1/reports/overview'] || []).map((x) => x.duration), 95), 'p(99)': percentile((byRoute['/api/v1/reports/overview'] || []).map((x) => x.duration), 99) } },
    },
  }
  fs.writeFileSync(rawFile, JSON.stringify(raw, null, 2))
}

function metricNumber(raw, name, key, fallback = 0) {
  return Number(raw?.metrics?.[name]?.values?.[key] ?? fallback)
}

function buildReport(raw) {
  const report = {
    generatedAt: new Date().toISOString(),
    mode,
    baseUrl: BASE_URL,
    fallbackMode: !!raw?.fallback,
    summary: {
      rps: metricNumber(raw, 'http_reqs', 'rate'),
      requestCount: metricNumber(raw, 'http_reqs', 'count'),
      p95Ms: metricNumber(raw, 'http_req_duration', 'p(95)'),
      p99Ms: metricNumber(raw, 'http_req_duration', 'p(99)'),
      errorRate: metricNumber(raw, 'http_req_failed', 'rate'),
      errorCount: metricNumber(raw, 'http_req_failed', 'count'),
      timeouts: metricNumber(raw, 'perf_timeout_count', 'count'),
    },
    routes: {
      dashboard: {
        count: metricNumber(raw, 'perf_dashboard_requests', 'count'),
        p95Ms: metricNumber(raw, 'perf_dashboard_duration', 'p(95)'),
        p99Ms: metricNumber(raw, 'perf_dashboard_duration', 'p(99)'),
      },
      customers: {
        count: metricNumber(raw, 'perf_customers_requests', 'count'),
        p95Ms: metricNumber(raw, 'perf_customers_duration', 'p(95)'),
        p99Ms: metricNumber(raw, 'perf_customers_duration', 'p(99)'),
      },
      reports: {
        count: metricNumber(raw, 'perf_reports_requests', 'count'),
        p95Ms: metricNumber(raw, 'perf_reports_duration', 'p(95)'),
        p99Ms: metricNumber(raw, 'perf_reports_duration', 'p(99)'),
      },
    },
    rawFile: path.relative(root, rawFile),
  }
  return report
}

function writeSummary(report) {
  const lines = [
    `mode=${report.mode}`,
    `rps=${report.summary.rps.toFixed(2)}`,
    `requestCount=${report.summary.requestCount}`,
    `p95Ms=${report.summary.p95Ms.toFixed(2)}`,
    `p99Ms=${report.summary.p99Ms.toFixed(2)}`,
    `errorRate=${report.summary.errorRate.toFixed(4)}`,
    `timeouts=${report.summary.timeouts}`,
    `dashboard.p95=${report.routes.dashboard.p95Ms.toFixed(2)}`,
    `customers.p95=${report.routes.customers.p95Ms.toFixed(2)}`,
    `reports.p95=${report.routes.reports.p95Ms.toFixed(2)}`,
  ]
  fs.writeFileSync(summaryFile, lines.join('\n'))
}

async function main() {
  ensureDir(outDir)
  let backend = null
  try {
    let ready = await waitForLive(BASE_URL, 3000)
    if (!ready) {
      backend = startBackendIfNeeded()
      ready = await waitForLive(BASE_URL, 90000, backend?.isExited)
    }
    if (!ready) {
      throw new Error('perf_backend_not_ready')
    }

    const k6Exists = spawnSync('k6', ['version'], { cwd: root, encoding: 'utf8' }).status === 0
    if (k6Exists) {
      const result = runK6()
      if (result.status !== 0) {
        throw new Error(`k6_run_failed status=${result.status} stderr=${(result.stderr || '').slice(-300)}`)
      }
    } else {
      if (REQUIRE_K6) {
        throw new Error('k6_not_available_but_required')
      }
      await runFallback()
    }

    const raw = JSON.parse(fs.readFileSync(rawFile, 'utf8'))
    const report = buildReport(raw)
    fs.writeFileSync(reportFile, JSON.stringify(report, null, 2))
    fs.writeFileSync(latestFile, JSON.stringify(report, null, 2))
    writeSummary(report)
    console.log(`PERF_${mode.toUpperCase()}_OK ${path.relative(root, reportFile)}`)
  } finally {
    if (backend?.child && !backend.isExited()) {
      backend.child.kill()
    }
  }
}

main().catch((err) => {
  ensureDir(outDir)
  const failReport = {
    generatedAt: new Date().toISOString(),
    mode,
    failed: true,
    error: err?.message || String(err),
  }
  fs.writeFileSync(reportFile, JSON.stringify(failReport, null, 2))
  fs.writeFileSync(latestFile, JSON.stringify(failReport, null, 2))
  console.error(`PERF_${mode.toUpperCase()}_FAIL ${err?.message || err}`)
  process.exit(1)
})
