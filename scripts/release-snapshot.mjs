import fs from 'node:fs'
import path from 'node:path'
import crypto from 'node:crypto'
import { execSync } from 'node:child_process'

const root = process.cwd()
const outDir = path.join(root, 'logs', 'release')
const now = new Date()
const stamp = now.toISOString().replace(/[:.]/g, '-')
const outFile = path.join(outDir, `release-snapshot-${stamp}.json`)

function ensureDir(dir) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })
}

function run(command) {
  try {
    return execSync(command, { cwd: root, stdio: ['ignore', 'pipe', 'ignore'] }).toString().trim()
  } catch {
    return ''
  }
}

function listArtifacts() {
  const artifacts = []
  const distDir = path.join(root, 'apps', 'web', 'dist', 'assets')
  if (fs.existsSync(distDir)) {
    for (const name of fs.readdirSync(distDir)) {
      if (name.endsWith('.js') || name.endsWith('.css')) {
        const full = path.join(distDir, name)
        const size = fs.statSync(full).size
        artifacts.push({ type: 'frontend', file: `apps/web/dist/assets/${name}`, bytes: size })
      }
    }
  }
  const backendJar = path.join(root, 'apps', 'api', 'target', 'crm-backend-1.0.0.jar')
  if (fs.existsSync(backendJar)) {
    artifacts.push({
      type: 'backend',
      file: 'apps/api/target/crm-backend-1.0.0.jar',
      bytes: fs.statSync(backendJar).size,
    })
  }
  const perfAcceptance = path.join(root, 'logs', 'perf', 'perf-acceptance-latest.json')
  if (fs.existsSync(perfAcceptance)) {
    artifacts.push({
      type: 'perf',
      file: 'logs/perf/perf-acceptance-latest.json',
      bytes: fs.statSync(perfAcceptance).size,
    })
  }
  return artifacts
}

function hashFile(file) {
  if (!fs.existsSync(file)) return ''
  const buf = fs.readFileSync(file)
  return crypto.createHash('sha256').update(buf).digest('hex')
}

function hashFrontendDist() {
  const distDir = path.join(root, 'apps', 'web', 'dist')
  if (!fs.existsSync(distDir)) return ''
  const files = []
  const stack = [distDir]
  while (stack.length) {
    const current = stack.pop()
    for (const name of fs.readdirSync(current)) {
      const full = path.join(current, name)
      const stat = fs.statSync(full)
      if (stat.isDirectory()) {
        stack.push(full)
      } else if (stat.isFile()) {
        files.push(path.relative(distDir, full).replace(/\\/g, '/'))
      }
    }
  }
  files.sort()
  const hash = crypto.createHash('sha256')
  for (const rel of files) {
    const full = path.join(distDir, rel)
    hash.update(rel)
    hash.update('\n')
    hash.update(fs.readFileSync(full))
    hash.update('\n')
  }
  return hash.digest('hex')
}

ensureDir(outDir)

const snapshot = {
  generatedAt: now.toISOString(),
  commit: run('git rev-parse HEAD'),
  branch: run('git rev-parse --abbrev-ref HEAD'),
  packageVersion: JSON.parse(fs.readFileSync(path.join(root, 'package.json'), 'utf8')).version,
  backendProfile: process.env.SPRING_PROFILES_ACTIVE || 'dev',
  configSnapshot: {
    AUTH_COOKIE_SECURE: process.env.AUTH_COOKIE_SECURE || 'unset',
    SECURITY_SSO_MODE: process.env.SECURITY_SSO_MODE || 'unset',
    APP_SEED_ENABLED: process.env.APP_SEED_ENABLED || 'unset',
    APP_SEED_DEMO_ENABLED: process.env.APP_SEED_DEMO_ENABLED || 'unset',
  },
  artifacts: listArtifacts(),
  releaseManifest: {
    frontendDistSha256: hashFrontendDist(),
    backendJarSha256: hashFile(path.join(root, 'apps', 'api', 'target', 'crm-backend-1.0.0.jar')),
  },
}

fs.writeFileSync(outFile, JSON.stringify(snapshot, null, 2))
console.log(`RELEASE_SNAPSHOT_OK ${path.relative(root, outFile)}`)

