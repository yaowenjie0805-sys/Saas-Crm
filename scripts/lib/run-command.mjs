import { spawnSync } from 'node:child_process'

function truncate(text = '', max = 2000) {
  const normalized = String(text || '').trim()
  if (!normalized) return ''
  if (normalized.length <= max) return normalized
  return normalized.slice(-max)
}

function resolveTimeoutMs(optionsTimeout) {
  if (Number.isFinite(optionsTimeout) && optionsTimeout > 0) {
    return Math.floor(optionsTimeout)
  }
  const raw = Number(process.env.CI_CMD_TIMEOUT_MS)
  if (Number.isFinite(raw) && raw > 0) {
    return Math.floor(raw)
  }
  return 30 * 60 * 1000
}

export function runCommand(command, args = [], options = {}) {
  const startedAt = new Date().toISOString()
  const beginAt = Date.now()
  const { timeout: optionsTimeout, ...spawnOptions } = options
  const timeoutMs = resolveTimeoutMs(optionsTimeout)
  const result = spawnSync(command, args, {
    encoding: 'utf8',
    maxBuffer: 20 * 1024 * 1024,
    ...spawnOptions,
    timeout: timeoutMs,
  })
  const timedOut = result.error?.code === 'ETIMEDOUT'

  return {
    command,
    args,
    ok: result.status === 0 && !result.error,
    status: result.status,
    signal: result.signal ?? null,
    error: result.error ? String(result.error.message || result.error) : '',
    errorCode: result.error?.code || '',
    errorErrno: result.error?.errno || null,
    errorSyscall: result.error?.syscall || '',
    errorPath: result.error?.path || '',
    errorSpawnargs: Array.isArray(result.error?.spawnargs) ? result.error.spawnargs : [],
    timeoutMs,
    timedOut,
    durationMs: Date.now() - beginAt,
    startedAt,
    finishedAt: new Date().toISOString(),
    stdoutTail: truncate(result.stdout),
    stderrTail: truncate(result.stderr),
  }
}
