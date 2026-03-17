import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'

const root = process.cwd()
const outDir = path.join(root, 'logs', 'staging')
const stamp = new Date().toISOString().replace(/[:.]/g, '-')
const reportFile = path.join(outDir, `staging-release-${stamp}.json`)
const latestFile = path.join(outDir, 'staging-release-latest.json')

function ensureDir(dir) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })
}

function run(cmd, args = []) {
  const res = spawnSync(cmd, args, { cwd: root, encoding: 'utf8' })
  return { ok: res.status === 0, out: (res.stdout || '').trim() }
}

function newestFile(dir, prefix, suffix) {
  if (!fs.existsSync(dir)) return ''
  const files = fs.readdirSync(dir)
    .filter((name) => name.startsWith(prefix) && name.endsWith(suffix))
    .sort()
  if (files.length === 0) return ''
  return path.join(dir, files[files.length - 1])
}

function readJson(file) {
  if (!file || !fs.existsSync(file)) return null
  try {
    return JSON.parse(fs.readFileSync(file, 'utf8'))
  } catch {
    return null
  }
}

function main() {
  ensureDir(outDir)

  const releaseSnapshotFile = newestFile(path.join(root, 'logs', 'release'), 'release-snapshot-', '.json')
  const preflightFile = path.join(root, 'logs', 'preflight', 'preflight-latest.json')
  const perfGateFile = path.join(root, 'logs', 'perf', 'perf-gate-latest.json')
  const sreFile = path.join(root, 'logs', 'sre', 'daily-latest.json')
  const stagingVerifyFile = path.join(root, 'logs', 'staging', 'staging-verify-latest.json')
  const stagingDeployFile = path.join(root, 'logs', 'staging', 'staging-deploy-latest.json')
  const stagingRollbackFile = path.join(root, 'logs', 'staging', 'staging-rollback-latest.json')
  const securityFile = path.join(root, 'logs', 'security', 'security-scan-latest.json')

  const releaseSnapshot = readJson(releaseSnapshotFile)
  const preflight = readJson(preflightFile)
  const perfGate = readJson(perfGateFile)
  const sre = readJson(sreFile)
  const stagingDeploy = readJson(stagingDeployFile)
  const stagingVerify = readJson(stagingVerifyFile)
  const stagingRollback = readJson(stagingRollbackFile)
  const security = readJson(securityFile)

  const commit = run('git', ['rev-parse', 'HEAD'])
  const branch = run('git', ['rev-parse', '--abbrev-ref', 'HEAD'])
  const operator = process.env.GITHUB_ACTOR || process.env.STAGING_OPERATOR || process.env.USERNAME || process.env.USER || 'unknown'
  const approvalStatus = process.env.STAGING_APPROVAL_STATUS || (process.env.GITHUB_ACTIONS ? 'environment-gated' : 'local')

  const pass = !!preflight?.pass
    && !!perfGate?.pass
    && !!sre?.verdict?.pass
    && !!stagingDeploy?.pass
    && !!stagingVerify?.pass
    && !!security?.pass

  const report = {
    generatedAt: new Date().toISOString(),
    environment: 'staging',
    pass,
    commit: commit.ok ? commit.out : '',
    branch: branch.ok ? branch.out : '',
    evidence: {
      releaseSnapshot: releaseSnapshotFile ? path.relative(root, releaseSnapshotFile) : '',
      preflight: path.relative(root, preflightFile),
      perfGate: path.relative(root, perfGateFile),
      sreDaily: path.relative(root, sreFile),
      stagingDeploy: path.relative(root, stagingDeployFile),
      stagingVerify: path.relative(root, stagingVerifyFile),
      stagingRollback: fs.existsSync(stagingRollbackFile) ? path.relative(root, stagingRollbackFile) : '',
      securityScan: path.relative(root, securityFile),
    },
    target: {
      stagingHost: process.env.STAGING_HOST || '',
      stagingBaseUrl: process.env.STAGING_BASE_URL || '',
      composeProjectName: process.env.COMPOSE_PROJECT_NAME || 'crm-staging',
    },
    approval: {
      status: approvalStatus,
      operator,
      workflow: process.env.GITHUB_WORKFLOW || '',
      runId: process.env.GITHUB_RUN_ID || '',
    },
    artifact: {
      version: stagingDeploy?.artifactVersion || releaseSnapshot?.commit || '',
      commit: commit.ok ? commit.out : '',
      configSnapshot: releaseSnapshot?.configSnapshot || {},
    },
    gate: {
      preflight: !!preflight?.pass,
      perf: !!perfGate?.pass,
      sre: !!sre?.verdict?.pass,
      stagingDeploy: !!stagingDeploy?.pass,
      stagingVerify: !!stagingVerify?.pass,
      security: !!security?.pass,
    },
    rollbackRecommendation: pass ? 'none' : 'rollback_recommended',
  }

  fs.writeFileSync(reportFile, JSON.stringify(report, null, 2))
  fs.writeFileSync(latestFile, JSON.stringify(report, null, 2))

  if (!pass) {
    console.error(`STAGING_RELEASE_SUMMARY_FAIL ${path.relative(root, reportFile)}`)
    process.exit(1)
  }
  console.log(`STAGING_RELEASE_SUMMARY_OK ${path.relative(root, reportFile)}`)
}

main()
