import { execSync, spawn } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'

function normalizePort(rawPort, envName) {
  const parsed = Number.parseInt(String(rawPort), 10)
  if (!Number.isInteger(parsed) || parsed < 1 || parsed > 65535) {
    throw new Error(`[full-test] invalid ${envName}: ${rawPort}`)
  }
  return String(parsed)
}

const DB_USER = process.env.DB_USER || 'root'
const DB_PASSWORD = process.env.DB_PASSWORD || 'root'
const DB_NAME = process.env.DB_NAME || 'crm_local_e2e'
const API_PORT = normalizePort(process.env.API_PORT || '18080', 'API_PORT')
const FRONTEND_PORT = normalizePort(process.env.FRONTEND_PORT || '5173', 'FRONTEND_PORT')
const E2E_FAST = process.env.E2E_FAST === '1'
const IS_WIN = process.platform === 'win32'
const MAVEN_REPO = process.env.MAVEN_REPO || path.join(process.cwd(), '.m2repo')

function removePathIfExists(targetPath) {
  try {
    if (fs.existsSync(targetPath)) {
      fs.rmSync(targetPath, { recursive: true, force: true })
    }
  } catch (error) {
    console.warn(`[full-test] failed to remove ${targetPath}: ${error.message}`)
  }
}

function freePort(port) {
  try {
    if (IS_WIN) {
      const output = execSync(`netstat -ano | findstr :${port}`, { encoding: 'utf8' })
      const pids = [...new Set(output
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter(Boolean)
        .map((line) => line.split(/\s+/))
        .filter((parts) => parts.length >= 5 && parts[3] === 'LISTENING' && parts[1].endsWith(`:${port}`))
        .map((parts) => Number.parseInt(parts[4], 10))
        .filter((pid) => Number.isInteger(pid) && pid > 0 && pid !== process.pid))]
      for (const pid of pids) {
        execSync(`taskkill /PID ${pid} /T /F`, { stdio: 'ignore' })
      }
      return
    }
    const output = execSync(`lsof -tiTCP:${port} -sTCP:LISTEN`, { encoding: 'utf8' })
    output
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter(Boolean)
      .forEach((pid) => {
        const numericPid = Number(pid)
        if (!Number.isInteger(numericPid) || numericPid <= 0 || numericPid === process.pid) return
        process.kill(numericPid, 'SIGKILL')
      })
  } catch {
    // port already free or tooling unavailable
  }
}

function run(cmd, args, label, options = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(cmd, args, { stdio: 'inherit', ...options })

    child.on('error', (err) => {
      reject(new Error(`${label} failed to start: ${err.message}`))
    })

    child.on('close', (code) => {
      if (code === 0) {
        resolve()
        return
      }
      reject(new Error(`${label} failed with exit code ${code}`))
    })
  })
}

async function main() {
  console.log(`[full-test] Mode: ${E2E_FAST ? 'FAST' : 'FULL'}`)
  console.log('[full-test] Step 1/4: init database')
  if (!E2E_FAST) {
    await run(
      IS_WIN ? 'mysql.exe' : 'mysql',
      [`-u${DB_USER}`, `-p${DB_PASSWORD}`, '-e', `DROP DATABASE IF EXISTS ${DB_NAME};`],
      'mysql drop db'
    )
  }
  await run(
    IS_WIN ? 'mysql.exe' : 'mysql',
    [`-u${DB_USER}`, `-p${DB_PASSWORD}`, '-e', `CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`],
    'mysql init'
  )

  console.log('[full-test] Step 2/4: package backend')
  freePort(API_PORT)
  freePort(FRONTEND_PORT)
  removePathIfExists(path.join(process.cwd(), 'apps', 'api', 'target'))
  const mavenArgs = [`-Dmaven.repo.local=${MAVEN_REPO}`, '-f', 'apps/api/pom.xml', 'package', '-Dmaven.test.skip=true']
  if (IS_WIN) {
    await run('cmd.exe', ['/c', 'mvn', ...mavenArgs.slice(0, -1), '"-Dmaven.test.skip=true"'], 'maven package')
  } else {
    await run('mvn', mavenArgs, 'maven package')
  }

  console.log('[full-test] Step 3/4: browser e2e')
  await run(IS_WIN ? 'node.exe' : 'node', ['apps/web/scripts/run-playwright-e2e.mjs'], 'browser e2e', {
    env: {
      ...process.env,
      E2E_SKIP_PACKAGE: '1',
      FRONTEND_PORT: process.env.FRONTEND_PORT || '5173',
      API_PORT: process.env.API_PORT || '18080',
      E2E_FRONTEND_PORT: process.env.FRONTEND_PORT || '5173',
      PLAYWRIGHT_SKIP_WEB_SERVER: '1',
    },
  })

  console.log('[full-test] Step 4/4: api smoke test')
  await run(IS_WIN ? 'node.exe' : 'node', ['scripts/api-smoke-test.mjs'], 'api smoke test')

  console.log('[full-test] ALL_OK')
}

main().catch((err) => {
  console.error('[full-test] FAIL', err.message)
  process.exitCode = 1
})

