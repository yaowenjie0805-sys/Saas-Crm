import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const files = [
  'backend/src/main/java/com/yao/crm/controller/DashboardController.java',
  'backend/src/main/java/com/yao/crm/controller/ReportController.java',
  'backend/src/main/java/com/yao/crm/controller/V1ReportController.java',
  'backend/src/main/java/com/yao/crm/controller/V1SalesInsightController.java',
  'backend/src/main/java/com/yao/crm/controller/V2CommerceController.java',
]

function checkOne(rel) {
  const full = path.join(root, rel)
  if (!fs.existsSync(full)) {
    return { file: rel, ok: false, reason: 'missing_file' }
  }
  const text = fs.readFileSync(full, 'utf8')
  const requiredHeaders = ['X-CRM-Cache', 'X-CRM-Cache-Tier', 'X-CRM-Cache-Fallback']
  const missing = requiredHeaders.filter((name) => !text.includes(name))
  return {
    file: rel,
    ok: missing.length === 0,
    reason: missing.length ? `missing_headers:${missing.join(',')}` : 'ok',
  }
}

const results = files.map(checkOne)
const failed = results.filter((row) => !row.ok)

if (failed.length) {
  console.error(`CACHE_HEADERS_FAIL ${failed.map((f) => `${f.file}:${f.reason}`).join(' | ')}`)
  process.exit(1)
}

console.log(`CACHE_HEADERS_OK ${results.length}`)
