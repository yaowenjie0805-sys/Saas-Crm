export const DEFAULT_CHANNELS_BY_MARKET = {
  CN: '["WECOM","DINGTALK"]',
  GLOBAL: '["EMAIL","SLACK"]',
}

export function parseRuleMembers(text) {
  return String(text || '')
    .split(',')
    .map((pair) => {
      const [usernameRaw, weightRaw] = pair.split(':')
      const username = String(usernameRaw || '').trim()
      const weight = Number(weightRaw || 1)
      if (!username) return null
      return { username, weight: Number.isFinite(weight) && weight > 0 ? Math.floor(weight) : 1 }
    })
    .filter((row) => row.username)
}

export function emitTenantConfigUpdated(payload) {
  if (typeof window === 'undefined' || typeof window.dispatchEvent !== 'function') return
  try {
    window.dispatchEvent(new CustomEvent('crm:tenant-config-updated', { detail: payload || {} }))
  } catch {
    // keep save flow resilient even if event construction fails
  }
}

export function withRequestIdMessage(err, fallback) {
  const message = String(err?.message || fallback || '').trim()
  const requestId = String(err?.requestId || '').trim()
  return requestId ? `${message} [${requestId}]` : message
}

export function normalizeTenantPayload(raw, normalizeDateFormat) {
  const marketProfile = String(raw?.marketProfile || 'CN').trim().toUpperCase() === 'GLOBAL' ? 'GLOBAL' : 'CN'
  const currency = String(raw?.currency || '').trim().toUpperCase() || (marketProfile === 'GLOBAL' ? 'USD' : 'CNY')
  const timezone = String(raw?.timezone || '').trim() || (marketProfile === 'GLOBAL' ? 'UTC' : 'Asia/Shanghai')
  const dateFormat = normalizeDateFormat(raw?.dateFormat)
  return {
    name: String(raw?.name || '').trim(),
    status: String(raw?.status || 'ACTIVE').trim().toUpperCase() || 'ACTIVE',
    quotaUsers: Number(raw?.quotaUsers || 100),
    timezone,
    currency,
    dateFormat,
    marketProfile,
    taxRule: String(raw?.taxRule || (marketProfile === 'GLOBAL' ? 'VAT_GLOBAL' : 'VAT_CN')).trim(),
    approvalMode: String(raw?.approvalMode || 'STRICT').trim().toUpperCase() === 'STAGE_GATE' ? 'STAGE_GATE' : 'STRICT',
    channels: String(raw?.channels || DEFAULT_CHANNELS_BY_MARKET[marketProfile]).trim() || DEFAULT_CHANNELS_BY_MARKET[marketProfile],
    dataResidency: String(raw?.dataResidency || marketProfile).trim().toUpperCase() || marketProfile,
    maskLevel: String(raw?.maskLevel || 'STANDARD').trim().toUpperCase() || 'STANDARD',
  }
}
