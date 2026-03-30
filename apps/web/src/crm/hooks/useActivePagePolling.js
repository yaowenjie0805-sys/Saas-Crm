import { useEffect, useRef } from 'react'

export function useActivePagePolling({ enabled = true, pollers = [] }) {
  const pollerRecordsRef = useRef(new Map())

  useEffect(() => {
    const records = pollerRecordsRef.current
    const canRunPoller = (poller) => {
      if (typeof poller?.canRun !== 'function') return true
      try {
        return !!poller.canRun()
      } catch {
        return false
      }
    }

    if (!enabled || !Array.isArray(pollers) || pollers.length === 0) {
      records.forEach((record) => {
        record.dispose()
      })
      records.clear()
      return
    }

    const nextKeys = new Set()

    pollers.forEach((poller, index) => {
      if (!poller || typeof poller.run !== 'function') return

      const intervalMs = Number(poller.intervalMs || 0)
      if (!Number.isFinite(intervalMs) || intervalMs <= 0) return

      const key = String(poller.id ?? index)
      nextKeys.add(key)
      const canRunNow = canRunPoller(poller)

      const existing = records.get(key)
      if (!canRunNow) {
        if (existing) {
          existing.dispose()
          records.delete(key)
        }
        return
      }

      if (existing?.intervalMs === intervalMs) {
        existing.poller = poller
        return
      }

      if (existing) {
        existing.dispose()
      }

      const record = {
        intervalMs,
        poller,
        running: false,
        disposed: false,
        controller: new AbortController(),
        timer: null,
        dispose() {
          if (record.disposed) return
          record.disposed = true
          record.controller.abort()
          if (record.timer) {
            clearInterval(record.timer)
            record.timer = null
          }
        },
      }

      record.timer = setInterval(async () => {
        const currentPoller = record.poller
        if (record.disposed || record.running || record.controller.signal.aborted) return
        if (!canRunPoller(currentPoller)) return

        record.running = true
        try {
          await currentPoller.run(record.controller.signal)
        } catch {
          // page-level handlers already process API errors
        } finally {
          record.running = false
        }
      }, intervalMs)

      records.set(key, record)
    })

    records.forEach((record, key) => {
      if (nextKeys.has(key)) return
      record.dispose()
      records.delete(key)
    })
  }, [enabled, pollers])

  useEffect(() => () => {
    const records = pollerRecordsRef.current
    records.forEach((record) => {
      record.dispose()
    })
    records.clear()
  }, [])
}
