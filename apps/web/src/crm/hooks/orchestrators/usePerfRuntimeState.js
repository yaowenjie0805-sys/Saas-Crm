import { useMemo } from 'react'
import { useRuntimeSectionFields } from './useRuntimeSectionFields'

export function usePerfRuntimeState({ getMetrics }) {
  const defaults = useMemo(() => ({
    perfMetrics: () => (typeof getMetrics === 'function' ? getMetrics() : {}),
    lastPerfSnapshotAt: '',
    currentLoaderKey: '',
    lastRefreshReason: 'default',
    currentPageSignature: '',
    currentSignatureHit: false,
    recentWorkbenchJump: { targetPage: '', signature: '', reason: 'workbench_jump', hit: false, at: '' },
    domainLoadSource: {
      customer: { source: '', at: '' },
      commerce: { source: '', at: '' },
      governance: { source: '', at: '' },
      approval: { source: '', at: '' },
      reporting: { source: '', at: '' },
      workbench: { source: '', at: '' },
    },
  }), [getMetrics])

  return useRuntimeSectionFields('perfDomain', 'ui', defaults)
}
