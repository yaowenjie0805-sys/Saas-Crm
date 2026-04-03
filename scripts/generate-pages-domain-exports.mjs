import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(scriptDir, '..')
const pagesDir = path.join(repoRoot, 'apps', 'web', 'src', 'crm', 'components', 'pages')
const outputPath = path.join(repoRoot, 'docs', 'PAGES_DOMAIN_EXPORTS.md')

const exportFromRegex = /export\s*{([\s\S]*?)}\s*from\s*['"]([^'"]+)['"]/g
const exportDeclRegex = /export\s+(?:const|let|var|function|class)\s+([A-Za-z0-9_$]+)/g

function splitSpecifiers(specText) {
  return specText
    .split(',')
    .map((part) => part.trim())
    .filter(Boolean)
}

function parseNamedExported(spec) {
  const aliasMatch = spec.match(/^(.+?)\s+as\s+(.+)$/)
  if (aliasMatch) {
    return {
      imported: aliasMatch[1].trim(),
      exported: aliasMatch[2].trim(),
    }
  }
  return { imported: spec, exported: spec }
}

function collectDomainRows(domainName) {
  const domainPath = path.join(pagesDir, domainName)
  const indexPath = path.join(domainPath, 'index.js')
  if (!fs.existsSync(indexPath)) {
    return [{
      domain: domainName,
      exported: '(missing index.js)',
      imported: '-',
      source: '-',
    }]
  }

  const content = fs.readFileSync(indexPath, 'utf8')
  const rows = []

  let match
  while ((match = exportFromRegex.exec(content)) !== null) {
    const fromPath = match[2]
    splitSpecifiers(match[1]).forEach((specText) => {
      const spec = parseNamedExported(specText)
      rows.push({
        domain: domainName,
        exported: spec.exported,
        imported: spec.imported,
        source: fromPath,
      })
    })
  }
  exportFromRegex.lastIndex = 0

  while ((match = exportDeclRegex.exec(content)) !== null) {
    rows.push({
      domain: domainName,
      exported: match[1],
      imported: match[1],
      source: '(local declaration)',
    })
  }
  exportDeclRegex.lastIndex = 0

  if (/export\s+default\b/.test(content)) {
    rows.push({
      domain: domainName,
      exported: 'default',
      imported: 'default',
      source: '(local default)',
    })
  }

  if (rows.length === 0) {
    rows.push({
      domain: domainName,
      exported: '(no exports)',
      imported: '-',
      source: '-',
    })
  }

  return rows
}

function markdownTable(rows) {
  const lines = [
    '| Domain | Exported name | Imported name | Source |',
    '| --- | --- | --- | --- |',
  ]
  rows.forEach((row) => {
    lines.push(`| \`${row.domain}\` | \`${row.exported}\` | \`${row.imported}\` | \`${row.source}\` |`)
  })
  return lines.join('\n')
}

function main() {
  const domains = fs
    .readdirSync(pagesDir, { withFileTypes: true })
    .filter((entry) => entry.isDirectory())
    .map((entry) => entry.name)
    .sort((a, b) => a.localeCompare(b))

  const allRows = domains.flatMap((domain) => collectDomainRows(domain))
  const doc = [
    '# Pages Domain Exports',
    '',
    'Auto-generated from `apps/web/src/crm/components/pages/*/index.js`.',
    'Run `npm run generate:pages-domain-exports --workspace apps/web` to refresh.',
    '',
    markdownTable(allRows),
    '',
  ].join('\n')

  fs.writeFileSync(outputPath, doc, 'utf8')
  console.log('PAGES_DOMAIN_EXPORTS_GENERATE_OK')
  console.log(path.relative(repoRoot, outputPath))
}

main()
