import { useCallback, useMemo, useState } from 'react'
import { FILTERS_KEY, LANG_KEY } from '../shared'

const readStoredAuth = () => {
  try {
    const parsed = JSON.parse(localStorage.getItem('crm_auth') || 'null')
    if (!parsed || typeof parsed !== 'object') return null
    if (!String(parsed.token || '').trim()) return null
    return parsed
  } catch {
    return null
  }
}

const readStoredFilters = () => {
  try {
    return JSON.parse(localStorage.getItem(FILTERS_KEY) || '{}')
  } catch {
    return {}
  }
}

export function useAppStateModel() {
  const [lang, setLang] = useState(() => localStorage.getItem(LANG_KEY) || 'en')
  const [auth, setAuth] = useState(readStoredAuth)
  const persisted = useMemo(() => readStoredFilters(), [])

  const readPageSize = useCallback((key, fallback = 8) => {
    const raw = Number(localStorage.getItem(key) || fallback)
    if (!Number.isFinite(raw)) return fallback
    return Math.min(Math.max(Math.floor(raw), 5), 50)
  }, [])

  return {
    lang,
    setLang,
    auth,
    setAuth,
    persisted,
    readPageSize,
  }
}
