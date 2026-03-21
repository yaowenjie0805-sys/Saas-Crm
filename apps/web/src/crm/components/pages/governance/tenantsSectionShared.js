export const PAGE_SIZES = [5, 8, 12, 20]
export const DATE_FORMAT_OPTIONS = ['yyyy-MM-dd', 'dd/MM/yyyy', 'MM-dd-yyyy']
export const MARKET_DEFAULTS = {
  CN: { currency: 'CNY', timezone: 'Asia/Shanghai', taxRule: 'VAT_CN', channels: '["WECOM","DINGTALK"]', dataResidency: 'CN' },
  GLOBAL: { currency: 'USD', timezone: 'UTC', taxRule: 'VAT_GLOBAL', channels: '["EMAIL","SLACK"]', dataResidency: 'GLOBAL' },
}

export function applyMarketDefaults(base, nextMarketProfile) {
  const prevMarketProfile = String(base?.marketProfile || 'CN').toUpperCase() === 'GLOBAL' ? 'GLOBAL' : 'CN'
  const marketProfile = String(nextMarketProfile || 'CN').toUpperCase() === 'GLOBAL' ? 'GLOBAL' : 'CN'
  const prevDefaults = MARKET_DEFAULTS[prevMarketProfile]
  const defaults = MARKET_DEFAULTS[marketProfile]
  const switched = prevMarketProfile !== marketProfile
  const normalize = (value) => String(value || '').trim()
  const pick = (field, fallback) => {
    const current = normalize(base?.[field])
    if (!switched) return current || fallback
    const prevDefault = normalize(prevDefaults?.[field])
    return !current || current === prevDefault ? fallback : current
  }
  return {
    ...base,
    marketProfile,
    currency: pick('currency', defaults.currency),
    timezone: pick('timezone', defaults.timezone),
    taxRule: pick('taxRule', defaults.taxRule),
    channels: pick('channels', defaults.channels),
    dataResidency: pick('dataResidency', defaults.dataResidency),
  }
}
