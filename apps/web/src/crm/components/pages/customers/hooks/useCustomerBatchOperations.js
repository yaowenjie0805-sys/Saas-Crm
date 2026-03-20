import { useState } from 'react'
import { api } from '../../../../shared'
import { useBatchActions } from '../../../useBatchActions'

/**
 * Encapsulates all customer batch operations while preserving existing semantics.
 */
export function useCustomerBatchOperations({
  t,
  canDeleteCustomer,
  selectedIds,
  clearSelection,
  onRefresh,
  token,
  lang,
  byId,
}) {
  const [batchOwner, setBatchOwner] = useState('')
  const [batchStatus, setBatchStatus] = useState('')
  const [batchModalOpen, setBatchModalOpen] = useState(false)
  const { summary: batchSummary, toastMessage: batchMessage, runBatch, clearSummary } = useBatchActions({ t })

  const updateOne = async (id, patch) => {
    const row = byId.get(id)
    if (!row) return
    const payload = {
      name: String(row.name || '').trim(),
      owner: String(patch.owner ?? row.owner ?? '').trim(),
      status: String(patch.status ?? row.status ?? '').trim(),
      tag: String(row.tag || '').trim(),
      value: Number(row.value || 0),
    }
    await api('/customers/' + id, { method: 'PATCH', body: JSON.stringify(payload) }, token, lang)
  }

  const batchDelete = async () => {
    if (!canDeleteCustomer) return
    const result = await runBatch({
      ids: [...selectedIds],
      worker: (id) => api('/customers/' + id, { method: 'DELETE' }, token, lang),
      batch: { path: '/v1/customers/batch-actions', action: 'DELETE', token, lang },
      canRun: canDeleteCustomer,
    })
    if (result?.failed > 0) setBatchModalOpen(true)
    clearSelection()
    if (onRefresh) await onRefresh()
  }

  const batchAssign = async () => {
    if (!batchOwner.trim()) return
    const result = await runBatch({
      ids: [...selectedIds],
      worker: (id) => updateOne(id, { owner: batchOwner.trim() }),
      batch: { path: '/v1/customers/batch-actions', action: 'ASSIGN_OWNER', payload: { owner: batchOwner.trim() }, token, lang },
    })
    if (result?.failed > 0) setBatchModalOpen(true)
    clearSelection()
    if (onRefresh) await onRefresh()
  }

  const batchChangeStatus = async () => {
    if (!batchStatus) return
    const result = await runBatch({
      ids: [...selectedIds],
      worker: (id) => updateOne(id, { status: batchStatus }),
      batch: { path: '/v1/customers/batch-actions', action: 'UPDATE_STATUS', payload: { status: batchStatus }, token, lang },
    })
    if (result?.failed > 0) setBatchModalOpen(true)
    clearSelection()
    if (onRefresh) await onRefresh()
  }

  return {
    batchOwner,
    setBatchOwner,
    batchStatus,
    setBatchStatus,
    batchModalOpen,
    setBatchModalOpen,
    batchSummary,
    batchMessage,
    batchDelete,
    batchAssign,
    batchChangeStatus,
    clearSummary,
  }
}
