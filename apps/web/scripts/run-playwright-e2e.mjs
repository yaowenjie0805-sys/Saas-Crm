import { execSync, spawn } from 'node:child_process'

const DB_USER = process.env.DB_USER || 'root'
const DB_PASSWORD = process.env.DB_PASSWORD || 'root'
const RUN_ID = Date.now()
const DB_NAME = process.env.DB_NAME || `crm_local_e2e_${RUN_ID}`
const API_PORT = process.env.API_PORT || '8080'
const FRONTEND_PORT = process.env.FRONTEND_PORT || '14173'
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
const MAVEN_REPO = process.env.MAVEN_REPO || `${process.cwd()}${IS_WIN ? '\\' : '/'} .m2repo`.replace(' .m2repo', '.m2repo')
const SKIP_PREPARE = process.env.E2E_SKIP_PREPARE === '1'
const SKIP_PACKAGE = process.env.E2E_SKIP_PACKAGE === '1'
const SKIP_FRONTEND_BUILD = process.env.E2E_SKIP_FRONTEND_BUILD === '1'
const FRONTEND_MODE = process.env.E2E_FRONTEND_MODE || 'dev'
const E2E_TEST_GLOB = process.env.E2E_TEST_GLOB || ''
const E2E_TEST_FILES = E2E_TEST_GLOB
  .split(',')
  .map((item) => item.trim())
  .filter(Boolean)

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
  const child = spawn(command, args, {
    cwd: process.cwd(),
    stdio: ['ignore', 'pipe', 'pipe'],
    ...options,
  })
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
      const pids = [...new Set(
        output
          .split(/\r?\n/)
          .map((line) => line.trim())
          .filter(Boolean)
          .map((line) => line.split(/\s+/).pop())
          .filter((pid) => pid && pid !== '0'),
      )]
      for (const pid of pids) {
        execSync(`taskkill /PID ${pid} /T /F`, { stdio: 'ignore' })
      }
      return
    }
    const output = execSync(`lsof -ti tcp:${port}`, { encoding: 'utf8' })
    output
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter(Boolean)
      .forEach((pid) => {
        process.kill(Number(pid), 'SIGKILL')
      })
  } catch {
    // port already free or tooling unavailable
  }
}

async function killProcessTree(child) {
  if (!child?.pid) return
  try {
    if (IS_WIN) {
      await run('taskkill', ['/PID', String(child.pid), '/T', '/F'])
      return
    }
    process.kill(-child.pid, 'SIGTERM')
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
  try {
    await ensureDatabase()
  } catch (error) {
    console.warn(`[e2e] database bootstrap skipped: ${error.message}`)
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
      await run('cmd.exe', ['/c', 'mvn', `-Dmaven.repo.local=${MAVEN_REPO}`, '-f', 'apps/api/pom.xml', 'package', '-DskipTests'])
    }
    return
  }
  if (!SKIP_FRONTEND_BUILD && FRONTEND_MODE !== 'dev') {
    await run('npm', ['run', 'build'], { env })
  }
  if (!SKIP_PACKAGE) {
    await run('mvn', [`-Dmaven.repo.local=${MAVEN_REPO}`, '-f', 'apps/api/pom.xml', 'package', '-DskipTests'])
  }
}

async function runPlaywright() {
  const env = {
    ...process.env,
    E2E_BASE_URL: BASE_URL,
    E2E_API_BASE_URL: API_BASE_URL,
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
    'apps/api/target/crm-backend-1.0.0.jar',
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
    for (const proc of [frontend, backend]) {
      await killProcessTree(proc.child)
    }
    await new Promise((resolve) => setTimeout(resolve, 500))
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
    await stopAll()
  }
}

main().catch((error) => {
  console.error('[e2e] FAIL')
  console.error(error.message)
  process.exitCode = 1
})

