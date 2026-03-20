export const I18N = { en: {}, zh: {} }

const BASE_LANG_LOADERS = {
  en: () => import('./i18n/common/en'),
  zh: () => import('./i18n/common/zh'),
}

const I18N_NS_LOADERS = {
  'market-dashboard': () => import('./i18n/namespaces/market-dashboard'),
  'market-governance': () => import('./i18n/namespaces/market-governance'),
  'opportunity-workbench': () => import('./i18n/namespaces/opportunity-workbench'),
  'report-designer': () => import('./i18n/namespaces/report-designer'),
}

const I18N_NS_BY_PAGE = {
  dashboard: ['market-dashboard', 'opportunity-workbench'],
  customers: ['opportunity-workbench'],
  pipeline: ['opportunity-workbench'],
  reports: ['market-dashboard'],
  governance: ['market-governance'],
  reportDesigner: ['report-designer'],
}

const i18nBaseLoaded = {
  en: false,
  zh: false,
}

const i18nBaseInFlight = {
  en: null,
  zh: null,
}

const i18nNsLoaded = {
  en: new Set(),
  zh: new Set(),
}

const i18nNsInFlight = {
  en: new Map(),
  zh: new Map(),
}

const normalizeLang = (lang) => (lang === 'zh' ? 'zh' : 'en')

export const tFactory = (lang) => {
  const safeLang = normalizeLang(lang)
  return (k) => I18N[safeLang]?.[k] ?? k
}

export async function ensureI18nBase(lang) {
  const safeLang = normalizeLang(lang)
  if (i18nBaseLoaded[safeLang]) {
    return
  }
  if (i18nBaseInFlight[safeLang]) {
    await i18nBaseInFlight[safeLang]
    return
  }
  const loader = BASE_LANG_LOADERS[safeLang]
  const task = loader()
    .then((mod) => {
      const pack = safeLang === 'zh' ? mod?.ZH || mod?.default : mod?.EN || mod?.default
      I18N[safeLang] = pack && typeof pack === 'object' ? pack : {}
      i18nBaseLoaded[safeLang] = true
      i18nNsLoaded[safeLang].add('common')
    })
    .finally(() => {
      i18nBaseInFlight[safeLang] = null
    })
  i18nBaseInFlight[safeLang] = task
  await task
}

export function getI18nNamespacesForPage(pageKey) {
  if (!pageKey) return ['common']
  const specific = I18N_NS_BY_PAGE[pageKey]
  return specific ? ['common', ...specific] : ['common']
}

export async function ensureI18nNamespaces(lang, namespaces = []) {
  const safeLang = normalizeLang(lang)
  await ensureI18nBase(safeLang)
  const target = Array.from(new Set(Array.isArray(namespaces) ? namespaces : []))
  for (const ns of target) {
    if (!ns || ns === 'common') continue
    if (i18nNsLoaded[safeLang].has(ns)) continue
    const existing = i18nNsInFlight[safeLang].get(ns)
    if (existing) {
      await existing
      continue
    }
    const loader = I18N_NS_LOADERS[ns]
    if (!loader) continue
    const task = loader()
      .then((mod) => {
        const exportKey = safeLang === 'zh' ? `ZH_${String(ns).replace(/-/g, '_').toUpperCase()}` : `EN_${String(ns).replace(/-/g, '_').toUpperCase()}`
        const patch = mod?.[exportKey]
        if (patch && typeof patch === 'object') {
          Object.assign(I18N[safeLang], patch)
        }
        i18nNsLoaded[safeLang].add(ns)
      })
      .finally(() => {
        i18nNsInFlight[safeLang].delete(ns)
      })
    i18nNsInFlight[safeLang].set(ns, task)
    await task
  }
}
