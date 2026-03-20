import { useCallback } from 'react'
import { api } from '../shared'

export function useWorkbenchDomainLoaders({
  authToken,
  lang,
  auditFrom,
  auditTo,
  reportOwner,
  reportDepartment,
  reportTimezone,
  setWorkbenchToday,
  setCustomerTimeline,
  setOpportunityTimeline,
}) {
  const loadWorkbenchToday = useCallback(async () => {
    if (!authToken) return
    const q = new URLSearchParams({
      from: auditFrom,
      to: auditTo,
      owner: reportOwner,
      department: reportDepartment,
      timezone: reportTimezone || (Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Shanghai'),
    })
    const data = await api('/v1/workbench/today?' + q, {}, authToken, lang)
    setWorkbenchToday(data || null)
  }, [authToken, lang, auditFrom, auditTo, reportOwner, reportDepartment, reportTimezone, setWorkbenchToday])

  const loadCustomerTimeline = useCallback(async (customerId) => {
    if (!authToken || !customerId) {
      setCustomerTimeline([])
      return []
    }
    const data = await api('/v1/customers/' + customerId + '/timeline', {}, authToken, lang)
    const items = data.items || []
    setCustomerTimeline(items)
    return items
  }, [authToken, lang, setCustomerTimeline])

  const loadOpportunityTimeline = useCallback(async (opportunityId) => {
    if (!authToken || !opportunityId) {
      setOpportunityTimeline([])
      return []
    }
    const data = await api('/v1/opportunities/' + opportunityId + '/timeline', {}, authToken, lang)
    const items = data.items || []
    setOpportunityTimeline(items)
    return items
  }, [authToken, lang, setOpportunityTimeline])

  return {
    loadWorkbenchToday,
    loadCustomerTimeline,
    loadOpportunityTimeline,
  }
}

