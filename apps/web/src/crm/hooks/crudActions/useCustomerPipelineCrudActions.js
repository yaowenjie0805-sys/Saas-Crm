import { useCallback } from 'react'
import { api } from '../../shared'

export function useCustomerPipelineCrudActions({
  authToken,
  lang,
  canWrite,
  canDeleteCustomer,
  canDeleteOpportunity,
  handleError,
  setCrudError,
  setCrudFieldError,
  pickFieldErrors,
  formatValidation,
  validateCustomerForm,
  customerForm,
  setCustomerForm,
  customerPage,
  setCustomerPage,
  customers,
  opportunityForm,
  setOpportunityForm,
  opportunityPage,
  setOpportunityPage,
  opportunities,
  refreshPage,
}) {
  const saveCustomer = useCallback(async () => {
    if (!canWrite) return
    setCrudError('customer', '')
    setCrudFieldError('customer', {})
    const localErrors = validateCustomerForm()
    if (Object.keys(localErrors).length > 0) {
      setCrudFieldError('customer', localErrors)
      setCrudError('customer', Object.values(localErrors)[0])
      return
    }
    try {
      const valueRaw = String(customerForm.value || '').trim()
      const payload = {
        name: String(customerForm.name || '').trim(),
        owner: String(customerForm.owner || '').trim(),
        status: String(customerForm.status || '').trim(),
        tag: String(customerForm.tag || '').trim(),
        value: valueRaw ? Number(valueRaw) : 0,
      }
      if (customerForm.id) await api('/customers/' + customerForm.id, { method: 'PATCH', body: JSON.stringify(payload) }, authToken, lang)
      else await api('/customers', { method: 'POST', body: JSON.stringify(payload) }, authToken, lang)
      setCustomerForm({ id: '', name: '', owner: '', status: '', tag: '', value: '' })
      await refreshPage('customers', 'panel_action')
    } catch (err) {
      setCrudError('customer', formatValidation(err))
      setCrudFieldError('customer', pickFieldErrors(err, ['name', 'owner', 'status', 'value']))
      handleError(err)
    }
  }, [authToken, canWrite, customerForm, formatValidation, handleError, lang, pickFieldErrors, refreshPage, setCrudError, setCrudFieldError, setCustomerForm, validateCustomerForm])

  const editCustomer = useCallback((c) => {
    setCustomerForm({ id: c.id, name: c.name || '', owner: c.owner || '', status: c.status || '', tag: c.tag || '', value: String(c.value || '') })
  }, [setCustomerForm])

  const removeCustomer = useCallback(async (id) => {
    if (!canDeleteCustomer) return
    try {
      await api('/customers/' + id, { method: 'DELETE' }, authToken, lang)
      const nextPage = customerPage > 1 && customers.length <= 1 ? customerPage - 1 : customerPage
      if (nextPage !== customerPage) setCustomerPage(nextPage)
      await refreshPage('customers', 'panel_action')
    } catch (err) { handleError(err) }
  }, [authToken, canDeleteCustomer, customerPage, customers.length, handleError, lang, refreshPage, setCustomerPage])

  const saveOpportunity = useCallback(async () => {
    if (!canWrite) return
    setCrudError('opportunity', '')
    setCrudFieldError('opportunity', {})
    try {
      const payload = {
        stage: opportunityForm.stage,
        count: Number(opportunityForm.count || 0),
        amount: Number(opportunityForm.amount || 0),
        progress: Number(opportunityForm.progress || 0),
        owner: opportunityForm.owner,
      }
      if (opportunityForm.id) await api('/opportunities/' + opportunityForm.id, { method: 'PATCH', body: JSON.stringify(payload) }, authToken, lang)
      else await api('/opportunities', { method: 'POST', body: JSON.stringify(payload) }, authToken, lang)
      setOpportunityForm({ id: '', stage: '', count: '', amount: '', progress: '', owner: '' })
      await refreshPage('pipeline', 'panel_action')
    } catch (err) {
      setCrudError('opportunity', formatValidation(err))
      setCrudFieldError('opportunity', pickFieldErrors(err, ['stage', 'count', 'amount', 'progress', 'owner']))
      handleError(err)
    }
  }, [authToken, canWrite, formatValidation, handleError, lang, opportunityForm, pickFieldErrors, refreshPage, setCrudError, setCrudFieldError, setOpportunityForm])

  const editOpportunity = useCallback((o) => {
    setOpportunityForm({ id: o.id, stage: o.stage || '', count: String(o.count || ''), amount: String(o.amount || ''), progress: String(o.progress || ''), owner: o.owner || '' })
  }, [setOpportunityForm])

  const removeOpportunity = useCallback(async (id) => {
    if (!canDeleteOpportunity) return
    try {
      await api('/opportunities/' + id, { method: 'DELETE' }, authToken, lang)
      const nextPage = opportunityPage > 1 && opportunities.length <= 1 ? opportunityPage - 1 : opportunityPage
      if (nextPage !== opportunityPage) setOpportunityPage(nextPage)
      await refreshPage('pipeline', 'panel_action')
    } catch (err) { handleError(err) }
  }, [authToken, canDeleteOpportunity, handleError, lang, opportunities.length, opportunityPage, refreshPage, setOpportunityPage])

  return {
    saveCustomer,
    editCustomer,
    removeCustomer,
    saveOpportunity,
    editOpportunity,
    removeOpportunity,
  }
}

