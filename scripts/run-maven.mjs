import { spawn } from 'node:child_process'
import { existsSync } from 'node:fs'
import { resolve } from 'node:path'
import process from 'node:process'

const IS_WIN = process.platform === 'win32'
const cwd = process.cwd()
const repo = process.env.MAVEN_REPO || `${cwd}${IS_WIN ? '\\' : '/'}\.m2repo`
const projectGlobalSettings = resolve(cwd, '.mvn', 'global-settings.xml')
const projectUserSettings = resolve(cwd, '.mvn', 'settings.xml')

const mavenArgs = [`-Dmaven.repo.local=${repo}`]

if (existsSync(projectGlobalSettings)) {
  mavenArgs.push('-gs', projectGlobalSettings)
}
if (process.env.MAVEN_SETTINGS) {
  mavenArgs.push('-s', process.env.MAVEN_SETTINGS)
} else if (existsSync(projectUserSettings)) {
  mavenArgs.push('-s', projectUserSettings)
}

if (process.env.MAVEN_INSECURE_SSL === '1' || process.env.MAVEN_INSECURE_SSL === 'true') {
  mavenArgs.push('-Dmaven.resolver.transport=wagon')
  mavenArgs.push('-Dmaven.wagon.http.ssl.insecure=true')
  mavenArgs.push('-Dmaven.wagon.http.ssl.allowall=true')
  mavenArgs.push('-Dmaven.wagon.http.ssl.ignore.validity.dates=true')
}
mavenArgs.push(...process.argv.slice(2))

const command = IS_WIN ? 'cmd.exe' : 'mvn'
const args = IS_WIN ? ['/c', 'mvn', ...mavenArgs] : mavenArgs
const commandLine = `${command} ${args.join(' ')}`

console.log(`[run-maven] platform=${process.platform} node=${process.version}`)
console.log(`[run-maven] repo=${repo}`)
console.log(`[run-maven] command=${commandLine}`)

const child = spawn(command, args, {
  stdio: 'inherit',
})

child.on('exit', (code, signal) => {
  if (signal) {
    console.error(`[run-maven] terminated by signal=${signal}`)
    process.kill(process.pid, signal)
    return
  }
  if ((code ?? 1) !== 0) {
    console.error(`[run-maven] exited with code=${code}`)
  }
  process.exit(code ?? 1)
})

child.on('error', (error) => {
  console.error('[run-maven] failed to spawn maven command')
  console.error(`[run-maven] message=${error?.message || error}`)
  if (error?.code) console.error(`[run-maven] code=${error.code}`)
  if (error?.errno != null) console.error(`[run-maven] errno=${error.errno}`)
  if (error?.syscall) console.error(`[run-maven] syscall=${error.syscall}`)
  if (error?.path) console.error(`[run-maven] path=${error.path}`)
  if (Array.isArray(error?.spawnargs)) {
    console.error(`[run-maven] spawnargs=${error.spawnargs.join(' ')}`)
  }
  process.exit(1)
})
