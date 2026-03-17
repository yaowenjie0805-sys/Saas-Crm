import { useEffect } from 'react'

export function useActivePagePolling({ enabled = true, pollers = [] }) {
  useEffect(() => {
    if (!enabled || !Array.isArray(pollers) || pollers.length === 0) return undefined

    const cleanups = []

    pollers.forEach((poller) => {
      if (!poller || typeof poller.run !== 'function') return
      const intervalMs = Number(poller.intervalMs || 0)
      if (!Number.isFinite(intervalMs) || intervalMs <= 0) return
      if (typeof poller.canRun === 'function' && !poller.canRun()) return

      let running = false
      let disposed = false
      const controller = new AbortController()

      const tick = async () => {
        if (disposed || running || controller.signal.aborted) return
        running = true
        try {
          await poller.run(controller.signal)
        } catch {
          // page-level handlers already process API errors
        } finally {
          running = false
        }
      }

      const timer = setInterval(tick, intervalMs)
      cleanups.push(() => {
        disposed = true
        controller.abort()
        clearInterval(timer)
      })
    })

    return () => {
      cleanups.forEach((fn) => {
        try { fn() } catch { /* noop */ }
      })
    }
  }, [enabled, pollers])
}
