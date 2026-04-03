import { execSync, spawn } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'

function normalizePort(rawPort, envName) {
  const parsed = Number.parseInt(String(rawPort), 10)
  if (!Number.isInteger(parsed) || parsed < 1 || parsed > 65535) {
    throw new Error(`[e2e] invalid ${envName}: ${rawPort}`)
  }
  return String(parsed)
}

const DB_USER = process.env.DB_USER || 'root'
const DB_PASSWORD = process.env.DB_PASSWORD || 'root'
const RUN_ID = Date.now()
const DB_NAME = process.env.DB_NAME || `crm_local_e2e_${RUN_ID}`
const API_PORT = normalizePort(process.env.API_PORT || '18080', 'API_PORT')
const FRONTEND_PORT = normalizePort(process.env.FRONTEND_PORT || '5173', 'FRONTEND_PORT')
const HOST = '127.0.0.1'
const BASE_URL = `http://${HOST}:${FRONTEND_PORT}`
const API_BASE_URL = `http://${HOST}:${API_PORT}/api`
const CORS_ALLOWED_ORIGINS = [
  `http://${HOST}:${FRONTEND_PORT}`,
  `http://localhost:${FRONTEND_PORT}`,
  'http://localhost:5173',
  'http://127.0.0.1:5173',
  'http://localhost:14173',
  'http://127.0.0.1:14173',
].join(',')
const DB_URL = process.env.DB_URL || `jdbc:mysql://${HOST}:3306/${DB_NAME}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true`
const IS_WIN = process.platform === 'win32'
const MAVEN_REPO = process.env.MAVEN_REPO || path.join(process.cwd(), '.m2repo')
const SKIP_PREPARE = process.env.E2E_SKIP_PREPARE === '1'
const SKIP_PACKAGE = process.env.E2E_SKIP_PACKAGE === '1'
const SKIP_FRONTEND_BUILD = process.env.E2E_SKIP_FRONTEND_BUILD === '1'
const FRONTEND_MODE = process.env.E2E_FRONTEND_MODE || 'dev'
const E2E_TEST_GLOB = process.env.E2E_TEST_GLOB || ''
const E2E_TEST_FILES = E2E_TEST_GLOB
  .split(',')
  .map((item) => item.trim())
  .filter(Boolean)

function resolveBackendJarPath() {
  if (process.env.E2E_BACKEND_JAR) {
    const explicitPath = path.resolve(process.cwd(), process.env.E2E_BACKEND_JAR)
    if (!fs.existsSync(explicitPath)) {
      throw new Error(`configured backend jar not found: ${explicitPath}`)
    }
    return explicitPath
  }
  const targetDir = path.join(process.cwd(), 'apps', 'api', 'target')
  if (!fs.existsSync(targetDir)) {
    throw new Error(`backend target directory not found: ${targetDir}`)
  }
  const candidates = fs
    .readdirSync(targetDir)
    .filter((name) => name.endsWith('.jar'))
    .filter((name) => /^crm-backend-.*\.jar$/i.test(name))
    .filter((name) => !name.includes('-sources') && !name.includes('-javadoc') && !name.includes('-original'))
    .map((name) => {
      const fullPath = path.join(targetDir, name)
      const stat = fs.statSync(fullPath)
      return { fullPath, mtimeMs: stat.mtimeMs }
    })
    .sort((a, b) => b.mtimeMs - a.mtimeMs)
  if (candidates.length === 0) {
    throw new Error(`no executable backend jar found under: ${targetDir}`)
  }
  return candidates[0].fullPath
}

function run(command, args, options = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, {
      cwd: process.cwd(),
      stdio: 'inherit',
      ...options,
    })
    child.on('error', (err) => reject(new Error(`${command} failed to start: ${err.message}`)))
    child.on('close', (code) => {
      if (code === 0) {
        resolve()
        return
      }
      reject(new Error(`${command} exited with code ${code}`))
    })
  })
}

function startBuffered(label, command, args, options = {}) {
  const chunks = []
  let child
  try {
    child = spawn(command, args, {
      cwd: process.cwd(),
      stdio: ['ignore', 'pipe', 'pipe'],
      ...options,
    })
  } catch (error) {
    throw new Error(`${label} failed to start: ${error.message}`)
  }
  const append = (chunk) => {
    chunks.push(String(chunk))
    if (chunks.length > 200) chunks.shift()
  }
  child.stdout.on('data', append)
  child.stderr.on('data', append)
  return {
    child,
    label,
    logs() {
      return chunks.join('')
    },
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

function removePathIfExists(targetPath) {
  try {
    if (fs.existsSync(targetPath)) {
      fs.rmSync(targetPath, { recursive: true, force: true })
    }
  } catch (error) {
    console.warn(`[e2e] failed to remove ${targetPath}: ${error.message}`)
  }
}

async function killProcessTree(child) {
  if (!child?.pid) return
  try {
    if (IS_WIN) {
      await run('taskkill', ['/PID', String(child.pid), '/T', '/F'])
      return
    }
    try {
      process.kill(-child.pid, 'SIGTERM')
    } catch {
      process.kill(child.pid, 'SIGTERM')
    }
  } catch {
    // process already gone
  }
}

async function waitFor(check, label, timeoutMs = 90_000, intervalMs = 400) {
  const startedAt = Date.now()
  while (Date.now() - startedAt < timeoutMs) {
    if (await check()) return
    await new Promise((resolve) => setTimeout(resolve, intervalMs))
  }
  throw new Error(`${label} was not ready within ${timeoutMs}ms`)
}

async function waitForHttp(url, label) {
  await waitFor(async () => {
    try {
      const response = await fetch(url)
      return response.ok
    } catch {
      return false
    }
  }, label)
}

async function ensureDatabase() {
  await run(IS_WIN ? 'mysql.exe' : 'mysql', [
    `-u${DB_USER}`,
    `-p${DB_PASSWORD}`,
    '-e',
    `CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`,
  ])
}

async function prepareArtifacts() {
  freePort(API_PORT)
  freePort(FRONTEND_PORT)
  try {
    await ensureDatabase()
  } catch (error) {
    console.warn(`[e2e] database bootstrap skipped: ${error.message}`)
  }
  if (!SKIP_PACKAGE) {
    removePathIfExists(path.join(process.cwd(), 'apps', 'api', 'target'))
  }
  const env = {
    ...process.env,
    VITE_API_BASE_URL: API_BASE_URL,
  }
  if (IS_WIN) {
    if (!SKIP_FRONTEND_BUILD && FRONTEND_MODE !== 'dev') {
      await run('cmd.exe', ['/c', 'npm.cmd', 'run', 'build'], { env })
    }
    if (!SKIP_PACKAGE) {
      await run('cmd.exe', ['/c', 'mvn', `-Dmaven.repo.local=${MAVEN_REPO}`, '-f', 'apps/api/pom.xml', 'package', '"-Dmaven.test.skip=true"'])
    }
    return
  }
  if (!SKIP_FRONTEND_BUILD && FRONTEND_MODE !== 'dev') {
    await run('npm', ['run', 'build'], { env })
  }
  if (!SKIP_PACKAGE) {
    await run('mvn', [`-Dmaven.repo.local=${MAVEN_REPO}`, '-f', 'apps/api/pom.xml', 'package', '-Dmaven.test.skip=true'])
  }
}

async function runPlaywright() {
  const env = {
    ...process.env,
    E2E_BASE_URL: BASE_URL,
    E2E_API_BASE_URL: API_BASE_URL,
    E2E_FRONTEND_PORT: FRONTEND_PORT,
    PLAYWRIGHT_SKIP_WEB_SERVER: '1',
  }
  const args = ['playwright', 'test', '--config', 'apps/web/playwright.config.js', ...E2E_TEST_FILES]
  if (IS_WIN) {
    await run('cmd.exe', ['/c', 'npx.cmd', ...args], { env })
    return
  }
  await run('npx', args, { env })
}

async function main() {
  if (!SKIP_PREPARE) {
    console.log('[e2e] preparing build artifacts')
    await prepareArtifacts()
  }
  const backendJarPath = resolveBackendJarPath()

  console.log('[e2e] starting backend and frontend')
  freePort(API_PORT)
  freePort(FRONTEND_PORT)
  const backend = startBuffered('backend', 'java', [
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
    `-DSECURITY_CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS}`,
    '-jar',
    backendJarPath,
  ])
  const frontend = startBuffered(
    'frontend',
    IS_WIN ? 'cmd.exe' : 'npm',
    IS_WIN
      ? (
          FRONTEND_MODE === 'dev'
            ? ['/c', 'npm.cmd', 'run', 'dev', '--workspace', 'apps/web', '--', '--host', HOST, '--port', FRONTEND_PORT, '--strictPort']
            : ['/c', 'npm.cmd', 'run', 'preview', '--workspace', 'apps/web', '--', '--host', HOST, '--port', FRONTEND_PORT, '--strictPort']
        )
      : (
          FRONTEND_MODE === 'dev'
            ? ['run', 'dev', '--workspace', 'apps/web', '--', '--host', HOST, '--port', FRONTEND_PORT, '--strictPort']
            : ['run', 'preview', '--workspace', 'apps/web', '--', '--host', HOST, '--port', FRONTEND_PORT, '--strictPort']
        ),
    {
      env: {
        ...process.env,
        VITE_API_BASE_URL: API_BASE_URL,
      },
    },
  )

  const stopAll = async () => {
    if (stopAll.stopped) return
    stopAll.stopped = true
    for (const proc of [frontend, backend]) {
      await killProcessTree(proc.child)
    }
    await new Promise((resolve) => setTimeout(resolve, 500))
  }
  stopAll.stopped = false

  const signalCleanup = async (signal) => {
    await stopAll()
    process.exit(128 + (signal === 'SIGINT' ? 2 : signal === 'SIGTERM' ? 15 : 1))
  }

  const signalHandlers = new Map([
    ['SIGINT', () => { void signalCleanup('SIGINT') }],
    ['SIGTERM', () => { void signalCleanup('SIGTERM') }],
  ])
  for (const [signal, handler] of signalHandlers) {
    process.once(signal, handler)
  }

  try {
    await waitForHttp(`${API_BASE_URL}/health`, 'backend health endpoint')
    await waitForHttp(`${BASE_URL}/`, 'frontend preview')
    console.log('[e2e] running Playwright suite')
    await runPlaywright()
    console.log('[e2e] PLAYWRIGHT_OK')
  } catch (error) {
    const details = [
      error.message,
      backend.logs().trim() ? `--- backend logs ---\n${backend.logs().trim()}` : '',
      frontend.logs().trim() ? `--- frontend logs ---\n${frontend.logs().trim()}` : '',
    ].filter(Boolean).join('\n')
    throw new Error(details)
  } finally {
    for (const [signal, handler] of signalHandlers) {
      process.off(signal, handler)
    }
    await stopAll()
  }
}

main().catch((error) => {
  console.error('[e2e] FAIL')
  console.error(error.message)
  process.exitCode = 1
})

