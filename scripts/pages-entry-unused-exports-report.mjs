import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(scriptDir, '..')
const crmSrcDir = path.join(repoRoot, 'apps', 'web', 'src', 'crm')
const pagesIndexPath = path.join(crmSrcDir, 'components', 'pages', 'index.js')
const reportPath = path.join(repoRoot, 'logs', 'structure', 'pages-unused-exports-report.md')

const exportFromRegex = /export\s*{([\s\S]*?)}\s*from\s*['"][^'"]+['"]/g
const importFromPagesRegex =
  /^\s*import\s+(.+?)\s+from\s*['"]([^'"]*components\/pages(?:\/index(?:\.js)?)?)['"]/gm

function readText(filePath) {
  return fs.readFileSync(filePath, 'utf8')
}

function splitSpecifiers(specText) {
  return specText
    .split(',')
    .map((part) => part.trim())
    .filter(Boolean)
}

function parseExportedNamesFromPagesIndex() {
  const content = readText(pagesIndexPath)
  const exported = new Set()
  let match
  while ((match = exportFromRegex.exec(content)) !== null) {
    splitSpecifiers(match[1]).forEach((spec) => {
      const aliasMatch = spec.match(/^(.+?)\s+as\s+(.+)$/)
      if (aliasMatch) {
        exported.add(aliasMatch[2].trim())
      } else {
        exported.add(spec)
      }
    })
  }
  return [...exported].sort((a, b) => a.localeCompare(b))
}

function walk(dir, files = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      walk(fullPath, files)
      continue
    }
    if (!/\.(mjs|js|cjs|jsx|ts|tsx)$/.test(entry.name)) continue
    files.push(fullPath)
  }
  return files
}

function parseImportedNames(importClause) {
  const result = new Set()
  const text = importClause.trim()
  if (!text) return result

  const namedMatch = text.match(/\{([\s\S]*?)\}/)
  if (namedMatch) {
    splitSpecifiers(namedMatch[1]).forEach((spec) => {
      const aliasMatch = spec.match(/^(.+?)\s+as\s+(.+)$/)
      if (aliasMatch) {
        result.add(aliasMatch[1].trim())
      } else {
        result.add(spec)
      }
    })
  }

  const withoutNamed = text.replace(/\{[\s\S]*?\}/g, '').trim()
  if (withoutNamed && !withoutNamed.startsWith('*')) {
    const defaultPart = withoutNamed.replace(/,$/, '').trim()
    if (defaultPart) result.add('default')
  }

  return result
}

function collectUsedExports() {
  const sourceFiles = walk(crmSrcDir)
  const used = new Set()
  const pagesIndexRelative = path.relative(repoRoot, pagesIndexPath)

  for (const filePath of sourceFiles) {
    const fileRelative = path.relative(repoRoot, filePath)
    if (fileRelative === pagesIndexRelative) continue

    const content = readText(filePath)
    let match
    while ((match = importFromPagesRegex.exec(content)) !== null) {
      const importClause = match[1]
      const namespaceMatch = importClause.match(/^\*\s+as\s+([A-Za-z_$][\w$]*)$/)
      if (namespaceMatch) {
        const alias = namespaceMatch[1]
        const memberRegex = new RegExp(`${alias}\\.([A-Za-z_$][\\w$]*)`, 'g')
        let memberMatch
        while ((memberMatch = memberRegex.exec(content)) !== null) {
          used.add(memberMatch[1])
        }
      }
      parseImportedNames(importClause).forEach((name) => used.add(name))
    }
    importFromPagesRegex.lastIndex = 0
  }

  return used
}

function ensureDir(dirPath) {
  if (!fs.existsSync(dirPath)) fs.mkdirSync(dirPath, { recursive: true })
}

function writeReport(unused, usedCount, totalCount) {
  ensureDir(path.dirname(reportPath))
  const lines = [
    '# Pages Entry Unused Exports Report',
    '',
    `- Source: \`apps/web/src/crm/components/pages/index.js\``,
    `- Total exports: ${totalCount}`,
    `- Used exports (in \`apps/web/src/crm\` imports from pages entry): ${usedCount}`,
    `- Unused exports: ${unused.length}`,
    '',
  ]

  if (unused.length === 0) {
    lines.push('All exports are referenced.')
  } else {
    lines.push('## Unused exports')
    lines.push('')
    unused.forEach((name) => lines.push(`- \`${name}\``))
  }

  fs.writeFileSync(reportPath, `${lines.join('\n')}\n`, 'utf8')
}

function main() {
  if (!fs.existsSync(pagesIndexPath)) {
    console.error('PAGES_ENTRY_UNUSED_EXPORTS_REPORT_FAILED')
    console.error('missing entry: apps/web/src/crm/components/pages/index.js')
    process.exit(1)
  }

  const exported = parseExportedNamesFromPagesIndex()
  const used = collectUsedExports()
  const unused = exported.filter((name) => !used.has(name))

  writeReport(unused, used.size, exported.length)
  console.log('PAGES_ENTRY_UNUSED_EXPORTS_REPORT_OK')
  console.log(path.relative(repoRoot, reportPath))
}

main()
