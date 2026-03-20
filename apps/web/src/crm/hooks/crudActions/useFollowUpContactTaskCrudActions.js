import { useCallback } from 'react'
import { api } from '../../shared'

export function useFollowUpContactTaskCrudActions({
  authToken,
  lang,
  canWrite,
  handleError,
  setCrudError,
  setCrudFieldError,
  pickFieldErrors,
  formatValidation,
  validateContactForm,
  followUpForm,
  setFollowUpForm,
  contactForm,
  setContactForm,
  contactPage,
  setContactPage,
  contacts,
  refreshPage,
}) {
  const toggleTaskDone = useCallback(async (task) => {
    if (!canWrite) return
    try {
      await api('/tasks/' + task.id, { method: 'PATCH', body: JSON.stringify({ done: !task.done }) }, authToken, lang)
      await refreshPage('tasks', 'panel_action')
    } catch (err) { handleError(err) }
  }, [authToken, canWrite, handleError, lang, refreshPage])

  const saveFollowUp = useCallback(async () => {
    if (!canWrite) return
    setCrudError('followUp', '')
    setCrudFieldError('followUp', {})
    try {
      const payload = {
        customerId: followUpForm.customerId,
        summary: followUpForm.summary,
        channel: followUpForm.channel,
        result: followUpForm.result,
        nextActionDate: followUpForm.nextActionDate,
      }
      if (followUpForm.id) await api('/follow-ups/' + followUpForm.id, { method: 'PATCH', body: JSON.stringify(payload) }, authToken, lang)
      else await api('/follow-ups', { method: 'POST', body: JSON.stringify(payload) }, authToken, lang)
      setFollowUpForm({ id: '', customerId: '', summary: '', channel: '', result: '', nextActionDate: '' })
      await refreshPage('followUps', 'panel_action')
    } catch (err) {
      setCrudError('followUp', formatValidation(err))
      setCrudFieldError('followUp', pickFieldErrors(err, ['customerId', 'summary', 'channel', 'result', 'nextActionDate']))
      handleError(err)
    }
  }, [authToken, canWrite, followUpForm, formatValidation, handleError, lang, pickFieldErrors, refreshPage, setCrudError, setCrudFieldError, setFollowUpForm])

  const editFollowUp = useCallback((f) => {
    setFollowUpForm({ id: f.id, customerId: f.customerId || '', summary: f.summary || '', channel: f.channel || '', result: f.result || '', nextActionDate: f.nextActionDate || '' })
  }, [setFollowUpForm])

  const removeFollowUp = useCallback(async (id) => {
    if (!canWrite) return
    try {
      await api('/follow-ups/' + id, { method: 'DELETE' }, authToken, lang)
      await refreshPage('followUps', 'panel_action')
    } catch (err) { handleError(err) }
  }, [authToken, canWrite, handleError, lang, refreshPage])

  const saveContact = useCallback(async () => {
    if (!canWrite) return
    setCrudError('contact', '')
    setCrudFieldError('contact', {})
    const localErrors = validateContactForm()
    if (Object.keys(localErrors).length > 0) {
      setCrudFieldError('contact', localErrors)
      setCrudError('contact', Object.values(localErrors)[0])
      return
    }
    try {
      const payload = {
        customerId: String(contactForm.customerId || '').trim(),
        name: String(contactForm.name || '').trim(),
        title: String(contactForm.title || '').trim(),
        phone: String(contactForm.phone || '').trim(),
        email: String(contactForm.email || '').trim(),
      }
      if (contactForm.id) await api('/contacts/' + contactForm.id, { method: 'PATCH', body: JSON.stringify(payload) }, authToken, lang)
      else await api('/contacts', { method: 'POST', body: JSON.stringify(payload) }, authToken, lang)
      setContactForm({ id: '', customerId: '', name: '', title: '', phone: '', email: '' })
      await refreshPage('contacts', 'panel_action')
    } catch (err) {
      setCrudError('contact', formatValidation(err))
      setCrudFieldError('contact', pickFieldErrors(err, ['customerId', 'name', 'title', 'phone', 'email']))
      handleError(err)
    }
  }, [authToken, canWrite, contactForm, formatValidation, handleError, lang, pickFieldErrors, refreshPage, setContactForm, setCrudError, setCrudFieldError, validateContactForm])

  const editContact = useCallback((c) => {
    setContactForm({ id: c.id, customerId: c.customerId || '', name: c.name || '', title: c.title || '', phone: c.phone || '', email: c.email || '' })
  }, [setContactForm])

  const removeContact = useCallback(async (id) => {
    if (!canWrite) return
    try {
      await api('/contacts/' + id, { method: 'DELETE' }, authToken, lang)
      const nextPage = contactPage > 1 && contacts.length <= 1 ? contactPage - 1 : contactPage
      if (nextPage !== contactPage) setContactPage(nextPage)
      await refreshPage('contacts', 'panel_action')
    } catch (err) { handleError(err) }
  }, [authToken, canWrite, contactPage, contacts.length, handleError, lang, refreshPage, setContactPage])

  return {
    toggleTaskDone,
    saveFollowUp,
    editFollowUp,
    removeFollowUp,
    saveContact,
    editContact,
    removeContact,
  }
}

