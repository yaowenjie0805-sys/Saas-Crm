/**
 * Search utility functions extracted from CommandPalette.
 * Placed in a separate module to satisfy react-refresh/only-export-components rule.
 */

/**
 * Clamp the selected index within valid range.
 */
export function clampSelectedIndex(selectedIndex, flatResultsLength) {
  if (flatResultsLength <= 0) return 0
  return Math.min(Math.max(selectedIndex, 0), flatResultsLength - 1)
}

/**
 * Build search request headers with tenant ID.
 */
export function buildSearchHeaders(tenantId) {
  const normalizedTenant = String(tenantId || '').trim()
  if (!normalizedTenant) {
    return {}
  }
  return { 'X-Tenant-Id': normalizedTenant }
}

/**
 * Flatten search results grouped by type into a flat array.
 */
export function flattenSearchResults(results) {
  const flat = []
  Object.entries(results || {}).forEach(([type, items]) => {
    ;(Array.isArray(items) ? items : []).forEach((item) => {
      flat.push({ ...item, type })
    })
  })
  return flat
}
