import fs from 'node:fs'
import path from 'node:path'

const repoRoot = process.cwd()
const allowlistPath = path.join(repoRoot, 'scripts', 'utf8-bom-allowlist.json')
const skipDirs = new Set([
  '.git',
  'node_modules',
  'target',
  'dist',
  'build',
  '.idea',
  '.vscode',
  '.m2repo',
  '.qoder',
  '.codeartsdoer',
  'coverage',
  'logs'
])

const textExt = new Set([
  '.java', '.kt', '.xml', '.yml', '.yaml', '.json', '.md',
  '.js', '.jsx', '.ts', '.tsx', '.css', '.scss', '.html',
  '.properties', '.sql', '.sh', '.ps1', '.mjs', '.cjs'
])

function shouldCheck(filePath) {
  const ext = path.extname(filePath).toLowerCase()
  return textExt.has(ext)
}

function walk(dir, out = []) {
  const entries = fs.readdirSync(dir, { withFileTypes: true })
  for (const entry of entries) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      if (skipDirs.has(entry.name)) continue
      walk(full, out)
      continue
    }
    if (shouldCheck(full)) out.push(full)
  }
  return out
}

function hasUtf8Bom(buf) {
  return buf.length >= 3 && buf[0] === 0xef && buf[1] === 0xbb && buf[2] === 0xbf
}

const offenders = []
for (const file of walk(repoRoot)) {
  const buf = fs.readFileSync(file)
  if (hasUtf8Bom(buf)) {
    offenders.push(path.relative(repoRoot, file).replace(/\\/g, '/'))
  }
}

let allowed = []
if (fs.existsSync(allowlistPath)) {
  try {
    const parsed = JSON.parse(fs.readFileSync(allowlistPath, 'utf8'))
    if (Array.isArray(parsed?.allowed)) {
      allowed = parsed.allowed
    }
  } catch (error) {
    console.error(`ENCODING_GUARD_FAIL: invalid allowlist at ${allowlistPath}`)
    console.error(String(error))
    process.exit(1)
  }
}

const allowset = new Set(allowed)
const newOffenders = offenders.filter((file) => !allowset.has(file))
const staleAllowlist = allowed.filter((file) => !offenders.includes(file))

if (newOffenders.length > 0) {
  console.error('ENCODING_GUARD_FAIL: new UTF-8 BOM files detected')
  for (const file of newOffenders) {
    console.error(`- ${file}`)
  }
  process.exit(1)
}

if (offenders.length > 0) {
  console.log(`ENCODING_GUARD_OK baseline=${offenders.length}`)
} else {
  console.log('ENCODING_GUARD_OK baseline=0')
}

if (staleAllowlist.length > 0) {
  console.log('ENCODING_GUARD_CLEANUP stale-allowlist-entries:')
  for (const file of staleAllowlist) {
    console.log(`- ${file}`)
  }
}
