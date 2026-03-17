import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'

const root = process.cwd()
const outDir = path.join(root, 'logs', 'staging')
const stamp = new Date().toISOString().replace(/[:.]/g, '-')
const reportFile = path.join(outDir, `staging-verify-${stamp}.json`)
const latestFile = path.join(outDir, 'staging-verify-latest.json')

function ensureDir(dir) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })
}

function run(command, args = []) {
  const result = spawnSync(command, args, {
    cwd: root,
    encoding: 'utf8',
    env: process.env,
  })
  return {
    ok: result.status === 0,
    status: result.status,
    stdout: result.stdout || '',
    stderr: result.stderr || '',
  }
}

function runNpm(script) {
  const isWin = process.platform === 'win32'
  return isWin
    ? run('cmd.exe', ['/c', 'npm', 'run', script])
    : run('npm', ['run', script])
}

function writeReport(report) {
  ensureDir(outDir)
  fs.writeFileSync(reportFile, JSON.stringify(report, null, 2))
  fs.writeFileSync(latestFile, JSON.stringify(report, null, 2))
}

async function checkHealth(baseUrl) {
  const endpoints = ['/api/health/live', '/api/health/ready', '/api/health/deps']
  const results = []
  for (const endpoint of endpoints) {
    const url = `${baseUrl.replace(/\/$/, '')}${endpoint}`
    let ok = false
    let status = 0
    let body = ''
    for (let i = 0; i < 20; i += 1) {
      try {
        const res = await fetch(url)
        status = res.status
        body = await res.text()
        if (res.ok && /UP/i.test(body)) {
          ok = true
          break
        }
      } catch (err) {
        body = String(err?.message || err)
      }
      await new Promise((resolve) => setTimeout(resolve, 2000))
    }
    results.push({ endpoint, ok, status, body: body.slice(0, 200) })
  }
  return results
}

async function main() {
  const beginAt = Date.now()
  const checks = {}
  const healthChecks = []
  const stagingBaseUrl = process.env.STAGING_BASE_URL || ''

  if (stagingBaseUrl) {
    const deployedHealth = await checkHealth(stagingBaseUrl)
    healthChecks.push(...deployedHealth)
    checks.stagingHealth = {
      ok: deployedHealth.every((item) => item.ok),
      status: deployedHealth.every((item) => item.ok) ? 0 : 1,
    }
  }

  const e2e = runNpm('test:e2e')
  checks.e2e = { ok: e2e.ok, status: e2e.status }

  const sre = runNpm('sre:daily-check')
  checks.sreDaily = { ok: sre.ok, status: sre.status }

  const perfGate = runNpm('perf:gate')
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
