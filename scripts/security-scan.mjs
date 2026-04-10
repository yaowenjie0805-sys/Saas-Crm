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

function run(command, args = [], env = {}, options = {}) {
  const timeoutMs = Number(options.timeoutMs || 0)
  const result = spawnSync(command, args, {
    cwd: root,
    encoding: 'utf8',
    env: { ...process.env, ...env },
    timeout: Number.isFinite(timeoutMs) && timeoutMs > 0 ? timeoutMs : undefined,
  })
  const timedOut = Boolean(result.error && String(result.error.code || '').toUpperCase() === 'ETIMEDOUT')
  return {
    ok: result.status === 0,
    status: result.status,
    stdout: result.stdout || '',
    stderr: result.stderr || '',
    signal: result.signal || '',
    timedOut,
    error: result.error ? String(result.error.message || result.error) : '',
  }
}

function runNpm(args = [], env = {}) {
  const isWin = process.platform === 'win32'
  if (isWin) {
    return run('cmd.exe', ['/c', 'npm', ...args], env)
  }
  return run('npm', args, env)
}

function runMaven(args = [], env = {}, options = {}) {
  const isWin = process.platform === 'win32'
  if (isWin) {
    return run('cmd.exe', ['/c', 'mvn', ...args], env, options)
  }
  return run('mvn', args, env, options)
}

function envFlag(name, fallback = false) {
  const raw = String(process.env[name] || '').trim().toLowerCase()
  if (!raw) return fallback
  return raw === '1' || raw === 'true' || raw === 'yes' || raw === 'on'
}

function envPositiveInt(name, fallback) {
  const raw = String(process.env[name] || '').trim()
  if (!raw) return fallback
  const value = Number(raw)
  if (!Number.isFinite(value) || value <= 0) return fallback
  return Math.floor(value)
}

function outputPreview(text, maxLength = 1200) {
  const value = String(text || '').trim()
  if (!value) return ''
  if (value.length <= maxLength) return value
  return `${value.slice(0, maxLength)}...`
}

function isMavenUnavailable(scanResult) {
  const detail = `${scanResult.stderr}\n${scanResult.stdout}\n${scanResult.error}`.toLowerCase()
  return (
    detail.includes('mvn is not recognized') ||
    detail.includes('mvn: command not found') ||
    detail.includes('not found') && detail.includes('mvn') ||
    detail.includes('enoent')
  )
}

function runJavaDependencyScan({ enabled, required, timeoutMs }) {
  const failBuildOnCvss = envPositiveInt('SECURITY_SCAN_JAVA_FAIL_ON_CVSS', 8)
  const args = [
    '-f',
    'apps/api/pom.xml',
    'org.owasp:dependency-check-maven:check',
    '-Dformat=JSON',
    `-DfailBuildOnCVSS=${failBuildOnCvss}`,
  ]
  const command = `mvn ${args.join(' ')}`

  if (!enabled) {
    return {
      enabled: false,
      requiredForPass: required,
      countedInOverallPass: required,
      ran: false,
      status: required ? 'disabled_required' : 'disabled',
      pass: !required,
      command,
      failBuildOnCvss,
      timeoutMs,
      exitCode: null,
      timedOut: false,
      reportFile: '',
      stdoutPreview: '',
      stderrPreview: '',
      error: '',
    }
  }

  const scanResult = runMaven(args, {}, { timeoutMs })
  const reportPath = path.join(root, 'apps', 'api', 'target', 'dependency-check-report.json')
  let status = 'passed'
  if (!scanResult.ok) {
    if (scanResult.timedOut) {
      status = 'timed_out'
    } else {
      status = isMavenUnavailable(scanResult) ? 'tool_unavailable' : 'failed'
    }
  }

  const scan = {
    enabled: true,
    requiredForPass: required,
    countedInOverallPass: required,
    ran: true,
    status,
    pass: scanResult.ok,
    command,
    timeoutMs,
    exitCode: typeof scanResult.status === 'number' ? scanResult.status : null,
    timedOut: scanResult.timedOut,
    reportFile: fs.existsSync(reportPath) ? path.relative(root, reportPath) : '',
    stdoutPreview: outputPreview(scanResult.stdout),
    stderrPreview: outputPreview(scanResult.stderr),
    error: scanResult.error || '',
  }

  if (scan.status !== 'passed') {
    if (scan.status === 'timed_out') {
      scan.actionableHint = 'Java dependency scan timed out. Increase SECURITY_SCAN_JAVA_TIMEOUT_MS if a longer scan is expected.'
    } else if (scan.status === 'tool_unavailable') {
      scan.actionableHint = 'Install Maven and verify `mvn -v` works. If Java scan is optional, set SECURITY_SCAN_JAVA_REQUIRED=0.'
    } else {
      scan.actionableHint = 'Inspect Maven output above and rerun dependency-check locally. If this gate is too strict, set SECURITY_SCAN_JAVA_REQUIRED=0.'
    }
  }

  return scan
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
  const fallback = null
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

function summarizeStatus(pass, fallback = false) {
  if (!pass) return 'failed'
  return fallback ? 'passed_fallback' : 'passed'
}

function main() {
  ensureDir(outDir)
  const beginAt = Date.now()
  const javaEnabled = envFlag('SECURITY_SCAN_JAVA_ENABLED', false)
  const javaRequired = envFlag('SECURITY_SCAN_JAVA_REQUIRED', false)
  const javaTimeoutMs = envPositiveInt('SECURITY_SCAN_JAVA_TIMEOUT_MS', 300000)

  const auditRes = runNpm(['audit', '--omit=dev', '--json'])
  const audit = parseAudit(auditRes)
  const auditParseOk = !!audit
  const high = Number(audit?.metadata?.vulnerabilities?.high ?? -1)
  const critical = Number(audit?.metadata?.vulnerabilities?.critical ?? -1)
  const auditPass = auditRes.ok && auditParseOk && high === 0 && critical === 0

  const secretFindings = scanSecrets()
  const secretsPass = secretFindings.length === 0

  const sbom = runSbom()
  const sbomPass = sbom.ok
  const javaDependencyScan = runJavaDependencyScan({
    enabled: javaEnabled,
    required: javaRequired,
    timeoutMs: javaTimeoutMs,
  })

  const report = {
    generatedAt: new Date().toISOString(),
    durationMs: Date.now() - beginAt,
    checks: {
      npmAudit: {
        pass: auditPass,
        commandOk: auditRes.ok,
        parseOk: auditParseOk,
        high,
        critical,
        stderrPreview: outputPreview(auditRes.stderr),
        stdoutPreview: outputPreview(auditRes.stdout),
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
      javaDependencyScan,
    },
    pass: auditPass && secretsPass && sbomPass && (!javaRequired || javaDependencyScan.pass),
  }
  writeReport(report)
  const summary = [
    `npmAudit(ok=${auditRes.ok},parse=${auditParseOk},high=${high},critical=${critical})`,
    `secrets(findings=${secretFindings.length})`,
    `sbom(${summarizeStatus(sbomPass, sbom.fallback)})`,
    `javaDependencyScan(${javaDependencyScan.status})`,
  ].join(' ')
  console.log(
    `SECURITY_SCAN_JAVA_RESULT status=${javaDependencyScan.status} enabled=${javaDependencyScan.enabled} required=${javaDependencyScan.requiredForPass} timeoutMs=${javaDependencyScan.timeoutMs} timedOut=${javaDependencyScan.timedOut} exitCode=${javaDependencyScan.exitCode === null ? 'null' : javaDependencyScan.exitCode}`
  )

  if (!report.pass) {
    console.error(`SECURITY_SCAN_SUMMARY ${summary}`)
    console.error(`SECURITY_SCAN_FAIL ${path.relative(root, reportFile)}`)
    process.exit(1)
  }
  console.log(`SECURITY_SCAN_SUMMARY ${summary}`)
  console.log(`SECURITY_SCAN_OK ${path.relative(root, reportFile)}`)
}

main()
