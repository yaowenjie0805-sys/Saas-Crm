import { useMemo } from 'react'
import { ApiContext } from './ApiContext'

export function AppProviders({ apiContext, children }) {
  const value = useMemo(() => ({
    token: apiContext?.token || '',
    lang: apiContext?.lang || 'en',
    tenantId: apiContext?.tenantId || '',
    requestId: apiContext?.requestId || '',
  }), [apiContext?.token, apiContext?.lang, apiContext?.tenantId, apiContext?.requestId])
  return <ApiContext.Provider value={value}>{children}</ApiContext.Provider>
}
