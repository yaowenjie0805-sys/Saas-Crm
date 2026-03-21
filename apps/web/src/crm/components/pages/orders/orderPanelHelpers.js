import { translateStatus } from '../../../shared'

export const EMPTY_FORM = { id: '', customerId: '', opportunityId: '', quoteId: '', owner: '', amount: '0', signDate: '' }
export const EMPTY_ROWS = []

export function getOrderStatusOptions(t) {
  return ['DRAFT', 'CONFIRMED', 'FULFILLING', 'COMPLETED', 'CANCELED'].map((status) => ({
    value: status,
    label: translateStatus(t, status),
  }))
}

export function formatOrderActionError(err, t) {
  if (err?.code === 'order_stage_gate_requires_quote_accepted') {
    const fallback = t('orderStageGateQuoteAcceptedRequired')
    const req = err?.details?.requiredStatus ? ` (${t('requiredStatusLabel')}: ${err.details.requiredStatus})` : ''
    return `${fallback}${req}`
  }
  if (err?.code === 'order_stage_gate_requires_fulfilling') {
    const fallback = t('orderStageGateFulfillingRequired')
    const req = err?.details?.requiredStatus ? ` (${t('requiredStatusLabel')}: ${err.details.requiredStatus})` : ''
    return `${fallback}${req}`
  }
  if (err?.code === 'order_status_transition_invalid') return t('orderStatusConflict')
  return err?.message || t('loadFailed')
}
