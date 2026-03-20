import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const perfDir = path.join(root, 'logs', 'perf')
const perfBaselineDoc = path.join(root, 'docs', 'operations', 'perf-baseline.md')
const sreDoc = path.join(root, 'docs', 'operations', 'sre-slo-baseline.md')
const outFile = path.join(perfDir, 'perf-gate-latest.json')
const perfSnapshotFile = path.join(perfDir, 'perf-acceptance-latest.json')

function readLatestBaselineReport() {
  if (!fs.existsSync(perfDir)) return null
  const files = fs.readdirSync(perfDir)
    .filter((name) => name.startsWith('perf-baseline-') && name.endsWith('.json') && !name.includes('-raw'))
    .sort()
  if (files.length === 0) return null
  const latest = path.join(perfDir, files[files.length - 1])
  const report = JSON.parse(fs.readFileSync(latest, 'utf8'))
  return { latest, report }
}

function readThresholdsFromPerfDoc() {
  if (!fs.existsSync(perfBaselineDoc)) {
    throw new Error('perf_baseline_doc_missing')
  }
  const text = fs.readFileSync(perfBaselineDoc, 'utf8')
  const begin = '<!-- perf-thresholds-json-begin -->'
  const end = '<!-- perf-thresholds-json-end -->'
  const beginAt = text.indexOf(begin)
  const endAt = text.indexOf(end)
  if (beginAt < 0 || endAt < 0 || endAt <= beginAt) {
    throw new Error('perf_thresholds_json_block_missing')
  }
  const region = text.slice(beginAt, endAt)
  const match = region.match(/```json([\s\S]*?)```/m)
  if (!match) {
    throw new Error('perf_thresholds_json_code_block_missing')
  }
  const parsed = JSON.parse(match[1].trim())
  return parsed
}

function readSreErrorRateMax() {
  if (!fs.existsSync(sreDoc)) return null
  const text = fs.readFileSync(sreDoc, 'utf8')
  const match = text.match(/api_error[^0-9]*([0-9]+(?:\.[0-9]+)?)%/i)
  if (!match) return null
  return Number(match[1]) / 100
}

function pass(name, detail) {
  return { name, ok: true, detail }
}

function fail(name, detail) {
  return { name, ok: false, detail }
}

function evaluate(report, thresholds) {
  const checks = []
  const global = thresholds.global || {}
  const routes = thresholds.routes || {}
  const summary = report.summary || {}

  const minRps = Number(global.minRps || 0)
  const maxP95Ms = Number(global.maxP95Ms || 1500)
  const maxP99Ms = Number(global.maxP99Ms || 2500)
  const maxErrorRate = Number(global.maxErrorRate || 0.02)
  const maxTimeouts = Number(global.maxTimeouts || 10)

  checks.push(summary.rps >= minRps
    ? pass('global rps', `rps=${summary.rps}`)
    : fail('global rps', `rps=${summary.rps} < ${minRps}`))
  checks.push(summary.p95Ms <= maxP95Ms
    ? pass('global p95', `p95=${summary.p95Ms}`)
    : fail('global p95', `p95=${summary.p95Ms} > ${maxP95Ms}`))
  checks.push(summary.p99Ms <= maxP99Ms
    ? pass('global p99', `p99=${summary.p99Ms}`)
    : fail('global p99', `p99=${summary.p99Ms} > ${maxP99Ms}`))
  checks.push(summary.errorRate <= maxErrorRate
    ? pass('global errorRate', `errorRate=${summary.errorRate}`)
    : fail('global errorRate', `errorRate=${summary.errorRate} > ${maxErrorRate}`))
  checks.push(summary.timeouts <= maxTimeouts
    ? pass('global timeouts', `timeouts=${summary.timeouts}`)
    : fail('global timeouts', `timeouts=${summary.timeouts} > ${maxTimeouts}`))

  const routeChecks = ['dashboard', 'customers', 'reports']
  for (const route of routeChecks) {
    const routeStats = report.routes?.[route] || {}
    const routeThreshold = routes[route] || {}
    const routeMaxP95 = Number(routeThreshold.maxP95Ms || maxP95Ms)
    const routeMaxP99 = Number(routeThreshold.maxP99Ms || maxP99Ms)
    checks.push(Number(routeStats.p95Ms || 0) <= routeMaxP95
      ? pass(`${route} p95`, `p95=${routeStats.p95Ms}`)
      : fail(`${route} p95`, `p95=${routeStats.p95Ms} > ${routeMaxP95}`))
    checks.push(Number(routeStats.p99Ms || 0) <= routeMaxP99
      ? pass(`${route} p99`, `p99=${routeStats.p99Ms}`)
      : fail(`${route} p99`, `p99=${routeStats.p99Ms} > ${routeMaxP99}`))
  }
  return checks
}

function main() {
  if (!fs.existsSync(perfDir)) fs.mkdirSync(perfDir, { recursive: true })
  const latest = readLatestBaselineReport()
  if (!latest) {
    const report = {
      generatedAt: new Date().toISOString(),
      pass: false,
      reason: 'precondition_failed',
      message: 'Missing perf baseline report. Run "npm run perf:baseline" before "npm run perf:gate".',
      checks: [fail('baseline report', 'missing perf-baseline-*.json')],
    }
    fs.writeFileSync(outFile, JSON.stringify(report, null, 2))
    console.error('PERF_GATE_PRECONDITION_FAIL missing baseline report')
    console.error('PERF_GATE_HINT run "npm run perf:baseline" then rerun "npm run perf:gate"')
    process.exit(1)
  }
  if (latest.report?.failed) {
    const report = {
      generatedAt: new Date().toISOString(),
      pass: false,
      reason: 'baseline_failed',
      baselineReport: path.relative(root, latest.latest),
      message: latest.report?.error || 'perf baseline failed',
      checks: [fail('baseline report', latest.report?.error || 'perf baseline failed')],
    }
    fs.writeFileSync(outFile, JSON.stringify(report, null, 2))
    console.error(`PERF_GATE_BASELINE_FAIL ${report.message}`)
    process.exit(1)
  }

  const thresholds = readThresholdsFromPerfDoc()
  const sreMax = readSreErrorRateMax()
  if (sreMax != null) {
    thresholds.global = thresholds.global || {}
    thresholds.global.maxErrorRate = sreMax
  }

  const checks = evaluate(latest.report, thresholds)
  const failed = checks.filter((c) => !c.ok)
  const bundleGate = readJsonIfExists(path.join(perfDir, 'bundle-gate-latest.json'))
  const sqlExplainCompare = readJsonIfExists(path.join(perfDir, 'sql-explain-compare-latest.json'))
  const gateReport = {
    generatedAt: new Date().toISOString(),
    baselineReport: path.relative(root, latest.latest),
    thresholds,
    checks,
    relatedReports: {
      bundleGate: bundleGate ? 'logs/perf/bundle-gate-latest.json' : '',
      sqlExplainCompare: sqlExplainCompare ? 'logs/perf/sql-explain-compare-latest.json' : '',
    },
    pass: failed.length === 0,
    failedCount: failed.length,
  }
  fs.writeFileSync(outFile, JSON.stringify(gateReport, null, 2))
  const snapshot = {
    generatedAt: new Date().toISOString(),
    perfGate: gateReport,
    bundleGate,
    sqlExplainCompare,
  }
  fs.writeFileSync(perfSnapshotFile, JSON.stringify(snapshot, null, 2))

  if (failed.length > 0) {
    console.error(`PERF_GATE_FAIL count=${failed.length}`)
    process.exit(1)
  }
  console.log(`PERF_GATE_OK ${path.relative(root, outFile)}`)
}

function readJsonIfExists(file) {
  if (!fs.existsSync(file)) return null
  try {
    return JSON.parse(fs.readFileSync(file, 'utf8'))
  } catch {
    return null
  }
}

main()
