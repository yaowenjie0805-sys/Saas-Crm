import { spawn } from 'node:child_process'

const DB_USER = process.env.DB_USER || 'root'
const DB_PASSWORD = process.env.DB_PASSWORD || 'root'
const DB_NAME = process.env.DB_NAME || 'crm_local_e2e'
const E2E_FAST = process.env.E2E_FAST === '1'
const IS_WIN = process.platform === 'win32'
const MAVEN_REPO = process.env.MAVEN_REPO || `${process.cwd()}${IS_WIN ? '\\' : '/'}\.m2repo`

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
  const mavenArgs = E2E_FAST
    ? [`-Dmaven.repo.local=${MAVEN_REPO}`, '-f', 'backend/pom.xml', 'package', '-DskipTests']
    : [`-Dmaven.repo.local=${MAVEN_REPO}`, '-f', 'backend/pom.xml', 'clean', 'package', '-DskipTests']
  if (IS_WIN) {
    await run('cmd.exe', ['/c', 'mvn', ...mavenArgs], 'maven package')
  } else {
    await run('mvn', mavenArgs, 'maven package')
  }

  console.log('[full-test] Step 3/4: browser e2e')
  await run(IS_WIN ? 'node.exe' : 'node', ['scripts/run-playwright-e2e.mjs'], 'browser e2e', {
    env: { ...process.env, E2E_SKIP_PACKAGE: '1' },
  })

  console.log('[full-test] Step 4/4: api smoke test')
  await run(IS_WIN ? 'node.exe' : 'node', ['scripts/api-smoke-test.mjs'], 'api smoke test')

  console.log('[full-test] ALL_OK')
}

main().catch((err) => {
  console.error('[full-test] FAIL', err.message)
  process.exitCode = 1
})
