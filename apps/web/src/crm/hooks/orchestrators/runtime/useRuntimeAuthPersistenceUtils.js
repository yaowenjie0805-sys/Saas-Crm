const REQUEST_CANCEL_ERRORS = new Set(['AbortError', 'CanceledError'])

export const isRequestCanceled = (error) => REQUEST_CANCEL_ERRORS.has(error?.name)

export const resolveOidcTenantId = (loginFormTenantId, cachedTenantId) => {
  const formTenant = typeof loginFormTenantId === 'string' ? loginFormTenantId.trim() : ''
  if (formTenant) return formTenant
  const cachedTenant = typeof cachedTenantId === 'string' ? cachedTenantId.trim() : ''
  if (cachedTenant) return cachedTenant
  return ''
}

export const isValidOidcState = (expected, actual) => {
  if (!expected || !actual) return false
  return expected === actual
}

export const createOidcExchangeCodeCache = ({ ttlMs = 10 * 60 * 1000, maxSize = 64 } = {}) => {
  const store = new Map()

  const prune = (now = Date.now()) => {
    for (const [code, cachedAt] of store) {
      if (now - cachedAt <= ttlMs) continue
      store.delete(code)
    }
    while (store.size > maxSize) {
      const oldestEntry = store.keys().next()
      if (oldestEntry.done) break
      store.delete(oldestEntry.value)
    }
  }

  const hasFresh = (code, now = Date.now()) => {
    prune(now)
    const cachedAt = store.get(code)
    if (cachedAt === undefined) return false
    if (now - cachedAt > ttlMs) {
      store.delete(code)
      return false
    }
    return true
  }

  const remember = (code, now = Date.now()) => {
    prune(now)
    store.set(code, now)
    prune(now)
  }

  const forget = (code) => {
    if (!code) return
    store.delete(code)
  }

  const clear = () => store.clear()

  return { hasFresh, remember, forget, clear }
}

export const sharedOidcExchangeCodeCache = createOidcExchangeCodeCache()
