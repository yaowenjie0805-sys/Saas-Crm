import fs from 'node:fs'
import path from 'node:path'
import zlib from 'node:zlib'

const root = process.cwd()
const distDir = path.join(root, 'apps', 'web', 'dist')
const assetsDir = path.join(distDir, 'assets')
const outDir = path.join(root, 'logs', 'perf')
const stamp = new Date().toISOString().replace(/[:.]/g, '-')
const outFile = path.join(outDir, `bundle-report-${stamp}.json`)
const latestFile = path.join(outDir, 'bundle-report-latest.json')
const baselineFile = path.join(outDir, 'bundle-report-baseline.json')
const compareLatestFile = path.join(outDir, 'bundle-report-compare-latest.json')
const updateBaseline = process.env.PERF_BUNDLE_UPDATE_BASELINE === '1'

function ensureDir(dir) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })
}

function gzipBytes(buf) {
  return zlib.gzipSync(buf).length
}

function listAssets() {
  if (!fs.existsSync(assetsDir)) return []
  return fs.readdirSync(assetsDir)
    .filter((name) => name.endsWith('.js') || name.endsWith('.css'))
    .sort()
    .map((name) => {
      const full = path.join(assetsDir, name)
      const content = fs.readFileSync(full)
      return {
        file: `apps/web/dist/assets/${name}`,
        ext: path.extname(name).slice(1),
        bytes: content.length,
        gzipBytes: gzipBytes(content),
      }
    })
}

function detectEntryJs(indexHtml) {
  const match = indexHtml.match(/<script[^>]*type="module"[^>]*src="([^"]+)"/i)
  if (!match) return ''
  return match[1].replace(/^\//, 'apps/web/dist/')
}

function main() {
  ensureDir(outDir)
  if (!fs.existsSync(path.join(distDir, 'index.html'))) {
    throw new Error('apps/web/dist/index.html not found, run build first')
  }
  const indexHtml = fs.readFileSync(path.join(distDir, 'index.html'), 'utf8')
  const entryJs = detectEntryJs(indexHtml)
  const assets = listAssets()
  const entry = assets.find((item) => item.file === entryJs) || null
  const jsAssets = assets.filter((item) => item.ext === 'js')
  const cssAssets = assets.filter((item) => item.ext === 'css')
  const report = {
    generatedAt: new Date().toISOString(),
    entryJs,
    entry: entry ? { bytes: entry.bytes, gzipBytes: entry.gzipBytes } : null,
    totals: {
      jsBytes: jsAssets.reduce((sum, item) => sum + item.bytes, 0),
      jsGzipBytes: jsAssets.reduce((sum, item) => sum + item.gzipBytes, 0),
      cssBytes: cssAssets.reduce((sum, item) => sum + item.bytes, 0),
      cssGzipBytes: cssAssets.reduce((sum, item) => sum + item.gzipBytes, 0),
    },
    assets,
  }
  const baseline = fs.existsSync(baselineFile)
    ? JSON.parse(fs.readFileSync(baselineFile, 'utf8'))
    : null
  const compare = baseline
    ? {
      generatedAt: new Date().toISOString(),
      baselineGeneratedAt: baseline.generatedAt || '',
      delta: {
        entryGzipBytes: Number(report.entry?.gzipBytes || 0) - Number(baseline.entry?.gzipBytes || 0),
        jsGzipBytes: Number(report.totals?.jsGzipBytes || 0) - Number(baseline.totals?.jsGzipBytes || 0),
        cssGzipBytes: Number(report.totals?.cssGzipBytes || 0) - Number(baseline.totals?.cssGzipBytes || 0),
      },
      assets: report.assets.map((asset) => {
        const base = (baseline.assets || []).find((item) => item.file === asset.file)
        return {
          file: asset.file,
          gzipBytes: asset.gzipBytes,
          baselineGzipBytes: Number(base?.gzipBytes || 0),
          deltaGzipBytes: Number(asset.gzipBytes || 0) - Number(base?.gzipBytes || 0),
        }
      }),
    }
    : null
  fs.writeFileSync(outFile, JSON.stringify(report, null, 2))
  fs.writeFileSync(latestFile, JSON.stringify(report, null, 2))
  if (compare) fs.writeFileSync(compareLatestFile, JSON.stringify(compare, null, 2))
  if (updateBaseline || !baseline) fs.writeFileSync(baselineFile, JSON.stringify(report, null, 2))
  console.log(`BUNDLE_REPORT_OK ${path.relative(root, outFile)}`)
  if (compare) console.log(`BUNDLE_COMPARE_OK ${path.relative(root, compareLatestFile)}`)
  if (updateBaseline || !baseline) console.log(`BUNDLE_BASELINE_OK ${path.relative(root, baselineFile)}`)
}

main()
