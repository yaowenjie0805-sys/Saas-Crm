export const LEAD_FORM_STATUS_VALUES = ['NEW', 'QUALIFIED', 'NURTURING', 'DISQUALIFIED']
export const LEAD_FILTER_STATUS_VALUES = [...LEAD_FORM_STATUS_VALUES, 'CONVERTED']
export const PAGE_SIZE_VALUES = [8, 10, 20, 50]
export const IMPORT_STATUS_VALUES = ['ALL', 'PENDING', 'RUNNING', 'PARTIAL_SUCCESS', 'SUCCESS', 'FAILED', 'CANCELED']
export const IMPORT_EXPORT_STATUS_VALUES = ['ALL', 'PENDING', 'RUNNING', 'DONE', 'FAILED']
export const IMPORT_PAGE_SIZE_VALUES = [10, 20, 50]

export function safeSnippet(value, max = 120) {
  const text = String(value || '').trim()
  if (!text) return '-'
  return text.length > max ? `${text.slice(0, max)}...` : text
}
