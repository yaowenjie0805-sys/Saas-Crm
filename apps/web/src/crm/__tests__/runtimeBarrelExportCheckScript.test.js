import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { describe, expect, it } from 'vitest'
import { __internal } from '../../../../../scripts/runtime-barrel-export-check.mjs'

const repoRoot = path.resolve(process.cwd(), '..', '..')
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

describe('runtime-barrel-export-check script', () => {
  it('emits structured exception report when runtime index cannot be read', () => {
    const backupPath = `${runtimeIndexPath}.__bak__`
    if (fs.existsSync(backupPath)) fs.rmSync(backupPath, { force: true })

    fs.renameSync(runtimeIndexPath, backupPath)
    fs.mkdirSync(runtimeIndexPath)

    try {
      const result = spawnSync(process.execPath, ['scripts/runtime-barrel-export-check.mjs', '--json'], {
        cwd: repoRoot,
        encoding: 'utf8',
      })
      expect(result.status).toBe(1)
      const report = JSON.parse((result.stdout || '').trim())
      expect(report.code).toBe('RUNTIME_BARREL_EXPORT_CHECK_EXCEPTION')
      expect(report.ok).toBe(false)
      expect(report.exception.code).toBe('EISDIR')
    } finally {
      fs.rmSync(runtimeIndexPath, { recursive: true, force: true })
      fs.renameSync(backupPath, runtimeIndexPath)
    }
  })

  it('resolves source module path using ts extension when js file is absent', () => {
    const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'runtime-barrel-check-'))
    const moduleName = 'useRuntimeTempUtils'
    const sourcePath = `./${moduleName}`
    const modulePath = path.join(tmpDir, `${moduleName}.ts`)
    try {
      fs.writeFileSync(modulePath, 'export const tempExport = 1\n', 'utf8')
      const resolved = __internal.resolveRuntimeModulePath(sourcePath, tmpDir)
      expect(resolved).toBe(modulePath)
      const exportsList = __internal.collectNamedExportsFromSourceModule(sourcePath, tmpDir)
      expect(exportsList).toContain('tempExport')
    } finally {
      fs.rmSync(tmpDir, { recursive: true, force: true })
    }
  })

  it('collects export sources from export-all declarations', () => {
    const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'runtime-barrel-check-'))
    const filePath = path.join(tmpDir, 'index.js')
    try {
      fs.writeFileSync(filePath, "export * from './alpha'\nexport { beta } from './beta'\n", 'utf8')
      const sources = Array.from(__internal.collectExportSources(filePath)).sort()
      expect(sources).toEqual(['./alpha', './beta'])
    } finally {
      fs.rmSync(tmpDir, { recursive: true, force: true })
    }
  })

  it('falls back to regex parsing when AST parsing fails', () => {
    const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'runtime-barrel-check-'))
    const filePath = path.join(tmpDir, 'index.ts')
    try {
      fs.writeFileSync(filePath, "export const typed: string = 'ok'\n", 'utf8')
      const named = Array.from(__internal.collectNamedExports(filePath))
      expect(named).toContain('typed')
    } finally {
      fs.rmSync(tmpDir, { recursive: true, force: true })
    }
  })
})
