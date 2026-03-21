import { useCallback } from 'react'
import { api } from '../../../shared'

const DEFAULT_SSO_CONFIG = { enabled: false, providerName: '', mode: 'mock' }

export function useRuntimeInlineLoaders({
  beginPageRequest,
  authToken,
  lang,
  followCustomerId,
  followQ,
  setTasks,
  setFollowUps,
  setSsoConfig,
}) {
  const loadTasks = useCallback(async () => {
    const controller = beginPageRequest('tasks')
    const data = await api('/tasks/search?page=1&size=10', { signal: controller.signal }, authToken, lang)
    setTasks(data.items || [])
  }, [authToken, beginPageRequest, lang, setTasks])

  const loadFollowUps = useCallback(async () => {
    const query = new URLSearchParams({
      customerId: followCustomerId,
      q: followQ,
      page: '1',
      size: '8',
    })
    const controller = beginPageRequest('followUps')
    const data = await api('/follow-ups/search?' + query, { signal: controller.signal }, authToken, lang)
    setFollowUps(data.items || [])
  }, [authToken, beginPageRequest, followCustomerId, followQ, lang, setFollowUps])

  const loadSsoConfig = useCallback(async () => {
    try {
      const data = await api('/auth/sso/config', {}, null, lang)
      setSsoConfig(data || DEFAULT_SSO_CONFIG)
    } catch {
      setSsoConfig(DEFAULT_SSO_CONFIG)
    }
  }, [lang, setSsoConfig])

  return {
    loadTasks,
    loadFollowUps,
    loadSsoConfig,
  }
}
