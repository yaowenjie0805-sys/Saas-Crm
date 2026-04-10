import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { parse as parseModule } from 'acorn'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(scriptDir, '..')
const runtimeIndexPath = path.join(
  repoRoot,
  'apps',
  'web',
  'src',
  'crm',
  'hooks',
  'orchestrators',
  'runtime',
  'index.js',
)
const runtimeDir = path.dirname(runtimeIndexPath)
const jsonMode = process.argv.includes('--json')
const strictHumanMode = process.argv.includes('--strict-human')
const RUNTIME_SOURCE_EXTENSIONS = ['.js', '.jsx', '.ts', '.tsx']

const requiredExports = [
  'useRuntimeAuthPersistenceEffects',
  'useRuntimeOidcExchangeEffect',
  'resolveOidcTenantId',
  'isValidOidcState',
]

function collectExpectedEffectExports() {
  const names = []
  const entries = fs.readdirSync(runtimeDir, { withFileTypes: true })
  for (const entry of entries) {
    if (!entry.isFile()) continue
    if (!/^useRuntime[A-Za-z0-9]*Effect\.js$/.test(entry.name)) continue
    names.push(entry.name.replace(/\.js$/, ''))
  }
  return names.sort()
}

function collectExpectedUtilsModules() {
  const modules = []
  const entries = fs.readdirSync(runtimeDir, { withFileTypes: true })
  for (const entry of entries) {
    if (!entry.isFile()) continue
    if (!/^useRuntime[A-Za-z0-9]*Utils\.js$/.test(entry.name)) continue
    modules.push(`./${entry.name.replace(/\.js$/, '')}`)
  }
  return modules.sort()
}

function collectDefaultExportViolations() {
  const violations = []
  const entries = fs.readdirSync(runtimeDir, { withFileTypes: true })
  for (const entry of entries) {
    if (!entry.isFile()) continue
    if (!/^useRuntime[A-Za-z0-9]*(Effect|Utils)\.js$/.test(entry.name)) continue
    const filePath = path.join(runtimeDir, entry.name)
    const source = fs.readFileSync(filePath, 'utf8')
    if (/export\s+default\b/.test(source)) {
      violations.push(`apps/web/src/crm/hooks/orchestrators/runtime/${entry.name}`)
    }
  }
  return violations.sort()
}

function splitSpecifiers(specText) {
  return specText
    .split(',')
    .map((part) => part.trim())
    .filter(Boolean)
}

function normalizeSpecifierForExport(spec) {
  const matched = spec.match(/^(.+?)\s+as\s+(.+)$/)
  if (!matched) return spec.trim()
  return matched[2].trim()
}

function collectNamedExportsByRegex(source) {
  const exportsSet = new Set()
  const exportRegex = /export\s*{([\s\S]*?)}/g
  let match
  while ((match = exportRegex.exec(source)) !== null) {
    splitSpecifiers(match[1]).forEach((spec) => {
      exportsSet.add(normalizeSpecifierForExport(spec))
    })
  }
  const declExportRegex = /export\s+(?:const|let|var|function|class)\s+([A-Za-z0-9_$]+)/g
  let declMatch
  while ((declMatch = declExportRegex.exec(source)) !== null) {
    exportsSet.add(declMatch[1])
  }
  return exportsSet
}

function collectExportSourcesByRegex(source) {
  const fromSources = new Set()
  const exportFromRegex = /export\s*{[\s\S]*?}\s*from\s*['"]([^'"]+)['"]/g
  let match
  while ((match = exportFromRegex.exec(source)) !== null) {
    fromSources.add(match[1])
  }
  return fromSources
}

function parseModuleProgram(filePath) {
  const source = fs.readFileSync(filePath, 'utf8')
  const parsed = parseModule(source, {
    ecmaVersion: 'latest',
    sourceType: 'module',
    allowHashBang: true,
  })
  return { source, parsed }
}

function collectPatternBindingNames(pattern, result = []) {
  if (!pattern) return result
  if (pattern.type === 'Identifier') {
    result.push(pattern.name)
    return result
  }
  if (pattern.type === 'RestElement') {
    collectPatternBindingNames(pattern.argument, result)
    return result
  }
  if (pattern.type === 'AssignmentPattern') {
    collectPatternBindingNames(pattern.left, result)
    return result
  }
  if (pattern.type === 'ArrayPattern') {
    for (const element of pattern.elements || []) {
      collectPatternBindingNames(element, result)
    }
    return result
  }
  if (pattern.type === 'ObjectPattern') {
    for (const property of pattern.properties || []) {
      if (property?.type === 'Property') {
        collectPatternBindingNames(property.value, result)
        continue
      }
      if (property?.type === 'RestElement') {
        collectPatternBindingNames(property.argument, result)
      }
    }
  }
  return result
}

function collectDeclarationExportNames(declaration) {
  if (!declaration) return []
  if (declaration.type === 'FunctionDeclaration' || declaration.type === 'ClassDeclaration') {
    return declaration.id?.name ? [declaration.id.name] : []
  }
  if (declaration.type === 'VariableDeclaration') {
    const names = []
    for (const item of declaration.declarations || []) {
      collectPatternBindingNames(item.id, names)
    }
    return names
  }
  return []
}

function collectNamedExports(filePath) {
  let parsed
  try {
    const moduleResult = parseModuleProgram(filePath)
    parsed = moduleResult.parsed
  } catch {
    const source = fs.readFileSync(filePath, 'utf8')
    return collectNamedExportsByRegex(source)
  }
  const exportsSet = new Set()
  for (const node of parsed.body || []) {
    if (node.type === 'ExportNamedDeclaration') {
      if (node.declaration) {
        for (const exportName of collectDeclarationExportNames(node.declaration)) {
          exportsSet.add(exportName)
        }
      }
      for (const specifier of node.specifiers || []) {
        if (specifier?.exported?.name) exportsSet.add(specifier.exported.name)
      }
      continue
    }
    if (node.type === 'ExportAllDeclaration' && node.exported?.name) {
      exportsSet.add(node.exported.name)
    }
  }
  return exportsSet
}

function collectExportSources(filePath) {
  let parsed
  try {
    const moduleResult = parseModuleProgram(filePath)
    parsed = moduleResult.parsed
  } catch {
    const source = fs.readFileSync(filePath, 'utf8')
    return collectExportSourcesByRegex(source)
  }
  const fromSources = new Set()
  for (const node of parsed.body || []) {
    if (node.type === 'ExportNamedDeclaration' && node.source?.value) {
      fromSources.add(String(node.source.value))
      continue
    }
    if (node.type === 'ExportAllDeclaration' && node.source?.value) {
      fromSources.add(String(node.source.value))
    }
  }
  return fromSources
}

function collectRuntimeModulePaths() {
  const paths = []
  const entries = fs.readdirSync(runtimeDir, { withFileTypes: true })
  for (const entry of entries) {
    if (!entry.isFile()) continue
    if (!/^useRuntime[A-Za-z0-9]*\.(js|jsx|ts|tsx)$/.test(entry.name)) continue
    paths.push(path.join(runtimeDir, entry.name))
  }
  return paths.sort()
}

function findExportSourcesForName(name) {
  const sources = []
  for (const modulePath of collectRuntimeModulePaths()) {
    const exportsSet = collectNamedExports(modulePath)
    if (!exportsSet.has(name)) continue
    const moduleName = path.basename(modulePath).replace(/\.(js|jsx|ts|tsx)$/, '')
    sources.push(`./${moduleName}`)
  }
  return sources
}

function collectNamedExportsFromSourceModule(sourcePath, baseDir = runtimeDir) {
  const modulePath = resolveRuntimeModulePath(sourcePath, baseDir)
  if (!fs.existsSync(modulePath)) return []
  return Array.from(collectNamedExports(modulePath)).sort()
}

function resolveRuntimeModulePath(sourcePath, baseDir = runtimeDir) {
  const moduleBaseName = sourcePath.replace('./', '')
  for (const ext of RUNTIME_SOURCE_EXTENSIONS) {
    const candidate = path.join(baseDir, `${moduleBaseName}${ext}`)
    if (fs.existsSync(candidate)) return candidate
  }
  return path.join(baseDir, `${moduleBaseName}.js`)
}

function createBaseReport(code, overrides = {}) {
  return {
    ok: false,
    code,
    missingEntry: null,
    missingExports: [],
    missingReExportSources: [],
    defaultExportViolations: [],
    suggestions: [],
    exception: null,
    ...overrides,
  }
}

function toExceptionReport(error) {
  return createBaseReport('RUNTIME_BARREL_EXPORT_CHECK_EXCEPTION', {
    exception: {
      name: error?.name || 'Error',
      message: error?.message || 'unknown error',
      code: error?.code || '',
      syscall: error?.syscall || '',
      path: error?.path || '',
    },
  })
}

function main() {
  try {
    if (!fs.existsSync(runtimeIndexPath)) {
      const report = createBaseReport('RUNTIME_BARREL_EXPORT_CHECK_FAILED', {
        missingEntry: 'apps/web/src/crm/hooks/orchestrators/runtime/index.js',
      })
      emitReport(report)
      process.exit(1)
    }

    const exportsSet = collectNamedExports(runtimeIndexPath)
    const exportSources = collectExportSources(runtimeIndexPath)
    const dynamicRequired = collectExpectedEffectExports()
    const expected = Array.from(new Set([...requiredExports, ...dynamicRequired]))
    const missing = expected.filter((name) => !exportsSet.has(name))
    const expectedUtilsModules = collectExpectedUtilsModules()
    const missingUtilsModules = expectedUtilsModules.filter((source) => !exportSources.has(source))
    const defaultExportViolations = collectDefaultExportViolations()
    const suggestions = []

    for (const name of missing) {
      const sources = findExportSourcesForName(name)
      if (sources.length === 1) {
        suggestions.push(`export { ${name} } from '${sources[0]}'`)
      } else if (sources.length > 1) {
        suggestions.push(`export "${name}" from one of: ${sources.join(', ')}`)
      }
    }
    for (const source of missingUtilsModules) {
      const namedExports = collectNamedExportsFromSourceModule(source)
      if (namedExports.length > 0) {
        suggestions.push(`export { ${namedExports.join(', ')} } from '${source}'`)
      }
    }
    for (const filePath of defaultExportViolations) {
      const exportName = path.basename(filePath).replace(/\.js$/, '')
      suggestions.push(`replace "export default" with "export function ${exportName}(...)" in ${filePath}`)
    }

    if (missing.length > 0 || missingUtilsModules.length > 0 || defaultExportViolations.length > 0) {
      emitReport(
        createBaseReport('RUNTIME_BARREL_EXPORT_CHECK_FAILED', {
          missingExports: missing,
          missingReExportSources: missingUtilsModules,
          defaultExportViolations,
          suggestions,
        }),
      )
      process.exit(1)
    }

    emitReport({
      ok: true,
      code: 'RUNTIME_BARREL_EXPORT_CHECK_OK',
      missingEntry: null,
      missingExports: [],
      missingReExportSources: [],
      defaultExportViolations: [],
      suggestions: [],
      exception: null,
    })
  } catch (error) {
    emitReport(toExceptionReport(error))
    process.exit(1)
  }
}

function emitReport(report) {
  if (jsonMode) {
    console.log(JSON.stringify(report, null, 2))
    return
  }
  if (strictHumanMode) {
    const missingExportsCount = (report.missingExports || []).length
    const missingSourcesCount = (report.missingReExportSources || []).length
    const defaultViolationsCount = (report.defaultExportViolations || []).length
    const suggestionsCount = (report.suggestions || []).length
    const hasException = report.exception ? 1 : 0
    if (report.ok) {
      console.log(
        `${report.code} missingExports=0 missingSources=0 defaultExportViolations=0 suggestions=0`,
      )
      return
    }
    console.error(
      `${report.code} missingExports=${missingExportsCount} missingSources=${missingSourcesCount} defaultExportViolations=${defaultViolationsCount} suggestions=${suggestionsCount} exception=${hasException}`,
    )
    return
  }
  if (report.ok) {
    console.log(report.code)
    return
  }
  console.error(report.code)
  if (report.missingEntry) {
    console.error(`missing entry: ${report.missingEntry}`)
  }
  for (const name of report.missingExports || []) {
    console.error(`missing export "${name}" in apps/web/src/crm/hooks/orchestrators/runtime/index.js`)
  }
  for (const source of report.missingReExportSources || []) {
    console.error(`missing re-export source "${source}" in apps/web/src/crm/hooks/orchestrators/runtime/index.js`)
  }
  for (const filePath of report.defaultExportViolations || []) {
    console.error(`default export is not allowed in ${filePath}; use named export only`)
  }
  for (const line of report.suggestions || []) {
    console.error(`suggestion: ${line}`)
  }
  if (report.exception) {
    const { name = 'Error', message = 'unknown error', code = '', syscall = '', path: errorPath = '' } = report.exception
    console.error(
      `exception: ${name}: ${message}${code ? ` code=${code}` : ''}${syscall ? ` syscall=${syscall}` : ''}${errorPath ? ` path=${errorPath}` : ''}`,
    )
  }
}

export const __internal = {
  collectNamedExportsFromSourceModule,
  collectNamedExports,
  collectExportSources,
  resolveRuntimeModulePath,
}

main()
