import { formatMoney, OPPORTUNITY_STAGE_OPTIONS, translateStage } from '../../../shared'

export function sortPipelineRows(opportunities, sortBy) {
  const sorted = [...(opportunities || [])]
  if (sortBy === 'amountDesc') sorted.sort((a, b) => Number(b.amount || 0) - Number(a.amount || 0))
  if (sortBy === 'amountAsc') sorted.sort((a, b) => Number(a.amount || 0) - Number(b.amount || 0))
  if (sortBy === 'progressDesc') sorted.sort((a, b) => Number(b.progress || 0) - Number(a.progress || 0))
  if (sortBy === 'progressAsc') sorted.sort((a, b) => Number(a.progress || 0) - Number(b.progress || 0))
  return sorted
}

export function getPipelineStageOptions(t) {
  return OPPORTUNITY_STAGE_OPTIONS.map((stage) => ({
    value: stage,
    label: translateStage(t, stage),
  }))
}

export function buildPipelineDetailRows(t, detail) {
  return [
    { label: t('idLabel'), value: detail?.id },
    { label: t('stage'), value: translateStage(t, detail?.stage) },
    { label: t('owner'), value: detail?.owner },
    { label: t('progress'), value: detail ? `${detail.progress}%` : '-' },
    { label: t('amount'), value: detail ? formatMoney(detail.amount) : '-' },
  ]
}
