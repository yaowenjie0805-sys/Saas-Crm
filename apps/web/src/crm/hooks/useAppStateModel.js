import { useAppStore, selectLang, selectAuth, selectPersistedFilters } from '../store/appStore'

export function useAppStateModel() {
  const lang = useAppStore(selectLang)
  const auth = useAppStore(selectAuth)
  const persisted = useAppStore(selectPersistedFilters)
  const setLang = useAppStore((state) => state.setLang)
  const setAuth = useAppStore((state) => state.setAuth)
  const readPageSize = useAppStore((state) => state.readPageSize)

  return {
    lang,
    setLang,
    auth,
    setAuth,
    persisted,
    readPageSize,
  }
}
