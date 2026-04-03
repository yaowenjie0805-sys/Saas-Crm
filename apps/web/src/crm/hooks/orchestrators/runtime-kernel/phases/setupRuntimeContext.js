import { useEffect, useMemo } from 'react'
import { ensureI18nNamespaces, getI18nNamespacesForPage, tFactory } from '../../../../i18n'
import { useAppStateModel } from '../../../useAppStateModel'
import { useRuntimeRouting, useRuntimeStateSlices } from '../../runtime'
import { useRuntimeKernelState } from '../useRuntimeKernelState'

export function useSetupRuntimeContext() {
  const { lang, setLang, auth, setAuth, persisted, readPageSize } = useAppStateModel()
  const runtime = useRuntimeStateSlices({ persisted, readPageSize }).runtime
  const { location, navigate, perfEnabledByQuery, apiContext, isAuthRoute } = useRuntimeRouting({ auth, lang })
  const t = useMemo(() => tFactory(lang), [lang])

  useEffect(() => {
    ensureI18nNamespaces(lang, getI18nNamespacesForPage(runtime.activePage)).catch(() => {})
  }, [lang, runtime.activePage])

  const kernel = useRuntimeKernelState({
    auth,
    setRecentWorkbenchJump: runtime.setRecentWorkbenchJump,
    setDomainLoadSource: runtime.setDomainLoadSource,
  })

  return {
    lang,
    setLang,
    auth,
    setAuth,
    runtime,
    location,
    navigate,
    perfEnabledByQuery,
    apiContext,
    isAuthRoute,
    t,
    kernel,
    pagePolicy: kernel.pagePolicy,
    perf: kernel.perf,
  }
}
