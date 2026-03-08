import { spawn } from 'node:child_process'

const DB_USER = process.env.DB_USER || 'root'
const DB_PASSWORD = process.env.DB_PASSWORD || 'root'
const DB_NAME = process.env.DB_NAME || 'crm_local'
const IS_WIN = process.platform === 'win32'

function run(cmd, args, label) {
  return new Promise((resolve, reject) => {
    const child = spawn(cmd, args, { stdio: 'inherit' })

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
  console.log('[full-test] Step 1/3: init database')
  await run(
    IS_WIN ? 'mysql.exe' : 'mysql',
    [`-u${DB_USER}`, `-p${DB_PASSWORD}`, '-e', `CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`],
    'mysql init'
  )

  console.log('[full-test] Step 2/3: package backend')
  if (IS_WIN) {
    await run('cmd.exe', ['/c', 'mvn', '-f', 'backend/pom.xml', 'clean', 'package', '-DskipTests'], 'maven package')
  } else {
    await run('mvn', ['-f', 'backend/pom.xml', 'clean', 'package', '-DskipTests'], 'maven package')
  }

  console.log('[full-test] Step 3/3: api smoke test')
  await run(IS_WIN ? 'node.exe' : 'node', ['scripts/api-smoke-test.mjs'], 'api smoke test')

  console.log('[full-test] ALL_OK')
}

main().catch((err) => {
  console.error('[full-test] FAIL', err.message)
  process.exitCode = 1
})
