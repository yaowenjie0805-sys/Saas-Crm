import { spawnSync } from 'node:child_process'

function truncate(text = '', max = 2000) {
  const normalized = String(text || '').trim()
  if (!normalized) return ''
  if (normalized.length <= max) return normalized
  return normalized.slice(-max)
}

export function runCommand(command, args = [], options = {}) {
  const startedAt = new Date().toISOString()
  const beginAt = Date.now()
  const result = spawnSync(command, args, {
    encoding: 'utf8',
    maxBuffer: 20 * 1024 * 1024,
    ...options,
  })

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
    durationMs: Date.now() - beginAt,
    startedAt,
    finishedAt: new Date().toISOString(),
    stdoutTail: truncate(result.stdout),
    stderrTail: truncate(result.stderr),
  }
}
