import { memo, useMemo } from 'react'
import ServerPager from '../../ServerPager'
import VirtualListTable from '../../VirtualListTable'
import { useGovernanceTableState } from '../../../hooks/useGovernanceTableState'

const PAGE_SIZES = [5, 8, 12, 20]

function SalesAutomationSection({ t, salesAutomation, onRefresh }) {
  const ruleState = useGovernanceTableState({ page: 1, size: 8 })
  const automationState = useGovernanceTableState({ page: 1, size: 8 })
  const ruleQuery = ruleState.query
  const setRuleQuery = ruleState.setQuery
  const rulePage = ruleState.page
  const setRulePage = ruleState.setPage
  const ruleSize = ruleState.size
  const setRuleSize = ruleState.setSize
  const automationQuery = automationState.query
  const setAutomationQuery = automationState.setQuery
  const automationTriggerFilter = automationState.filter
  const setAutomationTriggerFilter = automationState.setFilter
  const automationPage = automationState.page
  const setAutomationPage = automationState.setPage
  const automationSize = automationState.size
  const setAutomationSize = automationState.setSize

  const triggerLabel = (value) => {
    if (value === 'LEAD_CREATED') return t('triggerLeadCreated')
    if (value === 'LEAD_STATUS_CHANGED') return t('triggerLeadStatusChanged')
    if (value === 'LEAD_ASSIGNED') return t('triggerLeadAssigned')
    return value || '-'
  }

  const actionLabel = (value) => {
    if (value === 'CREATE_TASK') return t('actionCreateTask')
    if (value === 'UPDATE_LEAD_STATUS') return t('actionUpdateLeadStatus')
    if (value === 'CREATE_NOTIFICATION') return t('actionCreateNotification')
    return value || '-'
  }

  const assignmentRulesFiltered = useMemo(() => {
    const q = String(ruleQuery || '').trim().toLowerCase()
    return (salesAutomation.assignmentRules || []).filter((row) => !q || String(row.name || '').toLowerCase().includes(q))
  }, [salesAutomation.assignmentRules, ruleQuery])

  const automationRulesFiltered = useMemo(() => {
    const q = String(automationQuery || '').trim().toLowerCase()
    return (salesAutomation.automationRules || []).filter((row) => {
      if (automationTriggerFilter && String(row.triggerType || '') !== automationTriggerFilter) return false
      if (!q) return true
      return String(row.name || '').toLowerCase().includes(q)
        || String(row.actionType || '').toLowerCase().includes(q)
    })
  }, [salesAutomation.automationRules, automationQuery, automationTriggerFilter])

  const ruleTotalPages = Math.max(1, Math.ceil(assignmentRulesFiltered.length / ruleSize))
  const automationTotalPages = Math.max(1, Math.ceil(automationRulesFiltered.length / automationSize))
  const effectiveRulePage = Math.min(rulePage, ruleTotalPages)
  const effectiveAutomationPage = Math.min(automationPage, automationTotalPages)
  const ruleRows = assignmentRulesFiltered.slice((effectiveRulePage - 1) * ruleSize, effectiveRulePage * ruleSize)
  const automationRows = automationRulesFiltered.slice((effectiveAutomationPage - 1) * automationSize, effectiveAutomationPage * automationSize)

  return (
    <>
      <div className="panel" style={{ marginTop: 12, boxShadow: 'none' }}>
        <div className="panel-head"><h2>{t('leadAssignmentRules')}</h2><button className="mini-btn" onClick={onRefresh}>{t('refresh')}</button></div>
        <div className="inline-tools filter-row" style={{ marginBottom: 8 }}>
          <input className="tool-input" placeholder={t('search')} value={ruleQuery} onChange={(e) => setRuleQuery(e.target.value)} />
        </div>
        <div className="inline-tools filter-bar" style={{ marginBottom: 8 }}>
          <input className="tool-input" placeholder={t('title')} value={salesAutomation.assignmentRuleForm.name} onChange={(e) => salesAutomation.setAssignmentRuleForm((p) => ({ ...p, name: e.target.value }))} />
          <input className="tool-input" placeholder={t('leadRuleMembersHint')} value={salesAutomation.assignmentRuleForm.membersText} onChange={(e) => salesAutomation.setAssignmentRuleForm((p) => ({ ...p, membersText: e.target.value }))} />
          <label className="switch-inline"><input type="checkbox" checked={!!salesAutomation.assignmentRuleForm.enabled} onChange={(e) => salesAutomation.setAssignmentRuleForm((p) => ({ ...p, enabled: e.target.checked }))} />{t('enabled')}</label>
          <button className="mini-btn" onClick={salesAutomation.saveLeadAssignmentRule}>{t('save')}</button>
          <button className="mini-btn" onClick={ruleState.reset}>{t('reset')}</button>
        </div>
        <div className="table-row table-head-row table-row-5 compact">
          <span>{t('title')}</span>
          <span>{t('leadRuleMembersHint')}</span>
          <span>{t('enabled')}</span>
          <span>{t('summary')}</span>
          <span>{t('action')}</span>
        </div>
        <VirtualListTable
          rows={ruleRows}
          rowHeight={46}
          viewportHeight={Math.min(360, Math.max(140, ruleRows.length * 48))}
          getRowKey={(row) => row.id}
          renderRow={(row) => (
            <div key={row.id} className="table-row table-row-5 compact">
              <span>{row.name}</span>
              <span>{(row.members || []).map((m) => `${m.username}:${m.weight}`).join(', ')}</span>
              <span>{row.enabled ? t('enabledOn') : t('enabledOff')}</span>
              <span>{row.rrCursor || '-'}</span>
              <span><button className="mini-btn" onClick={() => salesAutomation.setAssignmentRuleForm({ id: row.id, name: row.name, enabled: !!row.enabled, membersText: (row.members || []).map((m) => `${m.username}:${m.weight}`).join(',') })}>{t('detail')}</button></span>
            </div>
          )}
        />
        {assignmentRulesFiltered.length === 0 && <div className="empty-tip">{t('noData')}</div>}
        {assignmentRulesFiltered.length > 0 && <ServerPager t={t} page={effectiveRulePage} totalPages={ruleTotalPages} size={ruleSize} onPageChange={setRulePage} onSizeChange={setRuleSize} sizeOptions={PAGE_SIZES} />}
      </div>

      <div className="panel" style={{ marginTop: 12, boxShadow: 'none' }}>
        <div className="panel-head"><h2>{t('automationRules')}</h2><button className="mini-btn" onClick={onRefresh}>{t('refresh')}</button></div>
        <div className="inline-tools filter-row" style={{ marginBottom: 8 }}>
          <input className="tool-input" placeholder={t('search')} value={automationQuery} onChange={(e) => setAutomationQuery(e.target.value)} />
          <select className="tool-input" value={automationTriggerFilter} onChange={(e) => setAutomationTriggerFilter(e.target.value)}>
            <option value="">{t('allStatuses')}</option>
            <option value="LEAD_CREATED">{t('triggerLeadCreated')}</option>
            <option value="LEAD_STATUS_CHANGED">{t('triggerLeadStatusChanged')}</option>
            <option value="LEAD_ASSIGNED">{t('triggerLeadAssigned')}</option>
          </select>
        </div>
        <div className="inline-tools filter-bar" style={{ marginBottom: 8 }}>
          <input className="tool-input" placeholder={t('title')} value={salesAutomation.automationRuleForm.name} onChange={(e) => salesAutomation.setAutomationRuleForm((p) => ({ ...p, name: e.target.value }))} />
          <select className="tool-input" value={salesAutomation.automationRuleForm.triggerType} onChange={(e) => salesAutomation.setAutomationRuleForm((p) => ({ ...p, triggerType: e.target.value }))}>
            <option value="LEAD_CREATED">{t('triggerLeadCreated')}</option>
            <option value="LEAD_STATUS_CHANGED">{t('triggerLeadStatusChanged')}</option>
            <option value="LEAD_ASSIGNED">{t('triggerLeadAssigned')}</option>
          </select>
          <select className="tool-input" value={salesAutomation.automationRuleForm.actionType} onChange={(e) => salesAutomation.setAutomationRuleForm((p) => ({ ...p, actionType: e.target.value }))}>
            <option value="CREATE_TASK">{t('actionCreateTask')}</option>
            <option value="UPDATE_LEAD_STATUS">{t('actionUpdateLeadStatus')}</option>
            <option value="CREATE_NOTIFICATION">{t('actionCreateNotification')}</option>
          </select>
          <input className="tool-input" placeholder={t('triggerExprJsonHint')} value={salesAutomation.automationRuleForm.triggerExpr} onChange={(e) => salesAutomation.setAutomationRuleForm((p) => ({ ...p, triggerExpr: e.target.value }))} />
          <input className="tool-input" placeholder={t('actionPayloadJsonHint')} value={salesAutomation.automationRuleForm.actionPayload} onChange={(e) => salesAutomation.setAutomationRuleForm((p) => ({ ...p, actionPayload: e.target.value }))} />
          <label className="switch-inline"><input type="checkbox" checked={!!salesAutomation.automationRuleForm.enabled} onChange={(e) => salesAutomation.setAutomationRuleForm((p) => ({ ...p, enabled: e.target.checked }))} />{t('enabled')}</label>
          <button className="mini-btn" onClick={salesAutomation.saveAutomationRule}>{t('save')}</button>
          <button className="mini-btn" onClick={automationState.reset}>{t('reset')}</button>
        </div>
        <div className="table-row table-head-row table-row-5 compact">
          <span>{t('title')}</span>
          <span>{t('summary')}</span>
          <span>{t('action')}</span>
          <span>{t('enabled')}</span>
          <span>{t('detail')}</span>
        </div>
        <VirtualListTable
          rows={automationRows}
          rowHeight={46}
          viewportHeight={Math.min(360, Math.max(140, automationRows.length * 48))}
          getRowKey={(row) => row.id}
          renderRow={(row) => (
            <div key={row.id} className="table-row table-row-5 compact">
              <span>{row.name}</span>
              <span>{triggerLabel(row.triggerType)}</span>
              <span>{actionLabel(row.actionType)}</span>
              <span>{row.enabled ? t('enabledOn') : t('enabledOff')}</span>
              <span><button className="mini-btn" onClick={() => salesAutomation.setAutomationRuleForm({ id: row.id, name: row.name || '', triggerType: row.triggerType || 'LEAD_CREATED', triggerExpr: row.triggerExpr || '{}', actionType: row.actionType || 'CREATE_TASK', actionPayload: row.actionPayload || '{}', enabled: !!row.enabled })}>{t('detail')}</button></span>
            </div>
          )}
        />
        {automationRulesFiltered.length === 0 && <div className="empty-tip">{t('noData')}</div>}
        {automationRulesFiltered.length > 0 && <ServerPager t={t} page={effectiveAutomationPage} totalPages={automationTotalPages} size={automationSize} onPageChange={setAutomationPage} onSizeChange={setAutomationSize} sizeOptions={PAGE_SIZES} />}
      </div>
    </>
  )
}

export default memo(SalesAutomationSection)
