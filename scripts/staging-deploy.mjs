import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import crypto from 'node:crypto'
import { spawnSync } from 'node:child_process'

const root = process.cwd()
const outDir = path.join(root, 'logs', 'staging')
const releaseDir = path.join(root, 'logs', 'release')
const stamp = new Date().toISOString().replace(/[:.]/g, '-')
const reportFile = path.join(outDir, `staging-deploy-${stamp}.json`)
const latestFile = path.join(outDir, 'staging-deploy-latest.json')

function ensureDir(dir) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })
}

function run(command, args = [], options = {}) {
  const result = spawnSync(command, args, {
    cwd: root,
    encoding: 'utf8',
    env: { ...process.env, ...(options.env || {}) },
  })
  return {
    ok: result.status === 0,
    status: result.status,
    stdout: result.stdout || '',
    stderr: result.stderr || '',
  }
}

function runGit(args = []) {
  return run('git', args)
}

function getArgValue(flag, fallback = '') {
  const idx = process.argv.indexOf(flag)
  if (idx >= 0 && idx + 1 < process.argv.length) return String(process.argv[idx + 1]).trim()
  return fallback
}

function env(name, fallback = '') {
  const val = process.env[name]
  return typeof val === 'string' && val.trim() ? val.trim() : fallback
}

function latestReleaseSnapshot() {
  if (!fs.existsSync(releaseDir)) return ''
  const files = fs.readdirSync(releaseDir)
    .filter((name) => name.startsWith('release-snapshot-') && name.endsWith('.json'))
    .sort()
  if (!files.length) return ''
  return path.join(releaseDir, files[files.length - 1])
}

function writeReport(report) {
  ensureDir(outDir)
  fs.writeFileSync(reportFile, JSON.stringify(report, null, 2))
  fs.writeFileSync(latestFile, JSON.stringify(report, null, 2))
}

function hashFile(file) {
  if (!fs.existsSync(file)) return ''
  return crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex')
}

function hashDirectoryTree(dir) {
  if (!fs.existsSync(dir)) return ''
  const entries = []
  const stack = [dir]
  while (stack.length) {
    const current = stack.pop()
    for (const name of fs.readdirSync(current)) {
      const full = path.join(current, name)
      const stat = fs.statSync(full)
      if (stat.isDirectory()) {
        stack.push(full)
      } else if (stat.isFile()) {
        entries.push(path.relative(dir, full).replace(/\\/g, '/'))
      }
    }
  }
  entries.sort()
  const hash = crypto.createHash('sha256')
  for (const rel of entries) {
    hash.update(rel)
    hash.update('\n')
    hash.update(fs.readFileSync(path.join(dir, rel)))
    hash.update('\n')
  }
  return hash.digest('hex')
}

function makeBundle(version) {
  const distDir = path.join(root, 'apps', 'web', 'dist')
  const jarFile = path.join(root, 'apps', 'api', 'target', 'crm-backend-1.0.0.jar')
  const composeFile = path.join(root, 'infra', 'staging', 'docker-compose.yml')
  const nginxFile = path.join(root, 'infra', 'staging', 'nginx.conf')
  const envTemplate = path.join(root, 'infra', 'staging', 'staging.env.example')

  if (!fs.existsSync(distDir)) throw new Error('missing dist directory; run npm run build first')
  if (!fs.existsSync(jarFile)) throw new Error('missing backend jar; run mvn package first')
  if (!fs.existsSync(composeFile)) throw new Error('missing infra/staging/docker-compose.yml')
  if (!fs.existsSync(nginxFile)) throw new Error('missing infra/staging/nginx.conf')
  if (!fs.existsSync(envTemplate)) throw new Error('missing infra/staging/staging.env.example')

  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'crm-staging-'))
  const bundleDir = path.join(tempRoot, version)
  fs.mkdirSync(bundleDir, { recursive: true })
  fs.mkdirSync(path.join(bundleDir, 'apps', 'web'), { recursive: true })
  fs.mkdirSync(path.join(bundleDir, 'apps', 'api'), { recursive: true })

  fs.cpSync(distDir, path.join(bundleDir, 'apps', 'web', 'dist'), { recursive: true })
  fs.copyFileSync(jarFile, path.join(bundleDir, 'apps', 'api', 'crm-backend-1.0.0.jar'))
  fs.copyFileSync(composeFile, path.join(bundleDir, 'docker-compose.yml'))
  fs.copyFileSync(nginxFile, path.join(bundleDir, 'apps', 'web', 'nginx.conf'))
  fs.copyFileSync(envTemplate, path.join(bundleDir, '.env'))

  const commit = runGit(['rev-parse', 'HEAD'])
  const branch = runGit(['rev-parse', '--abbrev-ref', 'HEAD'])
  const manifest = {
    generatedAt: new Date().toISOString(),
    artifactVersion: version,
    commit: commit.ok ? commit.stdout.trim() : '',
    branch: branch.ok ? branch.stdout.trim() : '',
    releaseManifest: {
      frontendDistSha256: hashDirectoryTree(distDir),
      backendJarSha256: hashFile(jarFile),
    },
  }
  fs.writeFileSync(path.join(bundleDir, 'manifest.json'), JSON.stringify(manifest, null, 2))
  return { tempRoot, bundleDir, manifest }
}

function ssh(host, user, port, remoteCommand) {
  const knownHostsFile = env('SSH_KNOWN_HOSTS_FILE', path.join(os.homedir(), '.ssh', 'known_hosts'))
  ensureDir(path.dirname(knownHostsFile))
  if (!fs.existsSync(knownHostsFile)) fs.writeFileSync(knownHostsFile, '')
  const target = `${user}@${host}`
  const args = [
    '-p', String(port),
    '-o', 'StrictHostKeyChecking=yes',
    '-o', `UserKnownHostsFile=${knownHostsFile}`,
    target,
    remoteCommand,
  ]
  return run('ssh', args)
}

function scp(host, user, port, sourcePath, targetPath) {
  const knownHostsFile = env('SSH_KNOWN_HOSTS_FILE', path.join(os.homedir(), '.ssh', 'known_hosts'))
  ensureDir(path.dirname(knownHostsFile))
  if (!fs.existsSync(knownHostsFile)) fs.writeFileSync(knownHostsFile, '')
  const target = `${user}@${host}:${targetPath}`
  const args = [
    '-P', String(port),
    '-o', 'StrictHostKeyChecking=yes',
    '-o', `UserKnownHostsFile=${knownHostsFile}`,
    '-r',
    sourcePath,
    target,
  ]
  return run('scp', args)
}

async function waitForHealth(baseUrl) {
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

async function main() {
  ensureDir(outDir)
  const beginAt = Date.now()
  const artifactVersion = getArgValue('--artifactVersion', env('ARTIFACT_VERSION', ''))
    || (runGit(['rev-parse', '--short', 'HEAD']).stdout || '').trim()
  const stagingHost = getArgValue('--stagingHost', env('STAGING_HOST', ''))
  const stagingUser = getArgValue('--stagingUser', env('STAGING_USER', ''))
  const stagingPort = Number(getArgValue('--stagingPort', env('STAGING_SSH_PORT', '22')))
  const composeProjectName = getArgValue('--composeProjectName', env('COMPOSE_PROJECT_NAME', 'crm-staging'))
  const stagingBaseDir = env('STAGING_BASE_DIR', '/opt/crm-staging')
  const stagingBaseUrl = env('STAGING_BASE_URL', stagingHost ? `http://${stagingHost}` : '')
  const dryRun = env('STAGING_DEPLOY_DRY_RUN', 'false').toLowerCase() === 'true'

  const report = {
    generatedAt: new Date().toISOString(),
    artifactVersion,
    target: {
      host: stagingHost,
      user: stagingUser,
      port: stagingPort,
      baseDir: stagingBaseDir,
      composeProjectName,
      baseUrl: stagingBaseUrl,
    },
    dryRun,
    pass: false,
    steps: [],
    healthChecks: [],
    durationMs: 0,
    rollbackRecommendation: 'rollback_recommended',
  }

  if (!artifactVersion) throw new Error('missing artifactVersion')
  const { tempRoot, bundleDir, manifest } = makeBundle(artifactVersion)
  report.manifest = manifest

  if (dryRun) {
    report.steps.push({ name: 'bundle', ok: true, detail: 'bundle prepared in dry-run mode' })
    report.pass = true
    report.rollbackRecommendation = 'none'
    report.durationMs = Date.now() - beginAt
    writeReport(report)
    fs.rmSync(tempRoot, { recursive: true, force: true })
    console.log(`STAGING_DEPLOY_OK ${path.relative(root, reportFile)}`)
    return
  }

  if (!stagingHost || !stagingUser) {
    throw new Error('missing STAGING_HOST/STAGING_USER for non-dry-run deployment')
  }

  const remoteReleaseDir = `${stagingBaseDir}/releases/${artifactVersion}`
  const remoteBootstrap = ssh(
    stagingHost,
    stagingUser,
    stagingPort,
    `mkdir -p "${stagingBaseDir}/releases" "${stagingBaseDir}/current"`
  )
  report.steps.push({ name: 'remote-bootstrap', ok: remoteBootstrap.ok, detail: remoteBootstrap.stderr.trim() || remoteBootstrap.stdout.trim() })
  if (!remoteBootstrap.ok) throw new Error('remote bootstrap failed')

  const upload = scp(stagingHost, stagingUser, stagingPort, path.join(bundleDir, '.'), `${remoteReleaseDir}/`)
  report.steps.push({ name: 'upload-release', ok: upload.ok, detail: upload.stderr.trim() || upload.stdout.trim() })
  if (!upload.ok) throw new Error('upload release bundle failed')

  const composeUp = ssh(
    stagingHost,
    stagingUser,
    stagingPort,
    [
      `set -e`,
      `ln -sfn "${remoteReleaseDir}" "${stagingBaseDir}/current"`,
      `export RELEASE_DIR="${stagingBaseDir}/current"`,
      `export COMPOSE_PROJECT_NAME="${composeProjectName}"`,
      `docker compose -f "${stagingBaseDir}/current/docker-compose.yml" --project-name "${composeProjectName}" pull`,
      `docker compose -f "${stagingBaseDir}/current/docker-compose.yml" --project-name "${composeProjectName}" up -d --remove-orphans`,
      `docker compose -f "${stagingBaseDir}/current/docker-compose.yml" --project-name "${composeProjectName}" ps`,
    ].join(' && ')
  )
  report.steps.push({ name: 'compose-up', ok: composeUp.ok, detail: composeUp.stderr.trim() || composeUp.stdout.trim() })
  if (!composeUp.ok) throw new Error('docker compose up failed')

  if (stagingBaseUrl) {
    report.healthChecks = await waitForHealth(stagingBaseUrl)
  }
  const healthPass = !report.healthChecks.length || report.healthChecks.every((c) => c.ok)
  report.steps.push({ name: 'health-check', ok: healthPass, detail: healthPass ? 'all health checks passed' : 'one or more health checks failed' })
  report.pass = report.steps.every((s) => s.ok)
  report.rollbackRecommendation = report.pass ? 'none' : 'rollback_recommended'
  report.durationMs = Date.now() - beginAt
  writeReport(report)

  fs.rmSync(tempRoot, { recursive: true, force: true })
  if (!report.pass) {
    console.error(`STAGING_DEPLOY_FAIL ${path.relative(root, reportFile)}`)
    process.exit(1)
  }
  console.log(`STAGING_DEPLOY_OK ${path.relative(root, reportFile)}`)
}

main().catch((err) => {
  const report = {
    generatedAt: new Date().toISOString(),
    pass: false,
    error: String(err?.message || err),
    rollbackRecommendation: 'rollback_recommended',
  }
  writeReport(report)
  console.error(`STAGING_DEPLOY_ERROR ${err?.message || err}`)
  process.exit(1)
})
