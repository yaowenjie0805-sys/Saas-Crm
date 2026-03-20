import { createContext, useContext } from 'react'

export const ApiContext = createContext({ token: '', lang: 'en', tenantId: '' })

export function useApiContext() {
  return useContext(ApiContext)
}

