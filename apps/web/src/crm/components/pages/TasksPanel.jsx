import { translateTaskLevel, translateTimeLabel } from '../../shared'

function TasksPanel({ activePage, t, tasks, canWrite, toggleTaskDone }) {
  if (activePage !== 'tasks') return null

  const visibleTasks = tasks.slice(0, 10)

  return (
    <section className="panel tasks-panel" data-testid="tasks-page">
      <div className="panel-head tasks-panel-head">
        <h2 data-testid="tasks-heading">{t('tasks')}</h2>
        <span className="tasks-count">{visibleTasks.length}</span>
      </div>
      {visibleTasks.length === 0 && (
        <div className="info-banner" style={{ marginBottom: 8 }}>
          {String(t('tasksSlowHint')) === 'tasksSlowHint'
            ? 'Task list may be delayed under high load. Try refresh in a few seconds.'
            : t('tasksSlowHint')}
        </div>
      )}
      <div className="task-list tasks-list-modern">
      {visibleTasks.map((task) => (
        <div key={task.id} className={`task-item task-item-modern ${task.done ? 'done' : ''}`}>
          <div className="task-main">
            <strong className="task-title">{task.title}</strong>
            <div className="task-meta">
              <span className="task-time-chip">{translateTimeLabel(t, task.time)}</span>
              <span className={`task-level-chip level-${String(task.level || '').toLowerCase()}`}>{translateTaskLevel(t, task.level)}</span>
              <span className="task-owner-chip">{t('owner')}: {task.owner || '-'}</span>
            </div>
          </div>
          <button
            className={`mini-btn task-action-btn ${task.done ? 'is-undo' : 'is-done'}`}
            disabled={!canWrite}
            onClick={() => toggleTaskDone(task)}
          >
            {task.done ? t('undo') : t('done')}
          </button>
        </div>
      ))}
      </div>
    </section>
  )
}

export default TasksPanel
