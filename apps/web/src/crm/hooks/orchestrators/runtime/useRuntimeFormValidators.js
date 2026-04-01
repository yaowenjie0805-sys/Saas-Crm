import {
  CONTRACT_STATUS_OPTIONS,
  CUSTOMER_STATUS_OPTIONS,
  PAYMENT_METHOD_OPTIONS,
  PAYMENT_STATUS_OPTIONS,
} from '../../../shared'
import { isValidDateStringByTenantFormat } from './routeConfig'

export function useRuntimeFormValidators({
  t,
  auth,
  loginForm,
  registerForm = {},
  ssoForm,
  contactForm,
  customerForm,
  contractForm,
  paymentForm,
  auditFrom,
  auditTo,
}) {
  const validateLogin = () => {
    const next = {}
    if (!loginForm.tenantId?.trim()) next.tenantId = t('fieldRequired')
    if (!loginForm.username?.trim()) next.username = t('fieldRequired')
    if (!loginForm.password?.trim()) next.password = t('fieldRequired')
    return next
  }

  const validateSso = () => {
    const next = {}
    if (!ssoForm.username?.trim()) next.username = t('fieldRequired')
    if (!ssoForm.code?.trim()) next.code = t('fieldRequired')
    return next
  }

  const validateRegister = () => {
    const next = {}
    if (!loginForm.tenantId?.trim()) next.tenantId = t('fieldRequired')
    if (!registerForm.username?.trim()) next.username = t('fieldRequired')
    if (!registerForm.password?.trim()) next.password = t('fieldRequired')
    if (!registerForm.confirmPassword?.trim()) next.confirmPassword = t('fieldRequired')
    if (
      registerForm.password?.trim() &&
      registerForm.confirmPassword?.trim() &&
      registerForm.password !== registerForm.confirmPassword
    ) {
      next.confirmPassword = t('passwordNotMatch')
    }
    return next
  }

  const validateContactForm = () => {
    const next = {}
    const phoneValue = String(contactForm.phone || '').trim()
    const emailValue = String(contactForm.email || '').trim()
    if (!String(contactForm.customerId || '').trim()) next.customerId = t('fieldRequired')
    if (!String(contactForm.name || '').trim()) next.name = t('fieldRequired')
    if (phoneValue && !/^\+?[0-9 ()-]{6,20}$/.test(phoneValue)) next.phone = t('invalidPhone')
    if (emailValue && !/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(emailValue)) next.email = t('invalidEmail')
    return next
  }

  const isValidDateString = (value) => isValidDateStringByTenantFormat(value, auth?.dateFormat)

  const validateCustomerForm = () => {
    const next = {}
    const valueRaw = String(customerForm.value || '').trim()
    if (!String(customerForm.name || '').trim()) next.name = t('fieldRequired')
    if (!String(customerForm.owner || '').trim()) next.owner = t('fieldRequired')
    if (!String(customerForm.status || '').trim()) next.status = t('fieldRequired')
    else if (!CUSTOMER_STATUS_OPTIONS.includes(customerForm.status)) next.status = t('invalidStatusOption')
    if (valueRaw) {
      const valueNum = Number(valueRaw)
      if (!Number.isFinite(valueNum)) next.value = t('invalidNumber')
      else if (valueNum < 0) next.value = t('nonNegativeNumber')
    }
    return next
  }

  const validateContractForm = () => {
    const next = {}
    const amountRaw = String(contractForm.amount || '').trim()
    if (!String(contractForm.customerId || '').trim()) next.customerId = t('fieldRequired')
    if (!String(contractForm.title || '').trim()) next.title = t('fieldRequired')
    if (!String(contractForm.status || '').trim()) next.status = t('fieldRequired')
    else if (!CONTRACT_STATUS_OPTIONS.includes(contractForm.status)) next.status = t('invalidStatusOption')
    if (amountRaw) {
      const amountNum = Number(amountRaw)
      if (!Number.isFinite(amountNum)) next.amount = t('invalidNumber')
      else if (amountNum < 0) next.amount = t('nonNegativeNumber')
    }
    if (!isValidDateString(contractForm.signDate)) next.signDate = t('invalidDateFormatText')
    return next
  }

  const validatePaymentForm = () => {
    const next = {}
    const amountRaw = String(paymentForm.amount || '').trim()
    if (!String(paymentForm.contractId || '').trim()) next.contractId = t('fieldRequired')
    if (amountRaw) {
      const amountNum = Number(amountRaw)
      if (!Number.isFinite(amountNum)) next.amount = t('invalidNumber')
      else if (amountNum < 0) next.amount = t('nonNegativeNumber')
    }
    if (!isValidDateString(paymentForm.receivedDate)) next.receivedDate = t('invalidDateFormatText')
    if (String(paymentForm.method || '').trim() && !PAYMENT_METHOD_OPTIONS.includes(paymentForm.method)) next.method = t('invalidMethodOption')
    if (String(paymentForm.status || '').trim() && !PAYMENT_STATUS_OPTIONS.includes(paymentForm.status)) next.status = t('invalidStatusOption')
    return next
  }

  const hasInvalidAuditRange = () => !!(auditFrom && auditTo && auditFrom > auditTo)

  return {
    validateLogin,
    validateRegister,
    validateSso,
    validateContactForm,
    validateCustomerForm,
    validateContractForm,
    validatePaymentForm,
    hasInvalidAuditRange,
  }
}
