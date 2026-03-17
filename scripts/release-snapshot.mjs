import fs from 'node:fs'
import path from 'node:path'
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
  const distDir = path.join(root, 'dist', 'assets')
  if (fs.existsSync(distDir)) {
    for (const name of fs.readdirSync(distDir)) {
      if (name.endsWith('.js') || name.endsWith('.css')) {
        const full = path.join(distDir, name)
        const size = fs.statSync(full).size
        artifacts.push({ type: 'frontend', file: `dist/assets/${name}`, bytes: size })
      }
    }
  }
  const backendJar = path.join(root, 'backend', 'target', 'crm-backend-1.0.0.jar')
  if (fs.existsSync(backendJar)) {
    artifacts.push({
      type: 'backend',
      file: 'backend/target/crm-backend-1.0.0.jar',
      bytes: fs.statSync(backendJar).size,
    })
  }
  return artifacts
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
}

fs.writeFileSync(outFile, JSON.stringify(snapshot, null, 2))
console.log(`RELEASE_SNAPSHOT_OK ${path.relative(root, outFile)}`)
