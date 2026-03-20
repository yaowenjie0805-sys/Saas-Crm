import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const reportFile = path.join(root, 'logs', 'perf', 'bundle-report-latest.json')
const budgetFile = path.join(root, 'docs', 'operations', 'perf-bundle-budget.json')
const outFile = path.join(root, 'logs', 'perf', 'bundle-gate-latest.json')

function fail(reason) {
  const report = {
    generatedAt: new Date().toISOString(),
    pass: false,
    reason,
  }
  fs.writeFileSync(outFile, JSON.stringify(report, null, 2))
  console.error(`BUNDLE_GATE_FAIL ${reason}`)
  process.exit(1)
}

function toKB(bytes) {
  return Math.round((Number(bytes || 0) / 1024) * 100) / 100
}

function main() {
  if (!fs.existsSync(reportFile)) fail('missing_bundle_report')
  if (!fs.existsSync(budgetFile)) fail('missing_bundle_budget')
  const report = JSON.parse(fs.readFileSync(reportFile, 'utf8'))
  const budget = JSON.parse(fs.readFileSync(budgetFile, 'utf8'))
  if (!report?.entry?.gzipBytes) fail('missing_entry_metric')

  const entryGzip = Number(report.entry.gzipBytes)
  const totalJsGzip = Number(report.totals?.jsGzipBytes || 0)
  const baselineEntry = Number(budget.baselineEntryGzipBytes || 0)
  const entryMax = Number(budget.entryGzipMaxBytes || 0)
  const totalJsMax = Number(budget.totalJsGzipMaxBytes || 0)
  const reductionMinPct = Number(budget.entryReductionMinPct || 0)
  const criticalRouteChunkMaxGzipBytes = Number(budget.criticalRouteChunkMaxGzipBytes || 0)
  const criticalRouteChunkPatterns = Array.isArray(budget.criticalRouteChunkPatterns)
    ? budget.criticalRouteChunkPatterns.filter((pattern) => typeof pattern === 'string' && pattern.trim())
    : []
  const requiredChunkPatterns = Array.isArray(budget.requiredChunkPatterns)
    ? budget.requiredChunkPatterns.filter((pattern) => typeof pattern === 'string' && pattern.trim())
    : criticalRouteChunkPatterns
  const chunkGzipMaxBytesByPattern = (budget.chunkGzipMaxBytesByPattern && typeof budget.chunkGzipMaxBytesByPattern === 'object')
    ? Object.entries(budget.chunkGzipMaxBytesByPattern)
      .filter(([pattern, maxBytes]) => typeof pattern === 'string' && pattern.trim() && Number(maxBytes) > 0)
      .map(([pattern, maxBytes]) => ({ pattern, maxBytes: Number(maxBytes) }))
    : []

  const reductionPct = baselineEntry > 0
    ? ((baselineEntry - entryGzip) / baselineEntry) * 100
    : 0

  const checks = [
    {
      name: 'entry_gzip_max',
      ok: entryMax > 0 ? entryGzip <= entryMax : true,
      detail: `${toKB(entryGzip)}KB <= ${toKB(entryMax)}KB`,
    },
    {
      name: 'total_js_gzip_max',
      ok: totalJsMax > 0 ? totalJsGzip <= totalJsMax : true,
      detail: `${toKB(totalJsGzip)}KB <= ${toKB(totalJsMax)}KB`,
    },
    {
      name: 'entry_reduction_pct',
      ok: baselineEntry > 0 ? reductionPct >= reductionMinPct : true,
      detail: `${Math.round(reductionPct * 100) / 100}% >= ${reductionMinPct}%`,
    },
  ]

  if (criticalRouteChunkMaxGzipBytes > 0 && criticalRouteChunkPatterns.length) {
    const criticalChunks = (report.assets || []).filter((asset) => (
      asset?.ext === 'js' && criticalRouteChunkPatterns.some((pattern) => asset.file?.includes(pattern))
    ))
    const tooLarge = criticalChunks.filter((chunk) => Number(chunk.gzipBytes || 0) > criticalRouteChunkMaxGzipBytes)
    checks.push({
      name: 'critical_route_chunk_gzip_max',
      ok: tooLarge.length === 0,
      detail: tooLarge.length
        ? tooLarge.map((chunk) => `${chunk.file}:${toKB(chunk.gzipBytes)}KB`).join(', ')
        : `${criticalChunks.length} chunks <= ${toKB(criticalRouteChunkMaxGzipBytes)}KB`,
    })
  }

  if (requiredChunkPatterns.length) {
    const missingPatterns = requiredChunkPatterns.filter((pattern) => !(report.assets || []).some((asset) => (
      asset?.ext === 'js' && String(asset.file || '').includes(pattern)
    )))
    checks.push({
      name: 'critical_route_chunk_presence',
      ok: missingPatterns.length === 0,
      detail: missingPatterns.length ? `missing: ${missingPatterns.join(', ')}` : `present: ${requiredChunkPatterns.join(', ')}`,
    })
  }

  if (chunkGzipMaxBytesByPattern.length) {
    for (const rule of chunkGzipMaxBytesByPattern) {
      const matched = (report.assets || []).filter((asset) => (
        asset?.ext === 'js' && String(asset.file || '').includes(rule.pattern)
      ))
      const oversized = matched.filter((asset) => Number(asset.gzipBytes || 0) > rule.maxBytes)
      checks.push({
        name: `chunk_gzip_max:${rule.pattern}`,
        ok: oversized.length === 0,
        detail: oversized.length
          ? oversized.map((asset) => `${asset.file}:${toKB(asset.gzipBytes)}KB`).join(', ')
          : `${matched.length} chunks <= ${toKB(rule.maxBytes)}KB`,
      })
    }
  }

  const pass = checks.every((item) => item.ok)
  const gate = {
    generatedAt: new Date().toISOString(),
    pass,
    metrics: {
      entryGzipBytes: entryGzip,
      totalJsGzipBytes: totalJsGzip,
      baselineEntryGzipBytes: baselineEntry,
      entryReductionPct: Math.round(reductionPct * 100) / 100,
    },
    budget,
    checks,
  }
  fs.writeFileSync(outFile, JSON.stringify(gate, null, 2))
  if (!pass) {
    console.error(`BUNDLE_GATE_FAIL ${path.relative(root, outFile)}`)
    process.exit(1)
  }
  console.log(`BUNDLE_GATE_OK ${path.relative(root, outFile)}`)
}

main()
