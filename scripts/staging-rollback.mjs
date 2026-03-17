import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'

const root = process.cwd()
const outDir = path.join(root, 'logs', 'staging')
const stamp = new Date().toISOString().replace(/[:.]/g, '-')
const reportFile = path.join(outDir, `staging-rollback-${stamp}.json`)
const latestFile = path.join(outDir, 'staging-rollback-latest.json')

function ensureDir(dir) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })
}

function run(command, args = []) {
  const res = spawnSync(command, args, {
    cwd: root,
    encoding: 'utf8',
    env: process.env,
  })
  return {
    ok: res.status === 0,
    status: res.status,
    stdout: res.stdout || '',
    stderr: res.stderr || '',
  }
}

function env(name, fallback = '') {
  const val = process.env[name]
  return typeof val === 'string' && val.trim() ? val.trim() : fallback
}

function arg(name, fallback = '') {
  const idx = process.argv.indexOf(name)
  if (idx >= 0 && idx + 1 < process.argv.length) return String(process.argv[idx + 1]).trim()
  return fallback
}

function ssh(host, user, port, remoteCommand) {
  return run('ssh', [
    '-p', String(port),
    '-o', 'StrictHostKeyChecking=accept-new',
    `${user}@${host}`,
    remoteCommand,
  ])
}

function writeReport(report) {
  ensureDir(outDir)
  fs.writeFileSync(reportFile, JSON.stringify(report, null, 2))
  fs.writeFileSync(latestFile, JSON.stringify(report, null, 2))
}

async function healthChecks(baseUrl) {
  const endpoints = ['/api/health/live', '/api/health/ready', '/api/health/deps']
  const checks = []
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
      await new Promise((resolve) => setTimeout(resolve, 3000))
    }
    checks.push({ endpoint, ok, status, body: body.slice(0, 200) })
  }
  return checks
}

function chooseRollbackVersion(releases, currentVersion, preferred) {
  if (preferred) return preferred
  const sorted = [...releases].sort()
  if (!sorted.length) return ''
  const idx = sorted.findIndex((item) => item === currentVersion)
  if (idx > 0) return sorted[idx - 1]
  if (idx === -1 && sorted.length >= 2) return sorted[sorted.length - 2]
  return ''
}

async function main() {
  ensureDir(outDir)
  const beginAt = Date.now()
  const stagingHost = arg('--stagingHost', env('STAGING_HOST', ''))
  const stagingUser = arg('--stagingUser', env('STAGING_USER', ''))
  const stagingPort = Number(arg('--stagingPort', env('STAGING_SSH_PORT', '22')))
  const stagingBaseDir = env('STAGING_BASE_DIR', '/opt/crm-staging')
  const composeProjectName = arg('--composeProjectName', env('COMPOSE_PROJECT_NAME', 'crm-staging'))
  const stagingBaseUrl = env('STAGING_BASE_URL', stagingHost ? `http://${stagingHost}` : '')
  const requestedVersion = arg('--artifactVersion', env('ARTIFACT_VERSION', ''))
  const dryRun = env('STAGING_DEPLOY_DRY_RUN', 'false').toLowerCase() === 'true'

  const report = {
    generatedAt: new Date().toISOString(),
    target: {
      host: stagingHost,
      user: stagingUser,
      port: stagingPort,
      baseDir: stagingBaseDir,
      composeProjectName,
      baseUrl: stagingBaseUrl,
    },
    requestedVersion,
    dryRun,
    pass: false,
    selectedRollbackVersion: '',
    steps: [],
    healthChecks: [],
    durationMs: 0,
  }

  if (dryRun) {
    report.pass = true
    report.steps.push({ name: 'dry-run', ok: true, detail: 'rollback not executed' })
    report.durationMs = Date.now() - beginAt
    writeReport(report)
    console.log(`STAGING_ROLLBACK_OK ${path.relative(root, reportFile)}`)
    return
  }
  if (!stagingHost || !stagingUser) throw new Error('missing STAGING_HOST/STAGING_USER for rollback')

  const currentRes = ssh(stagingHost, stagingUser, stagingPort, `basename "$(readlink -f "${stagingBaseDir}/current" || true)"`)
  const currentVersion = currentRes.ok ? currentRes.stdout.trim() : ''
  report.steps.push({ name: 'discover-current', ok: currentRes.ok, detail: currentVersion || currentRes.stderr.trim() })
  if (!currentRes.ok) throw new Error('failed to read current release')

  const releasesRes = ssh(stagingHost, stagingUser, stagingPort, `ls -1 "${stagingBaseDir}/releases" 2>/dev/null | sort`)
  const releases = releasesRes.ok
    ? releasesRes.stdout.split(/\r?\n/).map((s) => s.trim()).filter(Boolean)
    : []
  report.steps.push({ name: 'discover-releases', ok: releasesRes.ok, detail: `${releases.length} release(s) found` })
  if (!releasesRes.ok) throw new Error('failed to list staging releases')

  const rollbackVersion = chooseRollbackVersion(releases, currentVersion, requestedVersion)
  if (!rollbackVersion) throw new Error('no rollback version available')
  report.selectedRollbackVersion = rollbackVersion

  const rollbackRes = ssh(
    stagingHost,
    stagingUser,
    stagingPort,
    [
      `set -e`,
      `ln -sfn "${stagingBaseDir}/releases/${rollbackVersion}" "${stagingBaseDir}/current"`,
      `export RELEASE_DIR="${stagingBaseDir}/current"`,
      `export COMPOSE_PROJECT_NAME="${composeProjectName}"`,
      `docker compose -f "${stagingBaseDir}/current/docker-compose.yml" --project-name "${composeProjectName}" up -d --remove-orphans`,
      `docker compose -f "${stagingBaseDir}/current/docker-compose.yml" --project-name "${composeProjectName}" ps`,
    ].join(' && ')
  )
  report.steps.push({ name: 'rollback-compose-up', ok: rollbackRes.ok, detail: rollbackRes.stderr.trim() || rollbackRes.stdout.trim() })
  if (!rollbackRes.ok) throw new Error('rollback compose up failed')

  if (stagingBaseUrl) {
    report.healthChecks = await healthChecks(stagingBaseUrl)
  }
  const healthPass = !report.healthChecks.length || report.healthChecks.every((c) => c.ok)
  report.steps.push({ name: 'health-check', ok: healthPass, detail: healthPass ? 'all health checks passed' : 'one or more health checks failed' })

  report.pass = report.steps.every((step) => step.ok)
  report.durationMs = Date.now() - beginAt
  writeReport(report)

  if (!report.pass) {
    console.error(`STAGING_ROLLBACK_FAIL ${path.relative(root, reportFile)}`)
    process.exit(1)
  }
  console.log(`STAGING_ROLLBACK_OK ${path.relative(root, reportFile)}`)
}

main().catch((err) => {
  const report = {
    generatedAt: new Date().toISOString(),
    pass: false,
    error: String(err?.message || err),
  }
  writeReport(report)
  console.error(`STAGING_ROLLBACK_ERROR ${err?.message || err}`)
  process.exit(1)
})
