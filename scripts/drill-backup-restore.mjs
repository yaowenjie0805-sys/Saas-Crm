import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'

const root = process.cwd()
const outDir = path.join(root, 'logs', 'drills')
const now = new Date()
const stamp = now.toISOString().replace(/[:.]/g, '-')
const reportFile = path.join(outDir, `backup-restore-${stamp}.json`)
const latestFile = path.join(outDir, 'backup-restore-latest.json')

function ensureDir(dir) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })
}

function run(command, args, env = {}) {
  const res = spawnSync(command, args, {
    cwd: root,
    env: { ...process.env, ...env },
    encoding: 'utf8',
  })
  return {
    ok: res.status === 0,
    status: res.status,
    stdout: res.stdout || '',
    stderr: res.stderr || '',
  }
}

function writeReport(report) {
  ensureDir(outDir)
  fs.writeFileSync(reportFile, JSON.stringify(report, null, 2))
  fs.writeFileSync(latestFile, JSON.stringify(report, null, 2))
}

function listSqlFiles(dir) {
  if (!fs.existsSync(dir)) return []
  return fs.readdirSync(dir)
    .filter((name) => name.endsWith('.sql'))
    .map((name) => path.join(dir, name))
    .sort()
}

function main() {
  const beginAt = Date.now()
  const backupOutDir = path.join(outDir, `backup-files-${stamp}`)
  const dbHost = process.env.DB_HOST || '127.0.0.1'
  const dbPort = process.env.DB_PORT || '3306'
  const dbUser = process.env.DB_USER || 'root'
  const dbPassword = process.env.DB_PASSWORD || 'root'
  const dbName = process.env.DB_NAME || 'crm_local'
  const tempDbName = `${dbName}_drill_${Date.now() % 100000}`

  const backupRes = run('powershell', [
    '-ExecutionPolicy', 'Bypass',
    '-File', 'scripts/db-backup.ps1',
    '-OutDir', backupOutDir,
  ], {
    DB_HOST: dbHost,
    DB_PORT: dbPort,
    DB_USER: dbUser,
    DB_PASSWORD: dbPassword,
    DB_NAME: dbName,
  })

  const sqlFiles = listSqlFiles(backupOutDir)
  const latestSql = sqlFiles.length > 0 ? sqlFiles[sqlFiles.length - 1] : ''

  const createTempRes = run('mysql', [
    `-h${dbHost}`,
    `-P${dbPort}`,
    `-u${dbUser}`,
    `-p${dbPassword}`,
    '-e',
    `CREATE DATABASE IF NOT EXISTS ${tempDbName} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`,
  ])

  const restoreRes = latestSql
    ? run('powershell', [
      '-ExecutionPolicy', 'Bypass',
      '-File', 'scripts/db-restore.ps1',
      '-BackupFile', latestSql,
    ], {
      DB_HOST: dbHost,
      DB_PORT: dbPort,
      DB_USER: dbUser,
      DB_PASSWORD: dbPassword,
      DB_NAME: tempDbName,
    })
    : { ok: false, status: 1, stdout: '', stderr: 'backup_sql_not_found' }

  const verifyRes = run('mysql', [
    `-h${dbHost}`,
    `-P${dbPort}`,
    `-u${dbUser}`,
    `-p${dbPassword}`,
    '-D',
    tempDbName,
    '-e',
    'SHOW TABLES;',
  ])

  const cleanupRes = run('mysql', [
    `-h${dbHost}`,
    `-P${dbPort}`,
    `-u${dbUser}`,
    `-p${dbPassword}`,
    '-e',
    `DROP DATABASE IF EXISTS ${tempDbName};`,
  ])

  const checkpoints = {
    backupGenerated: backupRes.ok && !!latestSql,
    tempDatabaseCreated: createTempRes.ok,
    restoreExecuted: restoreRes.ok,
    restoredSchemaReadable: verifyRes.ok,
    tempDatabaseDropped: cleanupRes.ok,
  }
  const pass = Object.values(checkpoints).every((v) => v === true)

  const report = {
    generatedAt: new Date().toISOString(),
    type: 'backup-restore',
    pass,
    durationMs: Date.now() - beginAt,
    sourceDatabase: dbName,
    tempDatabase: tempDbName,
    backupFile: latestSql ? path.relative(root, latestSql) : '',
    checkpoints,
    commandResults: {
      backup: { ok: backupRes.ok, status: backupRes.status },
      createTempDb: { ok: createTempRes.ok, status: createTempRes.status },
      restore: { ok: restoreRes.ok, status: restoreRes.status },
      verify: { ok: verifyRes.ok, status: verifyRes.status },
      cleanup: { ok: cleanupRes.ok, status: cleanupRes.status },
    },
  }
  writeReport(report)
  if (!pass) {
    console.error(`DRILL_BACKUP_RESTORE_FAIL ${path.relative(root, reportFile)}`)
    process.exit(1)
  }
  console.log(`DRILL_BACKUP_RESTORE_OK ${path.relative(root, reportFile)}`)
}

try {
  main()
} catch (err) {
  writeReport({
    generatedAt: new Date().toISOString(),
    type: 'backup-restore',
    pass: false,
    error: err?.message || String(err),
  })
  console.error(`DRILL_BACKUP_RESTORE_ERROR ${err?.message || err}`)
  process.exit(1)
}
