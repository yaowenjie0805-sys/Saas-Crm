export const normalizeRuntimePageSizeValue = (value) => {
  if (value === null || value === undefined) return null
  const num = Number(value)
  if (!Number.isFinite(num) || num <= 0) return null
  return String(Math.trunc(num))
}
