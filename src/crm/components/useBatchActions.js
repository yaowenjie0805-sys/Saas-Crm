import { useCallback, useMemo, useState } from 'react'
import { api } from '../shared'

const DEFAULT_CONCURRENCY = 4

function normalizeError(err) {
  const message = err?.message || 'Unknown error'
  const requestId = err?.requestId || ''
  return { message, requestId }
}

export function useBatchActions({ t, concurrency = DEFAULT_CONCURRENCY }) {
  const [running, setRunning] = useState(false)
  const [summary, setSummary] = useState(null)

  const clearSummary = useCallback(() => setSummary(null), [])

  const runBatch = useCallback(async ({ ids, worker, batch, canRun = true, emptyMessage, itemLabel = 'ID' }) => {
    if (!canRun || typeof worker !== 'function') return null
    if (!Array.isArray(ids) || ids.length === 0) {
      setSummary({
        title: emptyMessage || t('batchNoSelection'),
        requested: 0,
        success: 0,
        failed: 0,
        failures: [],
      })
      return { success: 0, failed: 0, failures: [] }
    }

    setRunning(true)
    try {
      if (batch?.path && batch?.token) {
        const payload = { action: batch.action || '', ids: [...ids], ...(batch.payload || {}) }
        const response = await api(batch.path, { method: 'POST', body: JSON.stringify(payload) }, batch.token, batch.lang || 'en')
        const failures = Array.isArray(response.failures)
          ? response.failures.map((row) => ({
            id: row.id,
            itemLabel,
            message: row.message || row.error || 'Unknown error',
            requestId: row.requestId || response.requestId || '',
          }))
          : []
        const success = Number(response.succeeded || 0)
        const failed = Number(response.failed || failures.length || 0)
        const requested = Number(response.requested || ids.length)
        const nextSummary = {
          title: `${t('batchDone')} - ${t('success')}: ${success}, ${t('failed')}: ${failed}`,
          requested,
          success,
          failed,
          skipped: Number(response.skipped || 0),
          notFound: Number(response.notFound || 0),
          forbidden: Number(response.forbidden || 0),
          failures,
        }
        setSummary(nextSummary)
        return nextSummary
      }

      let success = 0
      let failed = 0
      const failures = []

      for (let i = 0; i < ids.length; i += concurrency) {
        const group = ids.slice(i, i + concurrency)
        const result = await Promise.allSettled(group.map((id) => worker(id)))
        result.forEach((row, idx) => {
          const id = group[idx]
          if (row.status === 'fulfilled') {
            success += 1
            return
          }
          failed += 1
          const err = normalizeError(row.reason)
          failures.push({ id, itemLabel, ...err })
        })
      }

      const nextSummary = {
        title: `${t('batchDone')} - ${t('success')}: ${success}, ${t('failed')}: ${failed}`,
        requested: ids.length,
        success,
        failed,
        failures,
      }
      setSummary(nextSummary)
      return nextSummary
    } finally {
      setRunning(false)
    }
  }, [concurrency, t])

  const retryFailed = useCallback(async ({ summary: current, worker }) => {
    const failedIds = (current?.failures || []).map((row) => row.id).filter(Boolean)
    if (!failedIds.length || typeof worker !== 'function') return null
    return runBatch({ ids: failedIds, worker })
  }, [runBatch])

  const toastMessage = useMemo(() => summary?.title || '', [summary])

  return {
    running,
    summary,
    toastMessage,
    runBatch,
    retryFailed,
    clearSummary,
  }
}

export function buildBatchFailureText(summary) {
  if (!summary || !Array.isArray(summary.failures) || summary.failures.length === 0) return ''
  return summary.failures
    .map((row) => `${row.itemLabel}: ${row.id} | ${row.message}${row.requestId ? ` [${row.requestId}]` : ''}`)
    .join('\n')
}
