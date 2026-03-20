import fs from 'node:fs'
import path from 'node:path'
import { execSync } from 'node:child_process'

const root = process.cwd()
const outDir = path.join(root, 'logs', 'perf')
const stamp = new Date().toISOString().replace(/[:.]/g, '-')
const outFile = path.join(outDir, `sql-explain-${stamp}.json`)
const latestFile = path.join(outDir, 'sql-explain-latest.json')
const baselineFile = path.join(outDir, 'sql-explain-baseline.json')
const compareLatestFile = path.join(outDir, 'sql-explain-compare-latest.json')
const updateBaseline = process.env.PERF_SQL_UPDATE_BASELINE === '1'

const DB_USER = process.env.DB_USER || 'root'
const DB_PASSWORD = process.env.DB_PASSWORD || 'root'
const DB_NAME = process.env.DB_NAME || 'crm_local_e2e'
const DB_HOST = process.env.DB_HOST || '127.0.0.1'
const DB_PORT = process.env.DB_PORT || '3306'
const TENANT_ID = process.env.TENANT_ID || 'tenant_default'

const queries = [
  {
    name: 'customers_list_by_status_updated',
    sql: `SELECT id, name, status, updated_at FROM customers WHERE tenant_id='${TENANT_ID}' AND status='Active' ORDER BY updated_at DESC LIMIT 50;`,
  },
  {
    name: 'opportunities_list_by_stage_updated',
    sql: `SELECT id, owner, stage, updated_at FROM opportunities WHERE tenant_id='${TENANT_ID}' AND stage='Lead' ORDER BY updated_at DESC LIMIT 50;`,
  },
  {
    name: 'tasks_list_by_done_updated',
    sql: `SELECT id, title, done, updated_at FROM tasks WHERE tenant_id='${TENANT_ID}' AND done=0 ORDER BY updated_at DESC LIMIT 50;`,
  },
  {
    name: 'tasks_list_by_owner_done_updated',
    sql: `SELECT id, title, owner, done, updated_at FROM tasks WHERE tenant_id='${TENANT_ID}' AND owner='sales_zhang' AND done=0 ORDER BY updated_at DESC LIMIT 50;`,
  },
  {
    name: 'lead_import_jobs_by_status_created',
    sql: `SELECT id, status, created_at FROM lead_import_jobs WHERE tenant_id='${TENANT_ID}' AND status='RUNNING' ORDER BY created_at DESC LIMIT 50;`,
  },
  {
    name: 'quotes_list_by_status_opportunity_updated',
    sql: `SELECT id, quote_no, status, opportunity_id, updated_at FROM quotes WHERE tenant_id='${TENANT_ID}' AND status='SUBMITTED' AND opportunity_id IS NOT NULL ORDER BY updated_at DESC LIMIT 50;`,
  },
  {
    name: 'orders_list_by_status_opportunity_updated',
    sql: `SELECT id, order_no, status, opportunity_id, updated_at FROM order_records WHERE tenant_id='${TENANT_ID}' AND status='CONFIRMED' AND opportunity_id IS NOT NULL ORDER BY updated_at DESC LIMIT 50;`,
  },
]

function ensureDir(dir) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })
}

function runMysql(sql) {
  const cmd = `mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASSWORD} -D ${DB_NAME} -N -B -e "${sql.replace(/"/g, '\\"')}"`
  return execSync(cmd, { encoding: 'utf8' }).trim()
}

function walkMetrics(node, summary) {
  if (!node || typeof node !== 'object') return
  Object.entries(node).forEach(([key, value]) => {
    if (key === 'rows_examined_per_scan' || key === 'rows_produced_per_join') {
      const n = Number(value || 0)
      if (!Number.isNaN(n)) summary.maxRowsExamined = Math.max(summary.maxRowsExamined, n)
    }
    if (key === 'query_cost') {
      const n = Number(value || 0)
      if (!Number.isNaN(n)) summary.queryCost = n
    }
    walkMetrics(value, summary)
  })
}

function explainOne(item) {
  const output = runMysql(`EXPLAIN FORMAT=JSON ${item.sql}`)
  const normalized = output
    .replace(/^"|"$/g, '')
    .replace(/\\\\n/g, '\n')
    .replace(/\\n/g, '\n')
    .replace(/\\"/g, '"')
  const parsed = JSON.parse(normalized)
  const summary = { queryCost: 0, maxRowsExamined: 0 }
  walkMetrics(parsed, summary)
  return {
    name: item.name,
    sql: item.sql,
    summary,
    explain: parsed,
  }
}

function compareAgainstBaseline(current, baseline) {
  if (!baseline) return null
  return {
    generatedAt: new Date().toISOString(),
    baselineGeneratedAt: baseline.generatedAt || '',
    queries: current.queries.map((query) => {
      const base = (baseline.queries || []).find((item) => item.name === query.name)
      const baseRows = Number(base?.summary?.maxRowsExamined || 0)
      const baseCost = Number(base?.summary?.queryCost || 0)
      return {
        name: query.name,
        currentMaxRowsExamined: query.summary.maxRowsExamined,
        baselineMaxRowsExamined: baseRows,
        deltaMaxRowsExamined: query.summary.maxRowsExamined - baseRows,
        currentQueryCost: query.summary.queryCost,
        baselineQueryCost: baseCost,
        deltaQueryCost: query.summary.queryCost - baseCost,
      }
    }),
  }
}

function main() {
  ensureDir(outDir)
  const report = {
    generatedAt: new Date().toISOString(),
    database: { host: DB_HOST, port: DB_PORT, db: DB_NAME },
    queries: queries.map(explainOne),
  }
  const baseline = fs.existsSync(baselineFile)
    ? JSON.parse(fs.readFileSync(baselineFile, 'utf8'))
    : null
  const compare = compareAgainstBaseline(report, baseline)

  fs.writeFileSync(outFile, JSON.stringify(report, null, 2))
  fs.writeFileSync(latestFile, JSON.stringify(report, null, 2))
  if (compare) fs.writeFileSync(compareLatestFile, JSON.stringify(compare, null, 2))
  if (updateBaseline || !baseline) fs.writeFileSync(baselineFile, JSON.stringify(report, null, 2))

  console.log(`SQL_EXPLAIN_OK ${path.relative(root, outFile)}`)
  if (compare) console.log(`SQL_EXPLAIN_COMPARE_OK ${path.relative(root, compareLatestFile)}`)
  if (updateBaseline || !baseline) console.log(`SQL_EXPLAIN_BASELINE_OK ${path.relative(root, baselineFile)}`)
}

main()
