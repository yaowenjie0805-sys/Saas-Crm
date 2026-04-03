import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(scriptDir, '..')
const pagesDir = path.join(repoRoot, 'apps', 'web', 'src', 'crm', 'components', 'pages')
const pagesIndexPath = path.join(pagesDir, 'index.js')

function readText(filePath) {
  return fs.readFileSync(filePath, 'utf8')
}

function splitSpecifiers(specText) {
  return specText
    .split(',')
    .map((part) => part.trim())
    .filter(Boolean)
}

function parseRequestedImports(specText) {
  return splitSpecifiers(specText).map((spec) => {
    const matched = spec.match(/^(.+?)\s+as\s+(.+)$/)
    if (matched) return { imported: matched[1].trim(), exported: matched[2].trim() }
    return { imported: spec, exported: spec }
  })
}

function normalizeSpecifierForExport(spec) {
  const matched = spec.match(/^(.+?)\s+as\s+(.+)$/)
  if (!matched) return spec.trim()
  return matched[2].trim()
}

function collectModuleExports(modulePath) {
  const source = readText(modulePath)
  const exportsSet = new Set()

  const braceExportRegex = /export\s*{([\s\S]*?)}\s*(?:from\s*['"][^'"]+['"])?/g
  let braceMatch
  while ((braceMatch = braceExportRegex.exec(source)) !== null) {
    splitSpecifiers(braceMatch[1]).forEach((spec) => {
      exportsSet.add(normalizeSpecifierForExport(spec))
    })
  }

  const declExportRegex = /export\s+(?:const|let|var|function|class)\s+([A-Za-z0-9_$]+)/g
  let declMatch
  while ((declMatch = declExportRegex.exec(source)) !== null) {
    exportsSet.add(declMatch[1])
  }

  if (/export\s+default\b/.test(source)) {
    exportsSet.add('default')
  }

  return exportsSet
}

function resolveTargetModule(fromPath) {
  const targetPath = path.resolve(pagesDir, fromPath)
  if (/\.(mjs|js|cjs|jsx|ts|tsx)$/.test(targetPath)) return targetPath
  const asFile = `${targetPath}.js`
  if (fs.existsSync(asFile)) return asFile
  return path.join(targetPath, 'index.js')
}

function collectViolations() {
  const content = readText(pagesIndexPath)
  const exportFromRegex = /export\s*{([\s\S]*?)}\s*from\s*['"]([^'"]+)['"]/g
  const violations = []

  let match
  while ((match = exportFromRegex.exec(content)) !== null) {
    const specText = match[1]
    const fromPath = match[2]
    const requested = parseRequestedImports(specText)
    const modulePath = resolveTargetModule(fromPath)
    const displayTarget = path.relative(repoRoot, modulePath)

    if (!fs.existsSync(modulePath)) {
      violations.push(`missing target module: ${displayTarget}`)
      continue
    }

    const exportedNames = collectModuleExports(modulePath)
    for (const req of requested) {
      if (!exportedNames.has(req.imported)) {
        violations.push(
          `missing export "${req.imported}" in ${displayTarget} (required by apps/web/src/crm/components/pages/index.js)`,
        )
      }
    }
  }

  return violations
}

function main() {
  if (!fs.existsSync(pagesIndexPath)) {
    console.error('PAGES_ENTRY_EXPORT_CHECK_FAILED')
    console.error('missing entry: apps/web/src/crm/components/pages/index.js')
    process.exit(1)
  }

  const violations = collectViolations()
  if (violations.length > 0) {
    console.error('PAGES_ENTRY_EXPORT_CHECK_FAILED')
    violations.forEach((line) => console.error(line))
    process.exit(1)
  }

  console.log('PAGES_ENTRY_EXPORT_CHECK_OK')
}

main()
