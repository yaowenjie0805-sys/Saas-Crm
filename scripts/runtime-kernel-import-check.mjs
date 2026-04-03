import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(scriptDir, '..')
const targetDir = path.join(repoRoot, 'apps', 'web', 'src', 'crm', 'hooks', 'orchestrators', 'runtime')
const importOrExportSourcePattern =
  /(?:import|export)\s+(?:[^'"]*?\s+from\s+)?['"]([^'"]+)['"]/g

function isSourceFile(fileName) {
  return /\.(mjs|js|cjs|jsx|ts|tsx)$/.test(fileName)
}

function walk(dir, files = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      walk(fullPath, files)
    } else if (entry.isFile() && isSourceFile(entry.name)) {
      files.push(fullPath)
    }
  }
  return files
}

function collectViolations() {
  if (!fs.existsSync(targetDir)) {
    return [`target directory missing: ${path.relative(repoRoot, targetDir)}`]
  }

  const violations = []
  for (const filePath of walk(targetDir)) {
    const content = fs.readFileSync(filePath, 'utf8')
    importOrExportSourcePattern.lastIndex = 0

    let match
    while ((match = importOrExportSourcePattern.exec(content)) !== null) {
      const source = match[1] ?? ''
      if (source.includes('runtime-kernel')) {
        violations.push(path.relative(repoRoot, filePath))
        break
      }
    }
  }

  return violations
}

function main() {
  const violations = collectViolations()
  if (violations.length > 0) {
    console.error('RUNTIME_KERNEL_IMPORT_CHECK_FAILED')
    for (const filePath of violations) {
      console.error(filePath)
    }
    process.exit(1)
  }

  console.log('RUNTIME_KERNEL_IMPORT_CHECK_OK')
}

main()
