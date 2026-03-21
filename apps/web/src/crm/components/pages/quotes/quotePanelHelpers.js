import { formatMoney, translateStatus } from '../../../shared'

export const EMPTY_FORM = { id: '', customerId: '', opportunityId: '', owner: '', status: 'DRAFT', validUntil: '' }
export const EMPTY_ROWS = []
export const NOOP = () => {}

export function formatRequestError(err, t) {
  const code = String(err?.code || '').trim().toLowerCase()
  const message = code === 'quote_stage_gate_requires_approval'
    ? t('quoteStageGateRequiresApproval')
    : String(err?.message || 'request_failed')
  const requestId = String(err?.requestId || '').trim()
  return requestId ? `${message} [${requestId}]` : message
}

export function getQuoteStatusOptions(t) {
  return ['DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED'].map((status) => ({
    value: status,
    label: translateStatus(t, status),
  }))
}

export function formatQuoteAmount(value) {
  return formatMoney(value)
}
