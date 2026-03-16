import { useCallback } from 'react'
import { api } from '../shared'

export function useLeadCrudActions({
  authToken,
  lang,
  t,
  canWrite,
  handleError,
  formatValidation,
  pickFieldErrors,
  setCrudError,
  setCrudFieldError,
  leadForm,
  setLeadForm,
  leads,
  refreshPage,
  setQuotePrefill,
  onLeadConvertedToQuote,
}) {
  const saveLead = useCallback(async () => {
    if (!canWrite) return
    setCrudError('lead', '')
    setCrudFieldError('lead', {})
    if (!String(leadForm?.name || '').trim()) {
      const required = t('fieldRequired')
      setCrudFieldError('lead', { name: required })
      setCrudError('lead', required)
      return
    }
    try {
      const payload = {
        name: String(leadForm.name || '').trim(),
        company: String(leadForm.company || '').trim(),
        phone: String(leadForm.phone || '').trim(),
        email: String(leadForm.email || '').trim(),
        owner: String(leadForm.owner || '').trim(),
        source: String(leadForm.source || '').trim(),
        status: String(leadForm.status || 'NEW').trim(),
      }
      if (leadForm.id) await api('/v1/leads/' + leadForm.id, { method: 'PATCH', body: JSON.stringify(payload) }, authToken, lang)
      else await api('/v1/leads', { method: 'POST', body: JSON.stringify(payload) }, authToken, lang)
      setLeadForm({ id: '', name: '', company: '', phone: '', email: '', status: 'NEW', owner: '', source: '' })
      await refreshPage('leads', 'panel_action')
    } catch (err) {
      setCrudError('lead', formatValidation(err))
      setCrudFieldError('lead', pickFieldErrors(err, ['name', 'company', 'phone', 'email', 'owner', 'source', 'status']))
      handleError(err)
    }
  }, [authToken, canWrite, formatValidation, handleError, lang, leadForm, pickFieldErrors, refreshPage, setCrudError, setCrudFieldError, setLeadForm, t])

  const editLead = useCallback((row) => {
    setLeadForm({
      id: row.id,
      name: row.name || '',
      company: row.company || '',
      phone: row.phone || '',
      email: row.email || '',
      status: row.status || 'NEW',
      owner: row.owner || '',
      source: row.source || '',
    })
  }, [setLeadForm])

  const convertLead = useCallback(async (id) => {
    if (!canWrite) return
    try {
      const converted = await api('/v1/leads/' + id + '/convert', { method: 'POST', body: JSON.stringify({}) }, authToken, lang)
      const owner = String(converted?.lead?.owner || '').trim()
      const nextPrefill = {
        customerId: String(converted?.customerId || '').trim(),
        contactId: String(converted?.contactId || '').trim(),
        opportunityId: String(converted?.opportunityId || '').trim(),
        owner,
        sourceLeadId: String(converted?.lead?.id || '').trim(),
      }
      await Promise.all([
        refreshPage('leads', 'panel_action'),
        refreshPage('customers', 'panel_action'),
        refreshPage('contacts', 'panel_action'),
        refreshPage('pipeline', 'panel_action'),
      ])
      if (nextPrefill.customerId || nextPrefill.opportunityId) {
        setQuotePrefill?.(nextPrefill)
        onLeadConvertedToQuote?.(nextPrefill)
      }
    } catch (err) { handleError(err) }
  }, [authToken, canWrite, handleError, lang, onLeadConvertedToQuote, refreshPage, setQuotePrefill])

  const bulkAssignLeadsByRule = useCallback(async (idsInput) => {
    const ids = Array.isArray(idsInput) ? idsInput : []
    if (!ids.length) return
    try {
      await Promise.all(ids.map((id) => api('/v1/leads/' + id + '/assign', { method: 'POST', body: JSON.stringify({ useRule: true }) }, authToken, lang)))
      await refreshPage('leads', 'panel_action')
    } catch (err) { handleError(err) }
  }, [authToken, handleError, lang, refreshPage])

  const bulkUpdateLeadStatus = useCallback(async (statusValue, idsInput) => {
    const ids = Array.isArray(idsInput) ? idsInput : []
    if (!ids.length) return
    try {
      const leadById = new Map((leads || []).map((item) => [item.id, item]))
      await Promise.all(ids.map((id) => {
        const lead = leadById.get(id) || {}
        return api('/v1/leads/' + id, {
          method: 'PATCH',
          body: JSON.stringify({
            name: lead.name || '-',
            company: lead.company || '',
            phone: lead.phone || '',
            email: lead.email || '',
            owner: lead.owner || '',
            source: lead.source || '',
            status: statusValue,
          }),
        }, authToken, lang)
      }))
      await refreshPage('leads', 'panel_action')
    } catch (err) { handleError(err) }
  }, [authToken, handleError, lang, leads, refreshPage])

  return {
    saveLead,
    editLead,
    convertLead,
    bulkAssignLeadsByRule,
    bulkUpdateLeadStatus,
  }
}
