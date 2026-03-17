import { useMemo, useState } from 'react'

const DEFAULT_ROW_HEIGHT = 48
const DEFAULT_OVERSCAN = 8

export function useWindowedRows(rows, options = {}) {
  const rowHeight = options.rowHeight || DEFAULT_ROW_HEIGHT
  const overscan = options.overscan || DEFAULT_OVERSCAN
  const viewportHeight = options.viewportHeight || 420
  const [scrollTop, setScrollTop] = useState(0)

  const total = rows.length
  const visibleCount = Math.ceil(viewportHeight / rowHeight)
  const start = Math.max(0, Math.floor(scrollTop / rowHeight) - overscan)
  const end = Math.min(total, start + visibleCount + overscan * 2)

  const windowedRows = useMemo(() => rows.slice(start, end), [rows, start, end])
  const topSpacerHeight = start * rowHeight
  const bottomSpacerHeight = Math.max(0, (total - end) * rowHeight)

  return {
    windowedRows,
    topSpacerHeight,
    bottomSpacerHeight,
    onScroll: (event) => setScrollTop(event.currentTarget.scrollTop),
    viewportStyle: { maxHeight: `${viewportHeight}px`, overflowY: 'auto' },
  }
}
