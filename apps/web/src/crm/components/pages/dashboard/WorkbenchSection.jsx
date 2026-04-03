import { memo } from 'react'
import { buildCardMeta, mapWorkbenchItems } from './dashboardPanelHelpers'

/**
 * 工作台部分
 */
function WorkbenchSection({
  workbenchToday,
  onWorkbenchNavigate,
  quickCreateTask,
  trackWorkbenchEvent,
  t,
}) {
  const workbenchCards = workbenchToday?.cards || []
  const todoItems = mapWorkbenchItems(workbenchToday?.todoItems, t)
  const warningItems = mapWorkbenchItems(workbenchToday?.warningItems, t)
  const cardMeta = buildCardMeta()
  const cardLabel = (row) => t(row?.labelKey || row?.key || '')

  const handleCardClick = (card, meta) => {
    const targetPage = meta.targetPage
    const payload = meta.payload
    trackWorkbenchEvent?.('card_view_click', { key: card.key, targetPage, payload })
    onWorkbenchNavigate?.(targetPage, payload)
  }

  const handleCardAction = (card, meta) => {
    const targetPage = meta.targetPage
    const payload = meta.payload
    trackWorkbenchEvent?.('card_action_click', { key: card.key, targetPage, payload })
    onWorkbenchNavigate?.(targetPage, payload)
  }

  const handleTodoClick = (item) => {
    trackWorkbenchEvent?.('todo_click', {
      id: item.id,
      type: item.type,
      targetPage: item.targetPage,
      payload: item.payload || {},
    })
    onWorkbenchNavigate?.(item.targetPage || 'dashboard', item.payload || {})
  }

  const handleWarningClick = (item) => {
    trackWorkbenchEvent?.('warning_click', {
      id: item.id,
      type: item.type,
      targetPage: item.targetPage,
      payload: item.payload || {},
    })
    onWorkbenchNavigate?.(item.targetPage || 'dashboard', item.payload || {})
  }

  return (
    <section className="panel" data-testid="dashboard-workbench">
      <div className="panel-head">
        <h2>{t('workbenchToday')}</h2>
        <div className="inline-tools">
          <button className="mini-btn" onClick={() => quickCreateTask?.({})}>
            {t('quickCreateTask')}
          </button>
          <button className="mini-btn" onClick={() => onWorkbenchNavigate?.('quotes')}>
            {t('quickCreateQuote')}
          </button>
        </div>
      </div>

      <div className="stats-grid" data-testid="dashboard-workbench-cards">
        {workbenchCards.map((card) => {
          const meta = cardMeta[card.key] || {
            targetPage: 'dashboard',
            payload: {},
            actionKey: 'workbenchView',
          }
          return (
            <article
              key={card.key}
              className={`stat-card workbench-card level-${String(card.level || 'info').toLowerCase()}`}
              data-testid="dashboard-workbench-card"
            >
              <p>{cardLabel(card)}</p>
              <h3>{card.value || 0}</h3>
              <div className="inline-tools filter-bar" style={{ marginBottom: 0 }}>
                <button className="mini-btn" onClick={() => handleCardClick(card, meta)}>
                  {t('workbenchView')}
                </button>
                <button className="mini-btn" onClick={() => handleCardAction(card, meta)}>
                  {t(meta.actionKey)}
                </button>
              </div>
            </article>
          )
        })}
      </div>

      <div className="inline-tools filter-bar" style={{ marginTop: 10 }}>
        <span className="muted-filter">{t('workbenchQuickActions')}</span>
        <button className="mini-btn" onClick={() => onWorkbenchNavigate?.('followUps')}>
          {t('quickCreateFollowUp')}
        </button>
        <button className="mini-btn" onClick={() => quickCreateTask?.({})}>
          {t('quickCreateTask')}
        </button>
        <button className="mini-btn" onClick={() => onWorkbenchNavigate?.('quotes')}>
          {t('quickCreateQuote')}
        </button>
        <button className="mini-btn" onClick={() => onWorkbenchNavigate?.('approvals', { status: 'PENDING' })}>
          {t('quickUrgeApproval')}
        </button>
      </div>

      <div className="workbench-grid" data-testid="dashboard-workbench-grid">
        <div className="workbench-col" data-testid="dashboard-workbench-todo">
          <h4>{t('todayTodo')}</h4>
          {todoItems.map((item) => (
            <button
              key={`${item.type}-${item.id}`}
              className="workbench-item"
              onClick={() => handleTodoClick(item)}
              data-testid="dashboard-workbench-todo-item"
            >
              <span>{item.title}</span>
              <small>{item.statusText} · {item.ownerText}</small>
            </button>
          ))}
          {todoItems.length === 0 && <div className="empty-tip">{t('noData')}</div>}
        </div>
        <div className="workbench-col" data-testid="dashboard-workbench-warning">
          <h4>{t('warningList')}</h4>
          {warningItems.map((item) => (
            <button
              key={`${item.type}-${item.id}`}
              className="workbench-item warning"
              onClick={() => handleWarningClick(item)}
              data-testid="dashboard-workbench-warning-item"
            >
              <span>{item.title}</span>
              <small>{item.statusText} · {item.ownerText}</small>
            </button>
          ))}
          {warningItems.length === 0 && <div className="empty-tip">{t('noData')}</div>}
        </div>
      </div>
    </section>
  )
}

export default memo(WorkbenchSection)
