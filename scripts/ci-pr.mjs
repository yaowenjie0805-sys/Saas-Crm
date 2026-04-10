import { spawnSync } from 'node:child_process'

const isWin = process.platform === 'win32'
const DEFAULT_TIMEOUT_MS = Number(process.env.CI_SCRIPT_TIMEOUT_MS || 15 * 60 * 1000)

function run(command, args, env = process.env) {
  const result = spawnSync(command, args, {
    stdio: 'inherit',
    env,
    timeout: DEFAULT_TIMEOUT_MS,
  })
  if (result.error && String(result.error.code || '').toUpperCase() === 'ETIMEDOUT') {
    throw new Error(`${command} ${args.join(' ')} timed out after ${DEFAULT_TIMEOUT_MS}ms`)
  }
  if (result.status !== 0) {
    throw new Error(`${command} ${args.join(' ')} failed with status ${result.status}`)
  }
}

function npmRun(script, env = process.env) {
  if (isWin) {
    run('cmd.exe', ['/c', 'npm', 'run', script], env)
    return
  }
  run('npm', ['run', script], env)
}

function main() {
  npmRun('optimize:guard')
  npmRun('repo:check:layout')
  npmRun('lint')
  npmRun('build:frontend')
  npmRun('validate:env')
  npmRun('test:e2e', {
    ...process.env,
    E2E_TEST_GLOB: 'apps/web/tests/e2e/smoke.spec.js,apps/web/tests/e2e/topbar-ai-shortcut.spec.js',
  })
  console.log('CI_PR_OK')
}

main()
