import fs from 'node:fs'
import path from 'node:path'
import net from 'node:net'

const root = process.cwd()
const backendProdProps = path.join(root, 'apps', 'api', 'src', 'main', 'resources', 'application-prod.properties')
const releaseDir = path.join(root, 'logs', 'release')
const sreDir = path.join(root, 'logs', 'sre')
const drillDir = path.join(root, 'logs', 'drills')
const perfDir = path.join(root, 'logs', 'perf')
const securityDir = path.join(root, 'logs', 'security')
const stagingDir = path.join(root, 'logs', 'staging')
const preflightDir = path.join(root, 'logs', 'preflight')

function pass(name, detail) {
  return { name, ok: true, detail }
}

function fail(name, detail) {
  return { name, ok: false, detail }
}

function envVal(key, fallback = '') {
  const value = process.env[key]
  return typeof value === 'string' && value.trim() ? value.trim() : fallback
}

function checkRequiredEnv() {
  const checks = []
  const secret = envVal('AUTH_TOKEN_SECRET')
  checks.push(secret && secret !== 'crm-secret-change-me'
    ? pass('AUTH_TOKEN_SECRET', 'configured')
    : fail('AUTH_TOKEN_SECRET', 'missing or insecure default'))

  const mfaCode = envVal('SECURITY_MFA_STATIC_CODE')
  checks.push(mfaCode && mfaCode !== '000000'
    ? pass('SECURITY_MFA_STATIC_CODE', 'configured')
    : fail('SECURITY_MFA_STATIC_CODE', 'missing or insecure default'))

  const ssoMode = envVal('SECURITY_SSO_MODE')
  checks.push(ssoMode && ssoMode.toLowerCase() !== 'mock'
    ? pass('SECURITY_SSO_MODE', `mode=${ssoMode}`)
    : fail('SECURITY_SSO_MODE', 'missing or mock mode'))

  const bootstrapPwd = envVal('AUTH_BOOTSTRAP_DEFAULT_PASSWORD')
  checks.push(bootstrapPwd && bootstrapPwd !== 'admin123'
    ? pass('AUTH_BOOTSTRAP_DEFAULT_PASSWORD', 'configured')
    : fail('AUTH_BOOTSTRAP_DEFAULT_PASSWORD', 'missing or insecure default'))

  const dbUrl = envVal('DB_URL')
  const dbUser = envVal('DB_USER')
  const dbPassword = envVal('DB_PASSWORD')
  checks.push(dbUrl && dbUser && dbPassword
    ? pass('DB credentials', 'DB_URL/DB_USER/DB_PASSWORD set')
    : fail('DB credentials', 'DB_URL/DB_USER/DB_PASSWORD required'))
  return checks
}

function checkProdDefaultsInRepo() {
  if (!fs.existsSync(backendProdProps)) {
    return [fail('application-prod.properties', 'file missing')]
  }
  const text = fs.readFileSync(backendProdProps, 'utf8')
  const checks = []
  checks.push(text.includes('app.seed.enabled=${APP_SEED_ENABLED:false}')
    ? pass('prod seed', 'disabled by default')
    : fail('prod seed', 'expected APP_SEED_ENABLED:false'))
  checks.push(text.includes('app.seed.demo.enabled=${APP_SEED_DEMO_ENABLED:false}')
    ? pass('prod demo seed', 'disabled by default')
    : fail('prod demo seed', 'expected APP_SEED_DEMO_ENABLED:false'))
  checks.push(text.includes('auth.cookie.secure=${AUTH_COOKIE_SECURE:true}')
    ? pass('secure cookie', 'enabled by default')
    : fail('secure cookie', 'expected AUTH_COOKIE_SECURE:true'))
  return checks
}

function checkRunbookFiles() {
  const files = [
    path.join(root, 'docs', 'operations', 'backup-restore-runbook.md'),
    path.join(root, 'docs', 'operations', 'release-rollback-runbook.md'),
    path.join(root, 'scripts', 'db-backup.ps1'),
    path.join(root, 'scripts', 'db-restore.ps1'),
  ]
  return files.map((f) => (fs.existsSync(f) ? pass(path.relative(root, f), 'exists') : fail(path.relative(root, f), 'missing')))
}

function newestFile(dir, prefix, suffix) {
  if (!fs.existsSync(dir)) return ''
  const files = fs.readdirSync(dir)
    .filter((name) => name.startsWith(prefix) && name.endsWith(suffix))
    .sort()
  if (files.length === 0) return ''
  return path.join(dir, files[files.length - 1])
}

function checkReleaseEvidence() {
  const checks = []
  const skipDrillEvidence = envVal('PREFLIGHT_SKIP_DRILL_EVIDENCE', 'false').toLowerCase() === 'true'
  const snapshot = newestFile(releaseDir, 'release-snapshot-', '.json')
  checks.push(snapshot
    ? pass('release snapshot', path.relative(root, snapshot))
    : fail('release snapshot', 'missing logs/release/release-snapshot-*.json'))

  if (skipDrillEvidence) {
    checks.push(pass('backup restore drill', 'skipped by PREFLIGHT_SKIP_DRILL_EVIDENCE=true'))
    checks.push(pass('rollback drill', 'skipped by PREFLIGHT_SKIP_DRILL_EVIDENCE=true'))
    return checks
  }

  const backupDrill = newestFile(drillDir, 'backup-restore-', '.json')
  if (!backupDrill) {
    checks.push(fail('backup restore drill', 'missing logs/drills/backup-restore-*.json'))
  } else {
    const body = JSON.parse(fs.readFileSync(backupDrill, 'utf8'))
    checks.push(body.pass
      ? pass('backup restore drill', path.relative(root, backupDrill))
      : fail('backup restore drill', `${path.relative(root, backupDrill)} pass=false`))
  }

  const rollbackDrill = newestFile(drillDir, 'rollback-', '.json')
  if (!rollbackDrill) {
    checks.push(fail('rollback drill', 'missing logs/drills/rollback-*.json'))
  } else {
    const body = JSON.parse(fs.readFileSync(rollbackDrill, 'utf8'))
    checks.push(body.pass
      ? pass('rollback drill', path.relative(root, rollbackDrill))
      : fail('rollback drill', `${path.relative(root, rollbackDrill)} pass=false`))
  }
  return checks
}

function checkSloEvidence() {
  const checks = []
  const skipSloEvidence = envVal('PREFLIGHT_SKIP_SLO_EVIDENCE', 'false').toLowerCase() === 'true'
  if (skipSloEvidence) {
    checks.push(pass('sre daily check', 'skipped by PREFLIGHT_SKIP_SLO_EVIDENCE=true'))
    return checks
  }
  const latest = path.join(sreDir, 'daily-latest.json')
  if (!fs.existsSync(latest)) {
    checks.push(fail('sre daily check', 'missing logs/sre/daily-latest.json'))
    return checks
  }
  let body = {}
  try {
    body = JSON.parse(fs.readFileSync(latest, 'utf8'))
  } catch (err) {
    checks.push(fail('sre daily check', `invalid json: ${err.message}`))
    return checks
  }
  const passFlag = !!body?.verdict?.pass
  checks.push(passFlag
    ? pass('sre daily check verdict', 'pass=true')
    : fail('sre daily check verdict', `pass=false reasons=${String((body?.verdict?.reasons || []).join(','))}`))

  const summary = body?.checks?.sloSnapshot?.summary || {}
  const thresholds = body?.checks?.sloSnapshot?.thresholds || {}
  const ready = body?.checks?.healthReady || {}
  const errorRate = Number(summary.errorRate || 0)
  const errorRateMax = Number(thresholds.errorRateMax || 0.02)
  const readyOk = !!ready.ok
  checks.push(errorRate <= errorRateMax
    ? pass('slo error rate', `errorRate=${errorRate}`)
    : fail('slo error rate', `errorRate=${errorRate} > ${errorRateMax}`))
  checks.push(readyOk
    ? pass('slo readiness', 'ready=true')
    : fail('slo readiness', 'ready=false'))
  return checks
}

function checkAlertEvidence() {
  const checks = []
  const skipAlertEvidence = envVal('PREFLIGHT_SKIP_ALERT_EVIDENCE', 'false').toLowerCase() === 'true'
  if (skipAlertEvidence) {
    checks.push(pass('sre alert check', 'skipped by PREFLIGHT_SKIP_ALERT_EVIDENCE=true'))
    return checks
  }

  const latest = path.join(sreDir, 'alerts-latest.json')
  if (!fs.existsSync(latest)) {
    checks.push(fail('sre alert check', 'missing logs/sre/alerts-latest.json'))
    return checks
  }
  let body = {}
  try {
    body = JSON.parse(fs.readFileSync(latest, 'utf8'))
  } catch (err) {
    checks.push(fail('sre alert check', `invalid json: ${err.message}`))
    return checks
  }

  const verdict = body?.verdict || {}
  const level = String(verdict.level || 'NONE')
  const reasons = Array.isArray(verdict.reasons) ? verdict.reasons : []
  const oncall = body?.sloSnapshot?.oncall || {}
  const dailyBudget = body?.sloSnapshot?.errorBudget?.daily || {}
  const weeklyBudget = body?.sloSnapshot?.errorBudget?.weekly || {}

  checks.push(verdict.pass
    ? pass('sre alert verdict', 'pass=true')
    : fail('sre alert verdict', `pass=false reasons=${reasons.join(',') || 'unknown'}`))
  checks.push(level === 'P1'
    ? fail('sre alert level', 'P1 active')
    : pass('sre alert level', `level=${level}`))
  checks.push(dailyBudget.pass === false
    ? fail('daily error budget', `consumed=${dailyBudget.consumed} budget=${dailyBudget.budget}`)
    : pass('daily error budget', `consumed=${dailyBudget.consumed || 0}`))
  checks.push(weeklyBudget.pass === false
    ? fail('weekly error budget', `consumed=${weeklyBudget.consumed} budget=${weeklyBudget.budget}`)
    : pass('weekly error budget', `consumed=${weeklyBudget.consumed || 0}`))
  checks.push(oncall.primary && oncall.primary !== 'UNASSIGNED'
    ? pass('oncall primary', `primary=${oncall.primary}`)
    : fail('oncall primary', 'missing current oncall primary'))
  checks.push(oncall.escalation && oncall.escalation !== 'UNDEFINED'
    ? pass('oncall escalation', `escalation=${oncall.escalation}`)
    : fail('oncall escalation', 'missing escalation chain'))
  return checks
}

function checkPerfEvidence() {
  const checks = []
  const skipPerfEvidence = envVal('PREFLIGHT_SKIP_PERF_EVIDENCE', 'false').toLowerCase() === 'true'
  if (skipPerfEvidence) {
    checks.push(pass('perf evidence', 'skipped by PREFLIGHT_SKIP_PERF_EVIDENCE=true'))
    return checks
  }
  const baseline = newestFile(perfDir, 'perf-baseline-', '.json')
  if (!baseline || baseline.includes('-raw.')) {
    checks.push(fail('perf baseline report', 'missing logs/perf/perf-baseline-*.json'))
  } else {
    const body = JSON.parse(fs.readFileSync(baseline, 'utf8'))
    checks.push(body?.failed
      ? fail('perf baseline report', `${path.relative(root, baseline)} failed=true`)
      : pass('perf baseline report', path.relative(root, baseline)))
  }

  const gate = path.join(perfDir, 'perf-gate-latest.json')
  if (!fs.existsSync(gate)) {
    checks.push(fail('perf gate report', 'missing logs/perf/perf-gate-latest.json'))
  } else {
    const body = JSON.parse(fs.readFileSync(gate, 'utf8'))
    checks.push(body?.pass
      ? pass('perf gate report', 'pass=true')
      : fail('perf gate report', `pass=false failedCount=${Number(body?.failedCount || 0)}`))
  }
  return checks
}

function checkSecurityEvidence() {
  const checks = []
  const skipSecurityEvidence = envVal('PREFLIGHT_SKIP_SECURITY_EVIDENCE', 'false').toLowerCase() === 'true'
  if (skipSecurityEvidence) {
    checks.push(pass('security evidence', 'skipped by PREFLIGHT_SKIP_SECURITY_EVIDENCE=true'))
    return checks
  }
  const latest = path.join(securityDir, 'security-scan-latest.json')
  if (!fs.existsSync(latest)) {
    checks.push(fail('security scan report', 'missing logs/security/security-scan-latest.json'))
    return checks
  }
  const body = JSON.parse(fs.readFileSync(latest, 'utf8'))
  checks.push(body?.pass
    ? pass('security scan report', 'pass=true')
    : fail('security scan report', 'pass=false'))
  return checks
}

function checkStagingEvidence() {
  const checks = []
  const defaultRequire = envVal('GITHUB_REF_NAME', '').toLowerCase() === 'main' ? 'true' : 'false'
  const requireStagingEvidence = envVal('PREFLIGHT_REQUIRE_STAGING_EVIDENCE', defaultRequire).toLowerCase() === 'true'
  if (!requireStagingEvidence) {
    checks.push(pass('staging release evidence', 'not required (PREFLIGHT_REQUIRE_STAGING_EVIDENCE=false)'))
    return checks
  }
  const latest = path.join(stagingDir, 'staging-release-latest.json')
  if (!fs.existsSync(latest)) {
    checks.push(fail('staging release evidence', 'missing logs/staging/staging-release-latest.json'))
    return checks
  }
  const body = JSON.parse(fs.readFileSync(latest, 'utf8'))
  const perfGate = !!body?.gate?.perf
  checks.push(perfGate
    ? pass('staging perf gate', 'pass=true')
    : fail('staging perf gate', 'pass=false'))
  checks.push(body?.pass
    ? pass('staging release evidence', 'pass=true')
    : fail('staging release evidence', 'pass=false'))
  return checks
}

function tcpCheck(host, port, timeoutMs = 1200) {
  return new Promise((resolve) => {
    const socket = new net.Socket()
    let done = false
    const finish = (ok, detail) => {
      if (done) return
      done = true
      socket.destroy()
      resolve({ ok, detail })
    }
    socket.setTimeout(timeoutMs)
    socket.once('error', (err) => finish(false, err.message))
    socket.once('timeout', () => finish(false, 'timeout'))
    socket.connect(port, host, () => finish(true, `connected ${host}:${port}`))
  })
}

async function checkOptionalDependencies() {
  const checks = []
  const mqEnabled = envVal('LEAD_IMPORT_MQ_PUBLISH_ENABLED', 'true').toLowerCase() !== 'false'
  if (!mqEnabled) {
    checks.push(pass('RabbitMQ connectivity', 'skipped (LEAD_IMPORT_MQ_PUBLISH_ENABLED=false)'))
    return checks
  }
  const host = envVal('RABBITMQ_HOST', '127.0.0.1')
  const port = Number(envVal('RABBITMQ_PORT', '5672'))
  const result = await tcpCheck(host, port)
  checks.push(result.ok
    ? pass('RabbitMQ connectivity', result.detail)
    : fail('RabbitMQ connectivity', result.detail))
  return checks
}

function printResults(allChecks) {
  let failed = 0
  if (!fs.existsSync(preflightDir)) fs.mkdirSync(preflightDir, { recursive: true })
  const report = {
    generatedAt: new Date().toISOString(),
    checks: allChecks,
    pass: true,
  }
  console.log('PRECHECK_SUMMARY')
  for (const c of allChecks) {
    const icon = c.ok ? 'OK' : 'FAIL'
    if (!c.ok) failed += 1
    console.log(`[${icon}] ${c.name} - ${c.detail}`)
  }
  report.pass = failed === 0
  report.failedCount = failed
  fs.writeFileSync(path.join(preflightDir, 'preflight-latest.json'), JSON.stringify(report, null, 2))
  if (failed > 0) {
    console.error(`PREFLIGHT_PROD_FAIL count=${failed}`)
    process.exit(1)
  }
  console.log('PREFLIGHT_PROD_OK')
}

async function main() {
  const checks = []
  checks.push(...checkRequiredEnv())
  checks.push(...checkProdDefaultsInRepo())
  checks.push(...checkRunbookFiles())
  checks.push(...checkReleaseEvidence())
  checks.push(...checkPerfEvidence())
  checks.push(...checkSloEvidence())
  checks.push(...checkAlertEvidence())
  checks.push(...checkSecurityEvidence())
  checks.push(...checkStagingEvidence())
  checks.push(...(await checkOptionalDependencies()))
  printResults(checks)
}

main().catch((err) => {
  console.error('PREFLIGHT_PROD_ERROR', err?.message || err)
  process.exit(1)
})
