import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'

const root = process.cwd()
const outDir = path.join(root, 'logs', 'drills')
const releaseDir = path.join(root, 'logs', 'release')
const now = new Date()
const stamp = now.toISOString().replace(/[:.]/g, '-')
const reportFile = path.join(outDir, `rollback-${stamp}.json`)
const latestFile = path.join(outDir, 'rollback-latest.json')

function ensureDir(dir) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })
}

function run(cmd, args) {
  const res = spawnSync(cmd, args, {
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

function runShell(commandLine) {
  const isWin = process.platform === 'win32'
  return isWin ? run('cmd.exe', ['/c', commandLine]) : run('sh', ['-lc', commandLine])
}

function listSnapshots() {
  if (!fs.existsSync(releaseDir)) return []
  return fs.readdirSync(releaseDir)
    .filter((name) => name.startsWith('release-snapshot-') && name.endsWith('.json'))
    .map((name) => path.join(releaseDir, name))
    .sort()
}

function writeReport(report) {
  ensureDir(outDir)
  fs.writeFileSync(reportFile, JSON.stringify(report, null, 2))
  fs.writeFileSync(latestFile, JSON.stringify(report, null, 2))
}

function main() {
  const beginAt = Date.now()
  const headRes = runShell('git rev-parse HEAD')
  const rollbackTargetRes = runShell('git rev-parse HEAD~1')
  const releaseSnapshotRes = runShell('npm run release:snapshot')
  const snapshots = listSnapshots()
  const latestSnapshot = snapshots.length > 0 ? snapshots[snapshots.length - 1] : ''
  const e2eRes = runShell('npm run test:e2e')

  const checkpoints = {
    currentCommitResolved: headRes.ok,
    rollbackTargetResolved: rollbackTargetRes.ok,
    releaseSnapshotGenerated: releaseSnapshotRes.ok,
    releaseSnapshotExists: !!latestSnapshot,
    rollbackVerificationE2E: e2eRes.ok,
  }
  const pass = Object.values(checkpoints).every((v) => v === true)

  const report = {
    generatedAt: new Date().toISOString(),
    type: 'rollback',
    pass,
    durationMs: Date.now() - beginAt,
    currentCommit: headRes.ok ? headRes.stdout.trim() : '',
    rollbackTargetCommit: rollbackTargetRes.ok ? rollbackTargetRes.stdout.trim() : '',
    latestReleaseSnapshot: latestSnapshot ? path.relative(root, latestSnapshot) : '',
    checkpoints,
    commandResults: {
      releaseSnapshot: { ok: releaseSnapshotRes.ok, status: releaseSnapshotRes.status },
      rollbackVerificationE2E: { ok: e2eRes.ok, status: e2eRes.status },
    },
  }
  writeReport(report)
  if (!pass) {
    console.error(`DRILL_ROLLBACK_FAIL ${path.relative(root, reportFile)}`)
    process.exit(1)
  }
  console.log(`DRILL_ROLLBACK_OK ${path.relative(root, reportFile)}`)
}

try {
  main()
} catch (err) {
  writeReport({
    generatedAt: new Date().toISOString(),
    type: 'rollback',
    pass: false,
    error: err?.message || String(err),
  })
  console.error(`DRILL_ROLLBACK_ERROR ${err?.message || err}`)
  process.exit(1)
}
