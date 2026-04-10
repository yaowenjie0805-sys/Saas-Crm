import fs from 'node:fs'
import path from 'node:path'

const repoRoot = process.cwd()
const now = new Date()
const stamp = now.toISOString().replace(/[:.]/g, '-')
const reportDir = path.join(repoRoot, 'logs', 'optimization')
const reportPath = path.join(reportDir, `auto-audit-${stamp}.md`)

const rootNoiseDirs = ['.idea', '.vscode', '.m2repo', '.qoder', '.codeartsdoer']
const docsMojibakeTokens = ['閺', '鈧', '鍞', '妫', '鐤', '锟�']

function walkFiles(dir, filter = () => true, out = []) {
  if (!fs.existsSync(dir)) return out
  const entries = fs.readdirSync(dir, { withFileTypes: true })
  for (const entry of entries) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      walkFiles(full, filter, out)
      continue
    }
    if (filter(full)) out.push(full)
  }
  return out
}

function rel(p) {
  return path.relative(repoRoot, p).replace(/\\/g, '/')
}

function readTextSafe(filePath) {
  try {
    return fs.readFileSync(filePath, 'utf8')
  } catch {
    return ''
  }
}

function readJsonSafe(filePath) {
  try {
    return JSON.parse(fs.readFileSync(filePath, 'utf8'))
  } catch {
    return null
  }
}

function analyzeWebHotspots() {
  const srcRoot = path.join(repoRoot, 'apps', 'web', 'src')
  const files = walkFiles(srcRoot, (full) => /\.(js|jsx|ts|tsx|css)$/.test(full))
  const items = files.map((file) => ({
    file,
    size: fs.statSync(file).size,
  }))
  items.sort((a, b) => b.size - a.size)
  const top = items.slice(0, 12)
  const large = items.filter((item) => item.size >= 30 * 1024)
  return { top, large }
}

function analyzeDocsMojibake() {
  const docsRoot = path.join(repoRoot, 'docs')
  const mdFiles = walkFiles(docsRoot, (full) => full.endsWith('.md'))
  const suspicious = []
  for (const file of mdFiles) {
    const text = readTextSafe(file)
    const hit = docsMojibakeTokens.find((token) => text.includes(token))
    if (hit) {
      suspicious.push({ file, token: hit })
    }
  }
  return suspicious
}

function analyzeRootNoise() {
  const vscodeSettings = readJsonSafe(path.join(repoRoot, '.vscode', 'settings.json')) || {}
  const filesExclude = vscodeSettings['files.exclude'] || {}
  const gitignore = readTextSafe(path.join(repoRoot, '.gitignore'))
  const gitignoreLines = gitignore
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line && !line.startsWith('#'))

  const isExcludedInVscode = (name) => {
    const keyA = `**/${name}`
    const keyB = `**/${name}/`
    return filesExclude[keyA] === true || filesExclude[keyB] === true
  }
  const isIgnoredInGit = (name) => {
    const alias = [name, `${name}/`]
    return gitignoreLines.some((line) => alias.includes(line))
  }

  return rootNoiseDirs
    .map((name) => {
      const exists = fs.existsSync(path.join(repoRoot, name))
      const managed = isExcludedInVscode(name) || isIgnoredInGit(name)
      return { name, exists, managed }
    })
    .filter((x) => x.exists)
}

function analyzeOpsDatedDocs() {
  const opsRoot = path.join(repoRoot, 'docs', 'operations')
  const opsFiles = walkFiles(opsRoot, (full) => full.endsWith('.md'))
  return opsFiles
    .filter((file) => !rel(file).startsWith('docs/operations/archive/'))
    .filter((file) => /\d{4}-\d{2}-\d{2}/.test(path.basename(file)))
}

function renderReport(data) {
  const { webHotspots, docsMojibake, rootNoise, opsDatedDocs } = data

  const lines = []
  lines.push('# Auto Optimization Audit')
  lines.push('')
  lines.push(`- Generated at: ${now.toISOString()}`)
  lines.push('')

  lines.push('## 1) Frontend Hotspots (Top 12 by size)')
  for (const item of webHotspots.top) {
    lines.push(`- ${rel(item.file)} (${item.size} bytes)`)
  }
  lines.push('')
  if (webHotspots.large.length > 0) {
    lines.push('Large files (>= 30KB):')
    for (const item of webHotspots.large) {
      lines.push(`- ${rel(item.file)} (${item.size} bytes)`) 
    }
  } else {
    lines.push('Large files (>= 30KB): none')
  }
  lines.push('')

  lines.push('## 2) Docs Encoding Risk (Mojibake token scan)')
  if (docsMojibake.length === 0) {
    lines.push('- No suspicious markdown files detected')
  } else {
    for (const item of docsMojibake) {
      lines.push(`- ${rel(item.file)} (token: ${item.token})`)
    }
  }
  lines.push('')

  lines.push('## 3) Root Noise Directories Present')
  if (rootNoise.length === 0) {
    lines.push('- None')
  } else {
    for (const item of rootNoise) {
      lines.push(`- ${item.name} (${item.managed ? 'managed' : 'unmanaged'})`)
    }
  }
  lines.push('')

  lines.push('## 4) Unarchived Dated Ops Docs')
  if (opsDatedDocs.length === 0) {
    lines.push('- None')
  } else {
    for (const file of opsDatedDocs) {
      lines.push(`- ${rel(file)}`)
    }
  }
  lines.push('')

  lines.push('## 5) Suggested Next Auto Actions')
  lines.push('- Continue splitting oversized runtime/orchestrator files by config builder and hook boundaries.')
  lines.push('- Normalize docs encoding to UTF-8 and keep historical records under docs/operations/archive/.')
  lines.push('- Keep root explorer clean: hide noise directories in IDE and avoid committing local machine artifacts.')
  lines.push('')

  return `${lines.join('\n')}\n`
}

function main() {
  const strictMode = process.argv.includes('--strict')
  const data = {
    webHotspots: analyzeWebHotspots(),
    docsMojibake: analyzeDocsMojibake(),
    rootNoise: analyzeRootNoise(),
    opsDatedDocs: analyzeOpsDatedDocs(),
  }
  const unmanagedRootNoise = data.rootNoise.filter((item) => !item.managed)

  const report = renderReport(data)
  fs.mkdirSync(reportDir, { recursive: true })
  fs.writeFileSync(reportPath, report, 'utf8')

  console.log(`OPTIMIZE_AUDIT_OK report=${rel(reportPath)}`)
  console.log(`HOTSPOTS_LARGE=${data.webHotspots.large.length}`)
  console.log(`DOCS_MOJIBAKE=${data.docsMojibake.length}`)
  console.log(`ROOT_NOISE_DIRS=${data.rootNoise.length}`)
  console.log(`ROOT_NOISE_UNMANAGED=${unmanagedRootNoise.length}`)
  console.log(`OPS_DATED_DOCS=${data.opsDatedDocs.length}`)

  if (strictMode) {
    const blockers = []
    if (data.docsMojibake.length > 0) {
      blockers.push(`docs mojibake count is ${data.docsMojibake.length}`)
    }
    if (data.opsDatedDocs.length > 0) {
      blockers.push(`unarchived dated ops docs count is ${data.opsDatedDocs.length}`)
    }
    if (blockers.length > 0) {
      console.error('OPTIMIZE_AUDIT_STRICT_FAILED')
      for (const blocker of blockers) {
        console.error(`- ${blocker}`)
      }
      process.exit(1)
    }
    console.log('OPTIMIZE_AUDIT_STRICT_OK')
  }
}

main()

