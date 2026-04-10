import { spawnSync } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const checkScriptPath = path.join(scriptDir, 'runtime-barrel-export-check.mjs')

function runCheckJson() {
  const result = spawnSync(process.execPath, [checkScriptPath, '--json'], {
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe'],
  })
  const stdout = (result.stdout || '').trim()
  const stderr = (result.stderr || '').trim()
  return { stdout, stderr }
}

function parseReport(stdout, stderr) {
  const primary = stdout || stderr
  if (!primary) return null
  try {
    return JSON.parse(primary)
  } catch {
    return null
  }
}

function buildSummary(report, rawText) {
  if (!report) {
    return [
      '### Runtime Barrel Export Check',
      '',
      'Result: unknown (invalid or empty JSON output).',
      '',
      '```text',
      rawText || '(empty output)',
      '```',
    ].join('\n')
  }

  const lines = [
    `### Runtime Barrel Export Check (${report.ok ? 'PASS' : 'FAIL'})`,
    '',
    `- missingExports: ${(report.missingExports || []).length}`,
    `- missingSources: ${(report.missingReExportSources || []).length}`,
    `- defaultExportViolations: ${(report.defaultExportViolations || []).length}`,
  ]
  if (report.exception) {
    lines.push(
      `- exception: ${report.exception.name || 'Error'}${report.exception.code ? ` (${report.exception.code})` : ''}`,
    )
  }

  const suggestions = report.suggestions || []
  if (suggestions.length > 0) {
    lines.push('', 'Top suggestions:')
    for (const suggestion of suggestions.slice(0, 8)) {
      lines.push(`- ${suggestion}`)
    }
  }

  return lines.join('\n')
}

function main() {
  const { stdout, stderr } = runCheckJson()
  const report = parseReport(stdout, stderr)
  const rawText = [stdout, stderr].filter(Boolean).join('\n')
  console.log(buildSummary(report, rawText))
}

main()
