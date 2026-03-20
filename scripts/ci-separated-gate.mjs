import fs from 'node:fs'
import path from 'node:path'
import { runCommand } from './lib/run-command.mjs'

const root = process.cwd()
const outDir = path.join(root, 'logs', 'qa')
const stamp = new Date().toISOString().replace(/[:.]/g, '-')
const outFile = path.join(outDir, `separated-gate-${stamp}.json`)
const latestFile = path.join(outDir, 'separated-gate-latest.json')

function ensureDir(dir) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })
}

const npmCmd = process.platform === 'win32' ? 'cmd.exe' : 'npm'
const npmArgs = (script) => (
  process.platform === 'win32'
    ? ['/c', 'npm', 'run', script]
    : ['run', script]
)

function createEmptyReport() {
  return {
    generatedAt: new Date().toISOString(),
    pass: false,
    phase: 'initializing',
    context: {
      platform: process.platform,
      nodeVersion: process.version,
      cwd: root,
      npmCmd,
    },
    durationMs: 0,
    steps: [],
  }
}

function writeReport(report) {
  ensureDir(outDir)
  fs.writeFileSync(outFile, JSON.stringify(report, null, 2))
  fs.writeFileSync(latestFile, JSON.stringify(report, null, 2))
}

async function main() {
  ensureDir(outDir)
  const beginAt = Date.now()
  const report = createEmptyReport()
  writeReport(report)

  const plan = [
    { name: 'frontend', script: 'ci:frontend' },
    { name: 'backend', script: 'ci:backend' },
  ]

  for (const step of plan) {
    report.phase = `running:${step.name}`
    writeReport(report)

    const result = runCommand(npmCmd, npmArgs(step.script), {
      cwd: root,
      env: process.env,
    })

    report.steps.push({
      name: step.name,
      script: step.script,
      ...result,
    })
    report.durationMs = Date.now() - beginAt
    writeReport(report)

    if (!result.ok) {
      report.phase = `failed:${step.name}`
      report.pass = false
      writeReport(report)
      if (result.error) {
        console.error(`SEPARATED_GATE_STEP_ERROR ${step.name} ${result.error}`)
      }
      if (result.signal) {
        console.error(`SEPARATED_GATE_STEP_SIGNAL ${step.name} ${result.signal}`)
      }
      console.error(`SEPARATED_GATE_FAIL ${path.relative(root, outFile)}`)
      process.exit(1)
    }
  }

  report.phase = 'completed'
  report.pass = true
  report.durationMs = Date.now() - beginAt
  writeReport(report)
  console.log(`SEPARATED_GATE_OK ${path.relative(root, outFile)}`)
}

main().catch((err) => {
  const report = createEmptyReport()
  report.phase = 'error'
  report.error = String(err?.message || err)
  writeReport(report)
  console.error(`SEPARATED_GATE_ERROR ${err?.message || err}`)
  process.exit(1)
})
