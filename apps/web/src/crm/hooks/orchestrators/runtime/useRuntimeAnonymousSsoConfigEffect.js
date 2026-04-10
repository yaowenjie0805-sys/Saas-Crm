import { useEffect } from 'react'

export function useRuntimeAnonymousSsoConfigEffect({
  authToken,
  lang,
  loadSsoConfig,
  ssoConfigLoadKeyRef,
}) {
  useEffect(() => {
    if (authToken) {
      ssoConfigLoadKeyRef.current = ''
      return
    }
    const loadKey = `${lang}:anonymous`
    if (ssoConfigLoadKeyRef.current === loadKey) return
    ssoConfigLoadKeyRef.current = loadKey
    loadSsoConfig()
  }, [lang, authToken, loadSsoConfig, ssoConfigLoadKeyRef])
}
