import { memo, Suspense, useCallback, useMemo, useState } from 'react'
import { lazyNamed } from './shared'

const ApprovalStatsCards = lazyNamed(() => import('./approvals/sections'), 'ApprovalStatsCards')
const ApprovalTemplateSection = lazyNamed(() => import('./approvals/sections'), 'ApprovalTemplateSection')
const ApprovalTaskSection = lazyNamed(() => import('./approvals/sections'), 'ApprovalTaskSection')

function ApprovalsPageContainer({ activePage, t, approvals, refreshApprovals }) {
  const [editingTemplateId, setEditingTemplateId] = useState('')
  const [selectedNodeIndex, setSelectedNodeIndex] = useState(0)
  const [expandedVersionId, setExpandedVersionId] = useState('')
  const [previewCtx, setPreviewCtx] = useState({ amount: '0', role: 'SALES', department: 'DEFAULT' })

  const editingTemplate = useMemo(() => (approvals.templates || []).find((x) => x.id === editingTemplateId) || null, [approvals.templates, editingTemplateId])
  const editingNodes = useMemo(() => Array.isArray(editingTemplate?.flowDefinition?.nodes) ? editingTemplate.flowDefinition.nodes : [], [editingTemplate])
  const previewMatched = useMemo(() => {
    const amount = Number(previewCtx.amount || 0)
    return editingNodes.filter((node) => {
      const c = node.conditions || {}
      if (c.amountMin !== null && c.amountMin !== undefined && c.amountMin !== '' && amount < Number(c.amountMin)) return false
      if (c.amountMax !== null && c.amountMax !== undefined && c.amountMax !== '' && amount > Number(c.amountMax)) return false
      if (c.role && String(c.role).toUpperCase() !== String(previewCtx.role || '').toUpperCase()) return false
      if (c.department && String(c.department).toUpperCase() !== String(previewCtx.department || '').toUpperCase()) return false
      return true
    }).map((x) => x.id)
  }, [editingNodes, previewCtx])

  const applyTemplatePatch = useCallback((templateId, patcher) => {
    approvals.setTemplates((prev) => prev.map((x) => (x.id === templateId ? patcher(x) : x)))
  }, [approvals])
  const updateNode = useCallback((index, patch) => {
    if (!editingTemplate) return
    const nextNodes = editingNodes.map((node, idx) => idx === index ? { ...node, ...patch } : node)
    applyTemplatePatch(editingTemplate.id, (x) => ({ ...x, flowDefinition: { ...(x.flowDefinition || {}), nodes: nextNodes } }))
  }, [applyTemplatePatch, editingNodes, editingTemplate])
  const moveNode = useCallback((from, to) => {
    if (!editingTemplate || from === to || from < 0 || to < 0 || from >= editingNodes.length || to >= editingNodes.length) return
    const arr = editingNodes.map((n) => ({ ...n }))
    const [moved] = arr.splice(from, 1)
    arr.splice(to, 0, moved)
    const normalized = arr.map((n, idx) => ({ ...n, seq: idx + 1 }))
    applyTemplatePatch(editingTemplate.id, (x) => ({ ...x, flowDefinition: { ...(x.flowDefinition || {}), nodes: normalized } }))
    setSelectedNodeIndex(to)
  }, [applyTemplatePatch, editingNodes, editingTemplate])
  const addNode = useCallback(() => {
    if (!editingTemplate) return
    const nextSeq = editingNodes.length + 1
    const node = { id: `node_${nextSeq}`, seq: nextSeq, approverRoles: ['MANAGER'], conditions: {}, slaMinutes: 240, escalateToRoles: ['ADMIN'] }
    applyTemplatePatch(editingTemplate.id, (x) => ({ ...x, flowDefinition: { ...(x.flowDefinition || {}), nodes: [...editingNodes, node] } }))
    setSelectedNodeIndex(editingNodes.length)
  }, [applyTemplatePatch, editingNodes, editingTemplate])
  const formatDateTime = useCallback((value) => {
    const text = String(value || '')
    return text ? text.replace('T', ' ').slice(0, 19) : '-'
  }, [])
  const summarizeVersionDiff = useCallback((currentTemplate, versionRow) => {
    const backendDiff = versionRow?.diffSummary
    if (backendDiff && Array.isArray(backendDiff.changes)) {
      return `${t('approvalDiffNode')}: ${backendDiff.nodeDelta >= 0 ? '+' : ''}${backendDiff.nodeDelta} | ${t('approvalDiffChangeCount')}: ${backendDiff.changes.length}`
    }
    if (!currentTemplate || !versionRow) return t('noData')
    const currentNodes = Array.isArray(currentTemplate.flowDefinition?.nodes) ? currentTemplate.flowDefinition.nodes : []
    const versionNodes = Array.isArray(versionRow.flowDefinition?.nodes) ? versionRow.flowDefinition.nodes : []
    const nodeDelta = currentNodes.length - versionNodes.length
    const currentSla = currentNodes.reduce((acc, row) => acc + Number(row?.slaMinutes || 0), 0)
    const versionSla = versionNodes.reduce((acc, row) => acc + Number(row?.slaMinutes || 0), 0)
    return `${t('approvalDiffNode')}: ${nodeDelta >= 0 ? '+' : ''}${nodeDelta} | ${t('approvalDiffSla')}: ${currentSla - versionSla >= 0 ? '+' : ''}${currentSla - versionSla}`
  }, [t])
  const summarizeNodeChange = useCallback((row) => {
    if (!row?.delta || typeof row.delta !== 'object' || Object.keys(row.delta).length === 0) return row?.type || '-'
    return Object.entries(row.delta).map(([key, val]) => `${key}: ${val?.from || '-'} -> ${val?.to || '-'}`).join(' | ')
  }, [])
  const confirmRollback = useCallback((row) => {
    const hint = summarizeVersionDiff(editingTemplate, row)
    const ok = window.confirm(`${t('approvalRollbackConfirm')} v${row.version}\n${hint}`)
    if (!ok) return
    approvals.rollbackTemplate(row.templateId, row.version)
  }, [approvals, editingTemplate, summarizeVersionDiff, t])
  if (activePage !== 'approvals') return null

  return (
    <section className="panel">
      <div className="panel-head"><h2>{t('approvals')}</h2></div>
      <Suspense fallback={<div className="loading">{t('loading')}</div>}>
        <ApprovalStatsCards t={t} stats={approvals.stats} />
        <ApprovalTemplateSection
          t={t}
          approvals={approvals}
          refreshApprovals={refreshApprovals}
          editingTemplate={editingTemplate}
          editingTemplateId={editingTemplateId}
          setEditingTemplateId={setEditingTemplateId}
          editingNodes={editingNodes}
          selectedNodeIndex={selectedNodeIndex}
          setSelectedNodeIndex={setSelectedNodeIndex}
          addNode={addNode}
          moveNode={moveNode}
          updateNode={updateNode}
          previewCtx={previewCtx}
          setPreviewCtx={setPreviewCtx}
          previewMatched={previewMatched}
          approvalVersionTemplateId={approvals.versionTemplateId}
          expandedVersionId={expandedVersionId}
          setExpandedVersionId={setExpandedVersionId}
          summarizeVersionDiff={summarizeVersionDiff}
          summarizeNodeChange={summarizeNodeChange}
          confirmRollback={confirmRollback}
          formatDateTime={formatDateTime}
        />
        <ApprovalTaskSection t={t} approvals={approvals} refreshApprovals={refreshApprovals} formatDateTime={formatDateTime} />
      </Suspense>
    </section>
  )
}

export default memo(ApprovalsPageContainer)
