const DEFAULT_DEV_API_PORT = '8080'
const DEFAULT_SHARED_API_BASE = '/api'

const trimTrailingSlash = (value) => String(value || '').replace(/\/+$/, '')

const readMode = () => {
  const mode = import.meta?.env?.MODE
  return typeof mode === 'string' && mode.trim() ? mode.trim() : 'development'
}

export const getRuntimeMode = () => readMode()

const resolveDevApiBase = () => {
  const host = typeof window !== 'undefined' && window.location && window.location.hostname
    ? window.location.hostname
    : '127.0.0.1'
  return `http://${host}:${DEFAULT_DEV_API_PORT}/api`
}

export const resolveApiBase = () => {
  const explicit = import.meta?.env?.VITE_API_BASE_URL
  if (typeof explicit === 'string' && explicit.trim()) {
    return trimTrailingSlash(explicit)
  }
  const mode = readMode()
  if (mode === 'development') {
    return resolveDevApiBase()
  }
  return DEFAULT_SHARED_API_BASE
}
