import { useCallback } from 'react'
import { api } from '../../shared'

export function useContractPaymentCrudActions({
  authToken,
  lang,
  canWrite,
  canDeleteCustomer,
  handleError,
  setCrudError,
  setCrudFieldError,
  pickFieldErrors,
  formatValidation,
  validateContractForm,
  validatePaymentForm,
  contractForm,
  setContractForm,
  contractPage,
  setContractPage,
  contracts,
  paymentForm,
  setPaymentForm,
  paymentPage,
  setPaymentPage,
  payments,
  refreshPage,
}) {
  const saveContract = useCallback(async () => {
    if (!canWrite) return
    setCrudError('contract', '')
    setCrudFieldError('contract', {})
    const localErrors = validateContractForm()
    if (Object.keys(localErrors).length > 0) {
      setCrudFieldError('contract', localErrors)
      setCrudError('contract', Object.values(localErrors)[0])
      return
    }
    try {
      const amountRaw = String(contractForm.amount || '').trim()
      const payload = {
        customerId: String(contractForm.customerId || '').trim(),
        contractNo: String(contractForm.contractNo || '').trim(),
        title: String(contractForm.title || '').trim(),
        amount: amountRaw ? Number(amountRaw) : 0,
        status: String(contractForm.status || '').trim(),
        signDate: String(contractForm.signDate || '').trim(),
      }
      if (contractForm.id) await api('/contracts/' + contractForm.id, { method: 'PATCH', body: JSON.stringify(payload) }, authToken, lang)
      else await api('/contracts', { method: 'POST', body: JSON.stringify(payload) }, authToken, lang)
      setContractForm({ id: '', customerId: '', contractNo: '', title: '', amount: '', status: '', signDate: '' })
      await refreshPage('contracts', 'panel_action')
    } catch (err) {
      setCrudError('contract', formatValidation(err))
      setCrudFieldError('contract', pickFieldErrors(err, ['customerId', 'contractNo', 'title', 'amount', 'status', 'signDate']))
      handleError(err)
    }
  }, [authToken, canWrite, contractForm, formatValidation, handleError, lang, pickFieldErrors, refreshPage, setContractForm, setCrudError, setCrudFieldError, validateContractForm])

  const editContract = useCallback((c) => {
    setContractForm({ id: c.id, customerId: c.customerId || '', contractNo: c.contractNo || '', title: c.title || '', amount: String(c.amount || ''), status: c.status || '', signDate: c.signDate || '' })
  }, [setContractForm])

  const removeContract = useCallback(async (id) => {
    if (!canDeleteCustomer) return
    try {
      await api('/contracts/' + id, { method: 'DELETE' }, authToken, lang)
      const nextPage = contractPage > 1 && contracts.length <= 1 ? contractPage - 1 : contractPage
      if (nextPage !== contractPage) setContractPage(nextPage)
      await refreshPage('contracts', 'panel_action')
    } catch (err) { handleError(err) }
  }, [authToken, canDeleteCustomer, contractPage, contracts.length, handleError, lang, refreshPage, setContractPage])

  const savePayment = useCallback(async () => {
    if (!canWrite) return
    setCrudError('payment', '')
    setCrudFieldError('payment', {})
    const localErrors = validatePaymentForm()
    if (Object.keys(localErrors).length > 0) {
      setCrudFieldError('payment', localErrors)
      setCrudError('payment', Object.values(localErrors)[0])
      return
    }
    try {
      const amountRaw = String(paymentForm.amount || '').trim()
      const payload = {
        contractId: String(paymentForm.contractId || '').trim(),
        amount: amountRaw ? Number(amountRaw) : 0,
        receivedDate: String(paymentForm.receivedDate || '').trim(),
        method: String(paymentForm.method || '').trim(),
        status: String(paymentForm.status || '').trim(),
        remark: String(paymentForm.remark || '').trim(),
      }
      if (paymentForm.id) await api('/payments/' + paymentForm.id, { method: 'PATCH', body: JSON.stringify(payload) }, authToken, lang)
      else await api('/payments', { method: 'POST', body: JSON.stringify(payload) }, authToken, lang)
      setPaymentForm({ id: '', contractId: '', amount: '', receivedDate: '', method: '', status: '', remark: '' })
      await refreshPage('payments', 'panel_action')
    } catch (err) {
      setCrudError('payment', formatValidation(err))
      setCrudFieldError('payment', pickFieldErrors(err, ['contractId', 'amount', 'receivedDate', 'method', 'status', 'remark']))
      handleError(err)
    }
  }, [authToken, canWrite, formatValidation, handleError, lang, paymentForm, pickFieldErrors, refreshPage, setCrudError, setCrudFieldError, setPaymentForm, validatePaymentForm])

  const editPayment = useCallback((p) => {
    setPaymentForm({ id: p.id, contractId: p.contractId || '', amount: String(p.amount || ''), receivedDate: p.receivedDate || '', method: p.method || '', status: p.status || '', remark: p.remark || '' })
  }, [setPaymentForm])

  const removePayment = useCallback(async (id) => {
    if (!canDeleteCustomer) return
    try {
      await api('/payments/' + id, { method: 'DELETE' }, authToken, lang)
      const nextPage = paymentPage > 1 && payments.length <= 1 ? paymentPage - 1 : paymentPage
      if (nextPage !== paymentPage) setPaymentPage(nextPage)
      await refreshPage('payments', 'panel_action')
    } catch (err) { handleError(err) }
  }, [authToken, canDeleteCustomer, handleError, lang, paymentPage, payments.length, refreshPage, setPaymentPage])

  return {
    saveContract,
    editContract,
    removeContract,
    savePayment,
    editPayment,
    removePayment,
  }
}

