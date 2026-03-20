export const MAX_ASSOC_ITEMS = 8
export const ONE_DAY_MS = 24 * 60 * 60 * 1000
export const THIRTY_DAYS_MS = 30 * ONE_DAY_MS

export const CUSTOMER360_MODULE_KEYS = [
  'contacts',
  'opportunities',
  'quotes',
  'orders',
  'contracts',
  'payments',
  'approvals',
  'audits',
  'notifications',
]

export const CUSTOMER360_PRIMARY_MODULES = [
  'contacts',
  'opportunities',
  'quotes',
  'orders',
  'contracts',
  'payments',
]

export const CUSTOMER360_SECONDARY_MODULES = ['approvals', 'audits', 'notifications']
export const CUSTOMER360_PREFETCH_MODULES = ['contacts', 'opportunities', 'quotes', 'orders']
export const CUSTOMER360_MODULE_TTL_MS = 45 * 1000
export const CUSTOMER360_ADAPTIVE_WINDOW = 8
export const CUSTOMER360_PREFETCH_LOW_HIT = 25
export const CUSTOMER360_PREFETCH_HIGH_HIT = 70
export const CUSTOMER360_PREFETCH_HIGH_LATENCY = 450

export const CUSTOMER360_ACTION_MODULES = {
  followup: ['timeline', 'audits'],
  task: ['timeline', 'audits'],
  quote: ['quotes', 'approvals'],
  urgeApproval: ['approvals', 'notifications'],
  manualRefresh: CUSTOMER360_MODULE_KEYS,
}

export function createCustomer360ModuleMeta() {
  return CUSTOMER360_MODULE_KEYS.reduce((acc, key) => {
    acc[key] = {
      loading: false,
      error: '',
      lastLoadedAt: 0,
      signature: '',
    }
    return acc
  }, {})
}

export function buildCustomer360ModuleSignature(customerIdText, moduleName, serverFilterSignature = '') {
  return `${customerIdText}:${moduleName}:v1:${serverFilterSignature || 'default'}`
}

export function copyIdToClipboard(value) {
  if (value === null || value === undefined) return
  const text = String(value)
  if (!text) return
  if (navigator?.clipboard?.writeText) {
    navigator.clipboard.writeText(text)
    return
  }
  const input = document.createElement('textarea')
  input.value = text
  input.setAttribute('readonly', 'true')
  input.style.position = 'absolute'
  input.style.left = '-9999px'
  document.body.appendChild(input)
  input.select()
  document.execCommand('copy')
  document.body.removeChild(input)
}

export const toArray = (value) => (Array.isArray(value) ? value : [])
export const toNumber = (value) => Number(value || 0)

export const toList = (result) => {
  if (!result) return []
  if (Array.isArray(result.items)) return result.items
  if (Array.isArray(result.data)) return result.data
  if (Array.isArray(result.rows)) return result.rows
  if (Array.isArray(result)) return result
  return []
}
