import { useCallback, useMemo, useRef, useState } from 'react'

const DEFAULT_ROW_HEIGHT = 48
const DEFAULT_OVERSCAN = 8
const SCROLL_THROTTLE_MS = 16

export function useWindowedRows(rows, options = {}) {
  const rowHeight = options.rowHeight || DEFAULT_ROW_HEIGHT
  const overscan = options.overscan || DEFAULT_OVERSCAN
  const viewportHeight = options.viewportHeight || 420
  const [scrollTop, setScrollTop] = useState(0)
  const lastScrollUpdate = useRef(0)
  const rafId = useRef(null)

  const total = rows.length
  const visibleCount = Math.ceil(viewportHeight / rowHeight)
  const start = Math.max(0, Math.floor(scrollTop / rowHeight) - overscan)
  const end = Math.min(total, start + visibleCount + overscan * 2)

  const windowedRows = useMemo(() => rows.slice(start, end), [rows, start, end])
  const topSpacerHeight = start * rowHeight
  const bottomSpacerHeight = Math.max(0, (total - end) * rowHeight)

  const onScroll = useCallback((event) => {
    const target = event.currentTarget
    const now = performance.now()
    if (now - lastScrollUpdate.current < SCROLL_THROTTLE_MS) {
      if (rafId.current) cancelAnimationFrame(rafId.current)
      rafId.current = requestAnimationFrame(() => {
        setScrollTop(target.scrollTop)
        lastScrollUpdate.current = performance.now()
      })
      return
    }
    lastScrollUpdate.current = now
    setScrollTop(target.scrollTop)
  }, [])

  const viewportStyle = useMemo(
    () => ({ maxHeight: `${viewportHeight}px`, overflowY: 'auto' }),
    [viewportHeight]
  )

  return {
    windowedRows,
    topSpacerHeight,
    bottomSpacerHeight,
    onScroll,
    viewportStyle,
  }
}
