import { useEffect } from 'react'
import { useAppStore } from '../../store/appStore'
import { useMainBaseMapper } from '../mainContentMappers/useMainBaseMapper'

export function useAuthOrchestrator() {
  const auth = useAppStore((state) => state.auth.session)
  const lang = useAppStore((state) => state.auth.lang)
  const setAuth = useAppStore((state) => state.setAuth)
  const setLang = useAppStore((state) => state.setLang)
  const setAuthBaseModel = useAppStore((state) => state.setAuthBaseModel)
  return { auth, lang, setAuth, setLang, setAuthBaseModel }
}

export function useSyncAuthPageModels(params) {
  const { setAuthBaseModel } = useAuthOrchestrator()
  const { mainBase } = useMainBaseMapper(params)
  useEffect(() => {
    setAuthBaseModel(mainBase)
  }, [setAuthBaseModel, mainBase])
}

export function buildAuthPageModelInputs(ctx) {
  return ctx?.authModel || {}
}
