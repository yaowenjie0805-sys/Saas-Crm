import { getCommonHeaders, getLang, getTenantId, requiresTenant } from './storage'

const getApiBase = () => {
  const env = import.meta.env.MODE || 'development'
  if (env === 'production') {
    return '/api'
  }
  return import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
}

export const API_BASE = getApiBase()

export const API_CACHE_CONFIG = {
  enabled: true,
  ttl: 60 * 1000,
  maxSize: 100,
  preloadEnabled: true,
  versionedCache: true,
  priorityLevels: {
    high: 5 * 60 * 1000,
    medium: 60 * 1000,
    low: 10 * 1000,
  },
}

const requestCache = new Map()
const requestCacheMeta = new Map()
const pendingRequests = new Map()

const TENANT_HEADER_KEYS = ['X-Tenant-Id', 'x-tenant-id']

const readHeaderValue = (headers, key) => {
  if (!headers) return null
  if (typeof headers.get === 'function') return headers.get(key)
  return headers[key]
}

const getCacheContext = (options = {}, lang) => {
  const headerTenant = TENANT_HEADER_KEYS.reduce((value, key) => {
    if (value) return value
    const headerValue = readHeaderValue(options.headers, key)
    return headerValue ? String(headerValue).trim() : value
  }, '')
  const tenantId = headerTenant || getTenantId()
  const normalizedLang = String(lang || '').trim() || 'en'
  return { tenantId: tenantId || '__NO_TENANT__', lang: normalizedLang }
}

const getAuthFingerprint = (options = {}, token) => {
  const headerAuth = readHeaderValue(options.headers, 'Authorization')
    || readHeaderValue(options.headers, 'authorization')
  const computedAuth = headerAuth || (token && token !== 'COOKIE_SESSION' ? `Bearer ${token}` : '')
  const normalized = String(computedAuth || '').trim()
  if (!normalized) return 'SESSION'
  return `AUTH:${hashString(normalized)}:${normalized.length}`
}

const hashString = (value) => {
  let hash = 2166136261
  for (let i = 0; i < value.length; i += 1) {
    hash ^= value.charCodeAt(i)
    hash = Math.imul(hash, 16777619)
  }
  return (hash >>> 0).toString(16).padStart(8, '0')
}

const getCacheKey = (path, options = {}, context) => {
  const method = (options.method || 'GET').toUpperCase()
  const body = options.body ? JSON.stringify(options.body) : ''
  return `${method}:${context.tenantId}:${context.lang}:${path}:${body}`
}

const cleanExpiredCache = () => {
  const now = Date.now()
  for (const [key, meta] of requestCacheMeta.entries()) {
    if (now > meta.expiresAt) {
      requestCache.delete(key)
      requestCacheMeta.delete(key)
    }
  }
}

const PRIORITY_RANK = {
  low: 0,
  medium: 1,
  high: 2,
}

const getPriorityRank = (priority) => {
  const normalized = String(priority || '').toLowerCase()
  return Object.prototype.hasOwnProperty.call(PRIORITY_RANK, normalized)
    ? PRIORITY_RANK[normalized]
    : PRIORITY_RANK.medium
}

const evictCacheEntries = () => {
  const maxSize = API_CACHE_CONFIG.maxSize
  if (maxSize <= 0) return
  const entries = Array.from(requestCacheMeta.entries()).sort(([, metaA], [, metaB]) => {
    const priorityDiff = getPriorityRank(metaA.priority) - getPriorityRank(metaB.priority)
    if (priorityDiff !== 0) return priorityDiff
    return (metaA.timestamp || 0) - (metaB.timestamp || 0)
  })
  for (const [key] of entries) {
    if (requestCache.size < maxSize) break
    requestCache.delete(key)
    requestCacheMeta.delete(key)
  }
}

const ensureCacheCapacity = (cacheKey) => {
  const maxSize = API_CACHE_CONFIG.maxSize
  if (maxSize <= 0) return false
  if (requestCache.has(cacheKey)) return true
  if (requestCache.size < maxSize) return true
  cleanExpiredCache()
  if (requestCache.size < maxSize) return true
  evictCacheEntries()
  return requestCache.size < maxSize
}

export async function api(path, options = {}, token, lang = 'en') {
  const requestContext = getCacheContext(options, lang)
  const method = (options.method || 'GET').toUpperCase()
  const authFingerprint = getAuthFingerprint(options, token)
  const dedupeEnabled = method === 'GET' && !options.signal
  const dedupeKey = dedupeEnabled
    ? `${method}:${requestContext.tenantId}:${requestContext.lang}:${authFingerprint}:${path}`
    : null

  if (dedupeKey && pendingRequests.has(dedupeKey)) {
    return pendingRequests.get(dedupeKey)
  }

  const requestPromise = (async () => {
    const body = options.body
    const isFormData = typeof FormData !== 'undefined' && body instanceof FormData
    const headers = { 'Accept-Language': requestContext.lang, ...(options.headers || {}) }
    if (!isFormData && !headers['Content-Type']) headers['Content-Type'] = 'application/json'
    if (isFormData && headers['Content-Type']) delete headers['Content-Type']
    if (token && token !== 'COOKIE_SESSION') headers.Authorization = `Bearer ${token}`
    if (!headers['X-Tenant-Id']) {
      const tenantId = getTenantId()
      if (tenantId) headers['X-Tenant-Id'] = tenantId
    }

    if (requiresTenant(path) && !String(headers['X-Tenant-Id'] || '').trim()) {
      const fallback = requestContext.lang === 'zh'
        ? '缺少租户上下文，请重新选择租户后再试'
        : 'Missing tenant context; please select a tenant and retry'
      const err = new Error(fallback)
      err.code = 'TENANT_REQUIRED'
      err.status = 400
      err.path = path
      throw err
    }

    const res = await fetch(`${API_BASE}${path}`, { ...options, credentials: 'include', headers, body })
    if (!res.ok) {
      const responseBody = await res.json().catch(() => ({}))
      const fallback = requestContext.lang === 'zh' ? '请求失败' : 'Request failed'
      const err = new Error(responseBody.message || fallback)
      err.code = responseBody.code || ''
      err.details = responseBody.details || {}
      err.requestId = responseBody.requestId || ''
      err.status = res.status
      err.validationErrors = responseBody.validationErrors || null
      throw err
    }

    if (res.status === 204) return null
    return res.json()
  })()

  if (dedupeKey) {
    pendingRequests.set(dedupeKey, requestPromise)
    requestPromise.finally(() => {
      pendingRequests.delete(dedupeKey)
    })
  }

  return requestPromise
}

let cacheCleanupInterval = null

const initCacheCleanup = () => {
  if (!cacheCleanupInterval && API_CACHE_CONFIG.enabled) {
    cacheCleanupInterval = setInterval(cleanExpiredCache, API_CACHE_CONFIG.ttl)
  }
}

export async function apiCached(path, options = {}, token, lang = 'en', useCache = true, priority = 'medium') {
  const method = (options.method || 'GET').toUpperCase()
  const requestContext = getCacheContext(options, lang)
  const cacheKey = getCacheKey(path, options, requestContext)
  const isCacheable = useCache && method === 'GET' && API_CACHE_CONFIG.enabled && API_CACHE_CONFIG.maxSize > 0
  const priorityTtl = API_CACHE_CONFIG.priorityLevels[priority] || API_CACHE_CONFIG.ttl

  if (isCacheable) {
    initCacheCleanup()
    const cached = requestCache.get(cacheKey)
    const meta = requestCacheMeta.get(cacheKey)
    if (cached && meta && Date.now() < meta.expiresAt) {
      return cached
    }
  }

  const result = await api(path, options, token, requestContext.lang)

  if (isCacheable && result && ensureCacheCapacity(cacheKey)) {
    requestCache.set(cacheKey, result)
    requestCacheMeta.set(cacheKey, {
      expiresAt: Date.now() + priorityTtl,
      priority,
      timestamp: Date.now(),
    })
  }

  return result
}

export const invalidateApiCache = (pathPrefix = null) => {
  if (pathPrefix) {
    for (const key of requestCache.keys()) {
      if (key.includes(pathPrefix)) {
        requestCache.delete(key)
        requestCacheMeta.delete(key)
      }
    }
  } else {
    requestCache.clear()
    requestCacheMeta.clear()
  }
}

export async function preloadApiData(requests = [], token, lang = 'en') {
  if (!API_CACHE_CONFIG.preloadEnabled) return

  const preloadPromises = requests.map(async (req) => {
    try {
      await apiCached(req.path, req.options || {}, token, lang, true, req.priority || 'medium')
    } catch (error) {
      console.warn('Preload API failed:', req.path, error)
    }
  })

  await Promise.all(preloadPromises)
}

export async function batchApiRequests(requests = [], token, lang = 'en') {
  const batchPromises = requests.map(async (req) => {
    try {
      const result = await apiCached(
        req.path,
        req.options || {},
        token,
        lang,
        req.useCache !== false,
        req.priority || 'medium',
      )
      return { path: req.path, success: true, data: result }
    } catch (error) {
      return { path: req.path, success: false, error: error.message }
    }
  })

  return Promise.all(batchPromises)
}

export async function apiDownload(path, filename = 'download') {
  const lang = getLang()
  const headers = getCommonHeaders(lang)

  if (requiresTenant(path) && !String(headers['X-Tenant-Id'] || '').trim()) {
    const fallback = lang === 'zh'
      ? '缺少租户上下文，请重新选择租户后再试'
      : 'Missing tenant context; please select a tenant and retry'
    const err = new Error(fallback)
    err.code = 'TENANT_REQUIRED'
    err.status = 400
    err.path = path
    throw err
  }

  const response = await fetch(`${API_BASE}${path}`, {
    method: 'GET',
    credentials: 'include',
    headers,
  })

  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    const fallback = lang === 'zh' ? '下载失败' : 'Download failed'
    throw new Error(body.message || fallback)
  }

  const blob = await response.blob()
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
  return blob
}

export async function apiUpload(path, formData, options = {}) {
  const lang = getLang()
  const headers = getCommonHeaders(lang)

  if (requiresTenant(path) && !String(headers['X-Tenant-Id'] || '').trim()) {
    const fallback = lang === 'zh'
      ? '缺少租户上下文，请重新选择租户后再试'
      : 'Missing tenant context; please select a tenant and retry'
    const err = new Error(fallback)
    err.code = 'TENANT_REQUIRED'
    err.status = 400
    err.path = path
    throw err
  }

  delete headers['Content-Type']

  const response = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    credentials: 'include',
    headers,
    body: formData,
    ...options,
  })

  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    const fallback = lang === 'zh' ? '上传失败' : 'Upload failed'
    const err = new Error(body.message || fallback)
    err.code = body.code || ''
    err.details = body.details || {}
    err.validationErrors = body.validationErrors || null
    throw err
  }

  if (response.status === 204) return null
  return response.json()
}
