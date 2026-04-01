export const EMPTY_CUSTOMER360_MODULES = {
  contacts: [],
  opportunities: [],
  quotes: [],
  orders: [],
  contracts: [],
  payments: [],
  approvals: [],
  audits: [],
  notifications: [],
}

export function filterRequestedModules(modules, moduleKeys) {
  if (!Array.isArray(modules) || modules.length === 0) return moduleKeys
  const allowed = new Set(moduleKeys)
  return modules.filter((key) => allowed.has(key))
}

export function moduleServerFilterSignature(moduleName, maxAssocItems) {
  return {
    contacts: `size=${maxAssocItems}`,
    opportunities: `size=${maxAssocItems}`,
    contracts: `size=${maxAssocItems}`,
    payments: `size=${maxAssocItems}`,
    quotes: `size=${maxAssocItems}`,
    orders: `size=${maxAssocItems}`,
    approvals: 'limit=30',
    audits: `size=${maxAssocItems}`,
    notifications: 'status=ALL:size=30',
  }[moduleName] || `size=${maxAssocItems}`
}

export function orderCustomer360Modules(requestedModules, primaryModules, secondaryModules) {
  const primarySet = new Set(primaryModules)
  const secondarySet = new Set(secondaryModules)
  const primary = requestedModules.filter((moduleName) => primarySet.has(moduleName))
  const secondary = requestedModules.filter((moduleName) => secondarySet.has(moduleName))
  const consumed = new Set([...primary, ...secondary])
  const remaining = requestedModules.filter((moduleName) => !consumed.has(moduleName))
  return [...primary, ...secondary, ...remaining]
}

export function createResilientListLoader({ api, controller, token, lang, toList }) {
  return async (path, fallbackPath = '') => {
    try {
      return toList(await api(path, { signal: controller.signal }, token, lang))
    } catch {
      if (!fallbackPath) return []
      try {
        return toList(await api(fallbackPath, { signal: controller.signal }, token, lang))
      } catch {
        return []
      }
    }
  }
}

export function getCustomer360PrefetchSignature(moduleName, maxAssocItems) {
  if (moduleName === 'notifications') return 'status=ALL:size=30'
  if (moduleName === 'approvals') return 'limit=30'
  return `size=${maxAssocItems}`
}

export function createCustomer360PrefetchLoaders({ customerIdText, maxAssocItems, loadList, toArray }) {
  return {
    contacts: async () => toArray(await loadList(`/contacts/search?customerId=${encodeURIComponent(customerIdText)}&page=1&size=${maxAssocItems}`)).slice(0, maxAssocItems),
    opportunities: async () => toArray(await loadList(`/opportunities/search?customerId=${encodeURIComponent(customerIdText)}&page=1&size=${maxAssocItems}`)).slice(0, maxAssocItems),
    quotes: async () =>
      toArray(await loadList(`/v1/quotes?page=1&size=${maxAssocItems}&customerId=${encodeURIComponent(customerIdText)}`, '/v1/quotes?page=1&size=30'))
        .filter((item) => String(item.customerId || item.customer?.id || '') === customerIdText)
        .slice(0, maxAssocItems),
    orders: async () =>
      toArray(await loadList(`/v1/orders?page=1&size=${maxAssocItems}&customerId=${encodeURIComponent(customerIdText)}`, '/v1/orders?page=1&size=30'))
        .filter((item) => String(item.customerId || item.customer?.id || '') === customerIdText)
        .slice(0, maxAssocItems),
  }
}

export function rebalanceAdaptivePrefetchModules({
  currentModules,
  preferredModules,
  statsRef,
  adaptiveWindow,
  lowHitThreshold,
  highHitThreshold,
  highLatencyThreshold,
}) {
  const next = new Set(currentModules)
  preferredModules.forEach((moduleName) => {
    const stats = statsRef.current[moduleName]
    if (!stats || stats.total < adaptiveWindow) return
    const hitRate = stats.total > 0 ? Math.round((stats.hits / stats.total) * 100) : 0
    const avgLatency = stats.latencyCount > 0 ? Math.round(stats.latencyTotal / stats.latencyCount) : 0
    if (hitRate <= lowHitThreshold && avgLatency <= highLatencyThreshold) next.delete(moduleName)
    else if (hitRate >= highHitThreshold || avgLatency >= highLatencyThreshold) next.add(moduleName)
    statsRef.current[moduleName] = { total: 0, hits: 0, latencyTotal: 0, latencyCount: 0 }
  })
  const normalized = preferredModules.filter((moduleName) => next.has(moduleName))
  return normalized.length ? normalized : [...preferredModules]
}

export function createCustomer360ModuleLoaders({
  customerIdText,
  maxAssocItems,
  timedLoad,
  toArray,
}) {
  const normalizeValue = (value) => String(value ?? '').trim()
  const hasCustomerId = (value) => {
    const normalized = normalizeValue(value)
    return normalized === customerIdText || normalized.includes(customerIdText)
  }
  const tryMatchByKeys = (target, keys) => {
    if (!target || typeof target !== 'object') return false
    return keys.some((key) => {
      const value = target[key]
      if (Array.isArray(value)) return value.some((entry) => hasCustomerId(entry))
      return hasCustomerId(value)
    })
  }
  const notificationMatchesCustomer = (item) => {
    if (!customerIdText) return false
    const directKeys = ['customerId', 'bizId', 'refId', 'entityId', 'relatedId', 'targetId', 'taskId', 'recordId']
    if (tryMatchByKeys(item, directKeys)) return true

    const nestedBlocks = ['payload', 'data', 'meta', 'context', 'detail']
    for (const blockKey of nestedBlocks) {
      if (tryMatchByKeys(item?.[blockKey], directKeys)) return true
    }

    // Lightweight fallback: inspect shallow scalar values only.
    return Object.values(item || {}).some((value) => (
      (typeof value === 'string' || typeof value === 'number') && hasCustomerId(value)
    ))
  }

  return {
    contacts: async () => toArray(await timedLoad('contacts', `/contacts/search?customerId=${encodeURIComponent(customerIdText)}&page=1&size=${maxAssocItems}`)).slice(0, maxAssocItems),
    opportunities: async () => toArray(await timedLoad('opportunities', `/opportunities/search?customerId=${encodeURIComponent(customerIdText)}&page=1&size=${maxAssocItems}`)).slice(0, maxAssocItems),
    contracts: async () => toArray(await timedLoad('contracts', `/contracts/search?customerId=${encodeURIComponent(customerIdText)}&page=1&size=${maxAssocItems}`)).slice(0, maxAssocItems),
    payments: async () => toArray(await timedLoad('payments', `/payments/search?customerId=${encodeURIComponent(customerIdText)}&page=1&size=${maxAssocItems}`)).slice(0, maxAssocItems),
    quotes: async () =>
      toArray(await timedLoad('quotes', `/v1/quotes?page=1&size=${maxAssocItems}&customerId=${encodeURIComponent(customerIdText)}`, '/v1/quotes?page=1&size=30'))
        .filter((item) => String(item.customerId || item.customer?.id || '') === customerIdText)
        .slice(0, maxAssocItems),
    orders: async () =>
      toArray(await timedLoad('orders', `/v1/orders?page=1&size=${maxAssocItems}&customerId=${encodeURIComponent(customerIdText)}`, '/v1/orders?page=1&size=30'))
        .filter((item) => String(item.customerId || item.customer?.id || '') === customerIdText)
        .slice(0, maxAssocItems),
    approvals: async () =>
      toArray(await timedLoad('approvals', '/v1/approval/instances?limit=30'))
        .filter((item) => {
          const bizId = String(item.bizId || '')
          const ref = String(item.refId || '')
          return bizId === customerIdText || ref === customerIdText || bizId.includes(customerIdText) || ref.includes(customerIdText)
        })
        .slice(0, maxAssocItems),
    audits: async () => toArray(await timedLoad('audits', `/audit-logs/search?q=${encodeURIComponent(customerIdText)}&page=1&size=${maxAssocItems}`)).slice(0, maxAssocItems),
    notifications: async () =>
      toArray(await timedLoad('notifications', '/v1/integrations/notifications/jobs?status=ALL&page=1&size=30'))
        .filter((item) => notificationMatchesCustomer(item))
        .slice(0, maxAssocItems),
  }
}

export function buildCustomer360ViewModel({ detailModules, timeline, t, toArray, toNumber, thirtyDaysMs }) {
  const now = Date.now()
  const timelineRows = toArray(timeline)
  const followUpsRecent = timelineRows.filter((item) => {
    const dt = new Date(item.time || item.createdAt || '').getTime()
    if (!dt || Number.isNaN(dt)) return false
    return now - dt <= thirtyDaysMs
  })
  const inFlightAmount = toArray(detailModules.opportunities)
    .filter((item) => !String(item.stage || '').toLowerCase().includes('closed'))
    .reduce((sum, item) => sum + toNumber(item.amount), 0)
  const orderAmount = toArray(detailModules.orders).reduce((sum, item) => sum + toNumber(item.amount), 0)
  const paymentReceived = toArray(detailModules.payments)
    .filter((item) => ['RECEIVED', 'COMPLETED'].includes(String(item.status || '').toUpperCase()))
    .reduce((sum, item) => sum + toNumber(item.amount), 0)
  const pendingApprovals = toArray(detailModules.approvals)
    .filter((item) => ['PENDING', 'WAITING', 'SUBMITTED'].includes(String(item.status || '').toUpperCase())).length
  const paymentOutstanding = Math.max(orderAmount - paymentReceived, 0)
  const riskTags = []
  if (paymentOutstanding > 0) riskTags.push(t('paymentWarnings'))
  if (pendingApprovals > 0) riskTags.push(t('pendingApprovals'))
  if (followUpsRecent.length === 0) riskTags.push(t('overdueFollowUps'))
  return {
    related: detailModules,
    timeline: timelineRows,
    metrics: {
      recentFollowUps30d: followUpsRecent.length,
      inFlightAmount,
      orderAmount,
      paymentReceived,
      paymentOutstanding,
      pendingApprovals,
    },
    riskTags,
  }
}
