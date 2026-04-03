import { FILTERS_KEY, STORAGE_KEYS, TENANT_OPTIONAL_PATH_PREFIXES } from './constants'

export const readFilters = () => {
  try {
    return JSON.parse(localStorage.getItem(FILTERS_KEY) || '{}')
  } catch {
    return {}
  }
}

export const parseHashPage = () => {
  const raw = (window.location.hash || '').replace(/^#\/?/, '')
  return raw || 'dashboard'
}

const getAuth = () => {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.AUTH) || 'null')
  } catch {
    return null
  }
}

const getToken = () => localStorage.getItem(STORAGE_KEYS.TOKEN) || ''

export const getTenantId = () => {
  const tenantFromAuth = String(getAuth()?.tenantId || '').trim()
  if (tenantFromAuth) return tenantFromAuth
  const lastTenant = String(localStorage.getItem(STORAGE_KEYS.LAST_TENANT) || '').trim()
  return lastTenant || ''
}

export const requiresTenant = (path = '') => {
  const normalized = String(path || '').trim()
  if (!normalized) return true
  return !TENANT_OPTIONAL_PATH_PREFIXES.some((prefix) => normalized.startsWith(prefix))
}

export const getLang = () => localStorage.getItem(STORAGE_KEYS.LANG) || 'en'

export const getCommonHeaders = (lang = 'en') => {
  const headers = { 'Accept-Language': lang }
  const tenantId = getTenantId()
  if (tenantId) headers['X-Tenant-Id'] = tenantId
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  return headers
}
