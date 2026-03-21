import { existsSync, readdirSync } from 'node:fs'
import path from 'node:path'

const repoRoot = process.cwd()

const forbiddenRootEntries = [
  'src',
  'public',
  'tests',
  'backend',
  'vite.config.js',
  'playwright.config.js',
  'eslint.config.js',
  'index.html',
]

const requiredEntries = [
  'apps',
  path.join('apps', 'web'),
  path.join('apps', 'api'),
  path.join('apps', 'web', 'src'),
  'scripts',
]

function collectViolations() {
  const violations = []

  for (const entry of forbiddenRootEntries) {
    if (existsSync(path.join(repoRoot, entry))) {
      violations.push(`forbidden root entry exists: ${entry}`)
    }
  }

  for (const entry of requiredEntries) {
    if (!existsSync(path.join(repoRoot, entry))) {
      violations.push(`required entry missing: ${entry}`)
    }
  }

  const rootFiles = readdirSync(repoRoot)
  for (const file of rootFiles) {
    if (/^verify-.*\.png$/i.test(file)) {
      violations.push(`forbidden verification screenshot in root: ${file}`)
    }
  }

  return violations
}

function main() {
  const violations = collectViolations()
  if (violations.length > 0) {
    console.error('REPO_LAYOUT_CHECK_FAILED')
    for (const violation of violations) {
      console.error(`- ${violation}`)
    }
    process.exit(1)
  }

  console.log('REPO_LAYOUT_CHECK_OK')
}

main()
