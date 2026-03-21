export function formatValidationError(err) {
  const validationErrors = err?.validationErrors
  if (!validationErrors || typeof validationErrors !== 'object') return err?.message
  const first = Object.entries(validationErrors)[0]
  return first ? `${first[0]}: ${first[1]}` : err?.message
}

export function formatRuntimeErrorMessage(err) {
  const message = err?.status === 400 ? formatValidationError(err) : err?.message
  return err?.requestId ? `${message} [${err.requestId}]` : message
}
