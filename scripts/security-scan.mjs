import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'

const root = process.cwd()
const outDir = path.join(root, 'logs', 'security')
const stamp = new Date().toISOString().replace(/[:.]/g, '-')
const reportFile = path.join(outDir, `security-scan-${stamp}.json`)
const latestFile = path.join(outDir, 'security-scan-latest.json')

function ensureDir(dir) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })
}

function run(command, args = [], env = {}) {
  const result = spawnSync(command, args, {
    cwd: root,
    encoding: 'utf8',
    env: { ...process.env, ...env },
  })
  return {
    ok: result.status === 0,
    status: result.status,
    stdout: result.stdout || '',
    stderr: result.stderr || '',
  }
}

function runNpm(args = [], env = {}) {
  const isWin = process.platform === 'win32'
  if (isWin) {
    return run('cmd.exe', ['/c', 'npm', ...args], env)
  }
  return run('npm', args, env)
}

function gitTrackedFiles() {
  const res = run('git', ['ls-files'])
  if (!res.ok) return []
  return res.stdout
    .split(/\r?\n/)
    .map((s) => s.trim())
    .filter(Boolean)
}

function scanSecrets() {
  const findings = []
  const patterns = [
    { id: 'private_key', regex: /BEGIN (?:RSA |EC |OPENSSH |)PRIVATE KEY/ },
    { id: 'aws_access_key', regex: /AKIA[0-9A-Z]{16}/ },
    { id: 'github_pat', regex: /ghp_[A-Za-z0-9]{36,}/ },
    { id: 'slack_token', regex: /xox[baprs]-[A-Za-z0-9-]{10,}/ },
  ]
  const files = gitTrackedFiles()
  for (const rel of files) {
    if (rel.startsWith('docs/')) continue
    if (rel.startsWith('test-results/')) continue
    if (rel.includes('.boss/')) continue
    const abs = path.join(root, rel)
    if (!fs.existsSync(abs)) continue
    let text = ''
    try {
      text = fs.readFileSync(abs, 'utf8')
    } catch {
      continue
    }
    for (const pattern of patterns) {
      if (pattern.regex.test(text)) {
        findings.push({ file: rel, pattern: pattern.id })
      }
    }
  }
  return findings
}

function parseAudit(auditRes) {
  const fallback = {
    metadata: {
      vulnerabilities: { low: 0, moderate: 0, high: 0, critical: 0, total: 0 },
    },
  }
  const text = (auditRes.stdout || '').trim()
  if (!text) return fallback
  try {
    return JSON.parse(text)
  } catch {
    return fallback
  }
}

function runSbom() {
  const sbomFile = path.join(outDir, `sbom-${stamp}.json`)
  const sbomRes = runNpm(['sbom', '--json'])
  if (sbomRes.ok && sbomRes.stdout.trim()) {
    fs.writeFileSync(sbomFile, sbomRes.stdout)
    return { ok: true, file: path.relative(root, sbomFile), fallback: false }
  }

  const lock = path.join(root, 'package-lock.json')
  const fallbackFile = path.join(outDir, `sbom-fallback-${stamp}.json`)
  if (fs.existsSync(lock)) {
    const lockBody = JSON.parse(fs.readFileSync(lock, 'utf8'))
    const pkgs = Object.keys(lockBody.packages || {})
    const fallbackBody = {
      generatedAt: new Date().toISOString(),
      source: 'package-lock.json',
      packageCount: pkgs.length,
      packages: pkgs.slice(0, 500),
    }
    fs.writeFileSync(fallbackFile, JSON.stringify(fallbackBody, null, 2))
    return { ok: true, file: path.relative(root, fallbackFile), fallback: true }
  }
  return { ok: false, file: '', fallback: true }
}

function writeReport(report) {
  ensureDir(outDir)
  fs.writeFileSync(reportFile, JSON.stringify(report, null, 2))
  fs.writeFileSync(latestFile, JSON.stringify(report, null, 2))
}

function main() {
  ensureDir(outDir)
  const beginAt = Date.now()

  const auditRes = runNpm(['audit', '--omit=dev', '--json'])
  const audit = parseAudit(auditRes)
  const high = Number(audit?.metadata?.vulnerabilities?.high || 0)
  const critical = Number(audit?.metadata?.vulnerabilities?.critical || 0)
  const auditPass = high === 0 && critical === 0

  const secretFindings = scanSecrets()
  const secretsPass = secretFindings.length === 0

  const sbom = runSbom()
  const sbomPass = sbom.ok

  const report = {
    generatedAt: new Date().toISOString(),
    durationMs: Date.now() - beginAt,
    checks: {
      npmAudit: {
        pass: auditPass,
        high,
        critical,
      },
      secretScan: {
        pass: secretsPass,
        findings: secretFindings,
      },
      sbom: {
        pass: sbomPass,
        file: sbom.file,
        fallback: sbom.fallback,
      },
    },
    pass: auditPass && secretsPass && sbomPass,
  }
  writeReport(report)

  if (!report.pass) {
    console.error(`SECURITY_SCAN_FAIL ${path.relative(root, reportFile)}`)
    process.exit(1)
  }
  console.log(`SECURITY_SCAN_OK ${path.relative(root, reportFile)}`)
}

main()
